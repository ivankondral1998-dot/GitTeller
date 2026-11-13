package app.unocontrol;

import org.junit.Test;
import static org.junit.Assert.*;

/** Rules that do not touch Android Keystore. They do not prove encryption. */
public class TokenVaultRulesTest {
    @Test public void repoIdIsCaseInsensitive() {
        assertEquals("octocat/hello-world", TokenVault.normalizeRepo(" Octocat/Hello-World "));
    }
    @Test public void pathTraversalRejected() {
        try {
            TokenVault.normalizeRepo("octocat/../Hello");
            fail("traversal");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("формате"));
        }
    }
    @Test public void emptyRejected() {
        try {
            TokenVault.normalizeRepo("   ");
            fail("blank");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
