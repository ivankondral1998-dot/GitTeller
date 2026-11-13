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
import java.nio.file.Files;

@RunWith(AndroidJUnit4.class)
public class TokenVaultDeviceTest {
    private static final String MARKER = "synthetic-pat-not-real-001";
    private static final String OTHER = "synthetic-pat-not-real-002";

    private Context context;
    private TokenVault vault;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        vault = new TokenVault(context);
        File dir = new File(context.getNoBackupFilesDir(), "token-vault-v1");
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
    }

    @Test public void roundTripDoesNotWritePlaintext() throws Exception {
        vault.delete("octocat/hello-world");
        vault.save("Octocat/Hello-World", MARKER);
        assertTrue(vault.hasToken("octocat/hello-world"));
        assertEquals(MARKER, vault.load("octocat/Hello-World"));
        assertNoPlaintext(context.getNoBackupFilesDir());
        assertNoPlaintext(new File(context.getApplicationInfo().dataDir, "shared_prefs"));
        File prefs = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        if (prefs.isDirectory()) {
            for (File f : prefs.listFiles()) {
                String text = new String(Files.readAllBytes(f.toPath()));
                assertFalse(text.contains(MARKER));
            }
        }
    }

    @Test public void reposStaySeparateAndDeleteIsLocal() throws Exception {
        vault.save("demo/alpha", MARKER);
        vault.save("demo/beta", OTHER);
        assertEquals(MARKER, vault.load("demo/alpha"));
        assertEquals(OTHER, vault.load("demo/beta"));
        vault.delete("demo/alpha");
        assertNull(vault.load("demo/alpha"));
        assertEquals(OTHER, vault.load("demo/beta"));
        vault.delete("demo/beta");
    }

    @Test public void emptySaveDoesNotErase() throws Exception {
        vault.save("demo/keep", MARKER);
        try {
            vault.save("demo/keep", "   ");
            fail("empty save");
        } catch (TokenVault.VaultException expected) {
            assertTrue(expected.getMessage().contains("не заменяет"));
        }
        assertEquals(MARKER, vault.load("demo/keep"));
        vault.delete("demo/keep");
    }

    @Test public void tamperFailsClosed() throws Exception {
        vault.save("demo/tamper", MARKER);
        File dir = new File(context.getNoBackupFilesDir(), "token-vault-v1");
        File blob = null;
        for (File f : dir.listFiles()) if (f.getName().endsWith(".bin")) blob = f;
        assertTrue(blob != null && blob.isFile());
        byte[] raw = Files.readAllBytes(blob.toPath());
        raw[raw.length - 1] ^= 0x5a;
        Files.write(blob.toPath(), raw);
        try {
            vault.load("demo/tamper");
            fail("tamper must not decrypt");
        } catch (TokenVault.VaultException expected) {
            assertTrue(expected.getMessage().contains("поврежд"));
            assertFalse(expected.getMessage().contains(MARKER));
        }
        vault.delete("demo/tamper");
    }

    private static void assertNoPlaintext(File root) throws Exception {
        if (root == null || !root.exists()) return;
        if (root.isFile()) {
            byte[] data = Files.readAllBytes(root.toPath());
            String text = new String(data);
            assertFalse(root.getPath(), text.contains(MARKER));
            assertFalse(root.getPath(), text.contains(OTHER));
            return;
        }
        File[] kids = root.listFiles();
        if (kids == null) return;
        for (File kid : kids) assertNoPlaintext(kid);
    }
}
