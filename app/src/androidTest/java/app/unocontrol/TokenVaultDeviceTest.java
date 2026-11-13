package app.unocontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Isolated synthetic-token checks. NEVER erase every file in token-vault-v1:
 * instrumentation is installed against the target application's real data directory.
 */
@RunWith(AndroidJUnit4.class)
public class TokenVaultDeviceTest {
    private static final String MARKER = "synthetic-pat-not-real-001";
    private static final String OTHER = "synthetic-pat-not-real-002";

    private Context context;
    private TokenVault vault;
    private String fixtureOwner;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        vault = new TokenVault(context);
        fixtureOwner = "gitteller-test-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String repo(String name) { return fixtureOwner + "/" + name; }

    @Test public void roundTripDoesNotWritePlaintext() throws Exception {
        String id = repo("roundtrip");
        try {
            vault.save(id.toUpperCase(Locale.ROOT), MARKER);
            assertTrue(vault.hasToken(id));
            assertEquals(MARKER, vault.load(id));
            assertNoPlaintext(blobFor(id));
            assertNoPlaintext(new File(context.getApplicationInfo().dataDir, "shared_prefs"));
        } finally {
            vault.delete(id);
        }
    }

    @Test public void reposStaySeparateAndDeleteIsLocal() throws Exception {
        String alpha = repo("alpha"), beta = repo("beta");
        try {
            vault.save(alpha, MARKER);
            vault.save(beta, OTHER);
            assertEquals(MARKER, vault.load(alpha));
            assertEquals(OTHER, vault.load(beta));
            vault.delete(alpha);
            assertNull(vault.load(alpha));
            assertEquals(OTHER, vault.load(beta));
        } finally {
            vault.delete(alpha);
            vault.delete(beta);
        }
    }

    @Test public void emptySaveDoesNotErase() throws Exception {
        String id = repo("keep");
        try {
            vault.save(id, MARKER);
            try {
                vault.save(id, "   ");
                fail("Empty save must fail without deleting a saved token.");
            } catch (TokenVault.VaultException expected) {
                assertTrue(expected.getMessage().contains("не заменяет"));
            }
            assertEquals(MARKER, vault.load(id));
        } finally {
            vault.delete(id);
        }
    }

    @Test public void tamperFailsClosed() throws Exception {
        String id = repo("tamper");
        try {
            vault.save(id, MARKER);
            File blob = blobFor(id); // Never select an arbitrary .bin belonging to the user.
            assertTrue(blob.isFile());
            byte[] raw = Files.readAllBytes(blob.toPath());
            raw[raw.length - 1] ^= 0x5a;
            Files.write(blob.toPath(), raw);
            try {
                vault.load(id);
                fail("Tampering must not decrypt.");
            } catch (TokenVault.VaultException expected) {
                assertTrue(expected.getMessage().contains("поврежд"));
                assertFalse(expected.getMessage().contains(MARKER));
            }
        } finally {
            vault.delete(id);
        }
    }

    private File blobFor(String repoId) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(TokenVault.normalizeRepo(repoId).getBytes(StandardCharsets.UTF_8));
        StringBuilder filename = new StringBuilder();
        for (byte b : digest) filename.append(String.format(Locale.ROOT, "%02x", b));
        return new File(new File(context.getNoBackupFilesDir(), "token-vault-v1"), filename + ".bin");
    }

    private static void assertNoPlaintext(File root) throws Exception {
        if (root == null || !root.exists()) return;
        if (root.isFile()) {
            String text = new String(Files.readAllBytes(root.toPath()), StandardCharsets.UTF_8);
            assertFalse(root.getPath(), text.contains(MARKER));
            assertFalse(root.getPath(), text.contains(OTHER));
            return;
        }
        File[] kids = root.listFiles();
        if (kids != null) for (File kid : kids) assertNoPlaintext(kid);
    }
}
