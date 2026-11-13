package app.unocontrol;

import org.junit.Test;
import static org.junit.Assert.*;

public class GithubStatusTest {
    @Test public void unauthorizedKeepsOldDataWording() {
        String m = GithubClient.errorForStatus(401, null);
        assertTrue(m.contains("401"));
        assertTrue(m.contains("сохранены"));
    }
    @Test public void forbiddenIsNotARateLimitUnlessRemainingIsZero() {
        String denied = GithubClient.errorForStatus(403, "12");
        assertTrue(denied.contains("403"));
        assertFalse(denied.contains("Лимит"));
    }
    @Test public void forbiddenWithZeroRemainingIsRateLimit() {
        assertTrue(GithubClient.errorForStatus(403, "0").contains("Лимит"));
    }
    @Test public void tooManyRequestsIsRateLimit() {
        assertTrue(GithubClient.errorForStatus(429, "0").contains("Лимит"));
        assertTrue(GithubClient.errorForStatus(429, null).contains("сохранены"));
    }
    @Test public void missingRepoIs404() {
        assertTrue(GithubClient.errorForStatus(404, null).contains("404"));
    }
    @Test public void otherStatusDoesNotInventSuccess() {
        String m = GithubClient.errorForStatus(500, null);
        assertTrue(m.contains("500"));
        assertFalse(m.contains("PASS"));
    }
}
