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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Two distinct instrumentation processes. Test-only repo identity is persisted
 * between them. Do not clear all real user tokens as part of test setup.
 */
@RunWith(AndroidJUnit4.class)
public class TokenVaultRestartTest {
    static final String MARKER = "synthetic-pat-not-real-001";
    private static final String META = "gitteller-instrument-restart-repo.txt";
    private static final String OWNER_PREFIX = "gitteller-restart-";

    @Test public void saveForRestart() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TokenVault vault = new TokenVault(context);
        File identity = new File(context.getNoBackupFilesDir(), META);
        // A stale test run must fail explicitly, not silently overwrite someone else's data.
        assertFalse("Previous restart test not completed; inspect before retrying.", identity.exists());
        String repo = OWNER_PREFIX + UUID.randomUUID().toString().replace("-", "") + "/checkpoint";
        assertFalse("Fixture unexpectedly collides with existing token.", vault.hasToken(repo));
        vault.save(repo, MARKER);
        assertEquals(MARKER, vault.load(repo));
        File blob = blobFor(context, repo);
        byte[] raw = Files.readAllBytes(blob.toPath());
        assertTrue(raw.length > 16);
        assertEquals('G', raw[0]);
        assertEquals('T', raw[1]);
        assertEquals('V', raw[2]);
        assertEquals('1', raw[3]);
        assertFalse(new String(raw, StandardCharsets.UTF_8).contains(MARKER));
        Files.write(identity.toPath(), repo.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void loadAfterRestart() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TokenVault vault = new TokenVault(context);
        File identity = new File(context.getNoBackupFilesDir(), META);
        assertTrue("Run saveForRestart in a previous instrumentation process.", identity.isFile());
        String repo = new String(Files.readAllBytes(identity.toPath()), StandardCharsets.UTF_8);
        assertTrue("Invalid fixture owner.", repo.startsWith(OWNER_PREFIX));
        assertTrue("Invalid fixture name.", repo.endsWith("/checkpoint"));
        try {
            File blob = blobFor(context, repo);
            byte[] raw = Files.readAllBytes(blob.toPath());
            assertFalse(new String(raw, StandardCharsets.UTF_8).contains(MARKER));
            assertEquals(MARKER, vault.load(repo));
            assertTrue(vault.hasToken(repo));
        } finally {
            vault.delete(repo);
            assertTrue("Could not remove test-only metadata.", identity.delete());
        }
        assertNull(vault.load(repo));
        assertFalse(vault.hasToken(repo));
    }

    private static File blobFor(Context context, String repo) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(TokenVault.normalizeRepo(repo).getBytes(StandardCharsets.UTF_8));
        StringBuilder name = new StringBuilder();
        for (byte b : digest) name.append(String.format(Locale.ROOT, "%02x", b));
        return new File(new File(context.getNoBackupFilesDir(), "token-vault-v1"), name + ".bin");
    }
}
