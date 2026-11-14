package app.unocontrol;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Locale;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Per-repository GitHub token sealed with the Android Keystore. Ciphertext only on disk. */
final class TokenVault {
    static final class VaultException extends Exception {
        VaultException(String message) { super(message); }
    }

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "gitteller.pat.aes.v1";
    private static final byte[] MAGIC = new byte[] {'G', 'T', 'V', '1'};
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final File dir;

    TokenVault(Context context) {
        dir = new File(context.getNoBackupFilesDir(), "token-vault-v1");
        if (!dir.exists() && !dir.mkdirs()) {
            // Directory is created lazily again on save; load of a missing dir is "no token".
        }
    }

    /** Test hook: the install step fails and the previous file must remain. */
    static boolean testFailInstall;

    /** Atomic replacement. On a rejected installation the previous ciphertext is untouched. */
    static void replaceAtomically(File tmp, File dest) throws VaultException {
        if (testFailInstall) {
            if (tmp != null) tmp.delete();
            throw new VaultException("Не удалось записать доступ. Прежняя запись сохранена.");
        }
        try {
            AtomicCipherFile.replace(tmp, dest);
        } catch (Exception e) {
            throw new VaultException("Не удалось атомарно заменить токен. Прежняя запись не удалена.");
        }
    }

    static String normalizeRepo(String repo) {
        if (repo == null) throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        String n = repo.trim().toLowerCase(Locale.ROOT);
        if (!n.matches("[a-z0-9_.-]{1,100}/[a-z0-9_.-]{1,100}") || n.contains(".."))
            throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        return n;
    }

    void save(String repoId, String token) throws VaultException {
        String repo = normalizeRepo(repoId);
        if (token == null || token.trim().isEmpty())
            throw new VaultException("Пустое поле не заменяет сохранённый доступ.");
        String value = token.trim();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD(aad(repo));
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != IV_LEN)
                throw new VaultException("Android не выдал новый вектор шифрования. Токен не записан.");
            byte[] body = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] blob = new byte[MAGIC.length + IV_LEN + body.length];
            System.arraycopy(MAGIC, 0, blob, 0, MAGIC.length);
            System.arraycopy(iv, 0, blob, MAGIC.length, IV_LEN);
            System.arraycopy(body, 0, blob, MAGIC.length + IV_LEN, body.length);
            File dest = fileFor(repo);
            File tmp = new File(dest.getParentFile(), dest.getName() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(blob);
                out.getFD().sync();
            }
            replaceAtomically(tmp, dest);
        } catch (VaultException e) {
            throw e;
        } catch (android.security.keystore.KeyPermanentlyInvalidatedException e) {
            throw new VaultException("Ключ Android сброшен. Введите токен заново.");
        } catch (Exception e) {
            throw new VaultException("Не удалось сохранить доступ средствами Android.");
        } finally {
            value = null;
        }
    }

    String load(String repoId) throws VaultException {
        String repo = normalizeRepo(repoId);
        File f = fileFor(repo);
        try { f = AtomicCipherFile.recover(f); }
        catch (Exception e) { throw new VaultException("Не удалось восстановить защищённую запись. Она не удалена."); }
        if (!f.isFile()) return null;
        try {
            byte[] blob = java.nio.file.Files.readAllBytes(f.toPath());
            if (blob.length < MAGIC.length + IV_LEN + TAG_BITS / 8) throw new java.io.IOException("short");
            for (int i = 0; i < MAGIC.length; i++) if (blob[i] != MAGIC[i]) throw new java.io.IOException("magic");
            byte[] iv = java.util.Arrays.copyOfRange(blob, MAGIC.length, MAGIC.length + IV_LEN);
            byte[] body = java.util.Arrays.copyOfRange(blob, MAGIC.length + IV_LEN, blob.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(repo));
            byte[] plain = cipher.doFinal(body);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new VaultException("Сохранённый доступ повреждён. Удалите его и введите токен заново.");
        } catch (android.security.keystore.KeyPermanentlyInvalidatedException e) {
            throw new VaultException("Ключ Android сброшен. Введите токен заново.");
        } catch (VaultException e) {
            throw e;
        } catch (Exception e) {
            throw new VaultException("Сохранённый доступ не читается. Удалите его и введите токен заново.");
        }
    }

    boolean hasToken(String repoId) {
        try {
            return AtomicCipherFile.hasRecord(fileFor(normalizeRepo(repoId)));
        } catch (RuntimeException | VaultException e) {
            return false;
        }
    }

    void delete(String repoId) throws VaultException {
        String repo = normalizeRepo(repoId);
        File f = fileFor(repo);
        try { AtomicCipherFile.delete(f); }
        catch (Exception e) { throw new VaultException("Не удалось удалить сохранённый доступ полностью."); }
    }

    private File fileFor(String normalizedRepo) throws VaultException {
        if (!dir.exists() && !dir.mkdirs()) throw new VaultException("Нет папки для защищённых записей.");
        try {
            byte[] dig = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(normalizedRepo.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder(64);
            for (byte b : dig) name.append(String.format(Locale.ROOT, "%02x", b));
            name.append(".bin");
            return new File(dir, name.toString());
        } catch (Exception e) {
            throw new VaultException("Не удалось выбрать запись проекта.");
        }
    }

    private static byte[] aad(String normalizedRepo) {
        return ("gitteller-pat-v1\n" + normalizedRepo).getBytes(StandardCharsets.UTF_8);
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            generator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            generator.generateKey();
        }
        return (SecretKey) ks.getKey(KEY_ALIAS, null);
    }
}
