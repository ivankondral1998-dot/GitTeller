package app.unocontrol;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Android API 26+: same-directory atomic encrypted-file replacement, fail closed. */
final class AtomicCipherFile {
    private AtomicCipherFile() {}

    static File recover(File dest) throws IOException {
        if (dest.isFile()) return dest;
        File backup = new File(dest.getPath() + ".bak");
        if (backup.isFile()) {
            try {
                Files.move(backup.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Cannot safely restore legacy ciphertext backup", e);
            }
        }
        return dest;
    }

    static void replace(File tmp, File dest) throws IOException {
        if (tmp == null || !tmp.isFile() || dest == null || dest.getParentFile() == null)
            throw new IOException("Invalid encrypted-file replacement");
        File parent = dest.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot prepare protected directory");
        boolean installed = false;
        try {
            recover(dest); // Complete any legacy backup left by a crashed older version.
            Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            installed = true;
        } finally {
            if (!installed) Files.deleteIfExists(tmp.toPath());
        }
        // Cleanup is best-effort: once the atomic move succeeded, do not report a
        // false save failure merely because an obsolete encrypted .bak remains.
        try { Files.deleteIfExists(new File(dest.getPath() + ".bak").toPath()); }
        catch (IOException ignored) { /* delete() also clears stale backups */ }
    }

    static boolean hasRecord(File dest) {
        return dest.isFile() || new File(dest.getPath() + ".bak").isFile();
    }

    static void delete(File dest) throws IOException {
        Files.deleteIfExists(dest.toPath());
        Files.deleteIfExists(new File(dest.getPath() + ".bak").toPath());
        Files.deleteIfExists(new File(dest.getPath() + ".tmp").toPath());
    }
}
