package app.unocontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;

/**
 * Two separate instrumentation processes. Run saveForRestart, force-stop the app,
 * then loadAfterRestart. A single in-process call is not a restart proof.
 */
@RunWith(AndroidJUnit4.class)
public class TokenVaultRestartTest {
    static final String MARKER = "synthetic-pat-not-real-001";
    static final String REPO = "octocat/hello-world";

    @Test public void saveForRestart() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TokenVault vault = new TokenVault(context);
        File dir = new File(context.getNoBackupFilesDir(), "token-vault-v1");
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) if (!f.delete()) throw new AssertionError(f.getName());
        vault.save("Octocat/Hello-World", MARKER);
        assertTrue(vault.hasToken(REPO));
        assertEquals(MARKER, vault.load(REPO));
        File blob = onlyBlob(dir);
        byte[] raw = Files.readAllBytes(blob.toPath());
        assertTrue(raw.length > 16);
        assertEquals('G', raw[0]);
        assertEquals('T', raw[1]);
        assertEquals('V', raw[2]);
        assertEquals('1', raw[3]);
        assertFalse(new String(raw).contains(MARKER));
    }

    @Test public void loadAfterRestart() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TokenVault vault = new TokenVault(context);
        File dir = new File(context.getNoBackupFilesDir(), "token-vault-v1");
        File blob = onlyBlob(dir);
        byte[] raw = Files.readAllBytes(blob.toPath());
        assertFalse(new String(raw).contains(MARKER));
        assertEquals(MARKER, vault.load("octocat/Hello-World"));
        assertTrue(vault.hasToken(REPO));
        vault.delete(REPO);
        assertNull(vault.load(REPO));
        assertFalse(vault.hasToken(REPO));
    }

    private static File onlyBlob(File dir) {
        File blob = null;
        File[] files = dir.listFiles();
        assertTrue(files != null);
        for (File f : files) if (f.getName().endsWith(".bin")) blob = f;
        assertTrue(blob != null && blob.isFile());
        return blob;
    }
}
