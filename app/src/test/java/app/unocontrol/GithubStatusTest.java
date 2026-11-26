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

    @Test public void retryAfterOn403IsALimitNotABadToken() {
        String m = GithubClient.errorForStatus(403, "12", "8", null, null);
        assertTrue(m.contains("Лимит"));
        assertTrue(m.contains("8"));
        assertFalse(m.toLowerCase().contains("удалите токен"));
    }

    @Test public void apiRateMessageIsALimit() {
        String m = GithubClient.errorForStatus(403, "40", null, null, "You have exceeded a secondary rate limit");
        assertTrue(m.contains("Лимит"));
        assertTrue(m.contains("сохранены"));
    }

    @Test public void rateLimitIsNotRetried() {
        assertFalse(GithubClient.mayRetry(0, 429, null));
        assertFalse(GithubClient.mayRetry(0, 401, null));
        assertFalse(GithubClient.mayRetry(0, 403, null));
        assertFalse(GithubClient.mayRetry(0, 404, null));
        assertTrue(GithubClient.mayRetry(0, 503, null));
        assertFalse(GithubClient.mayRetry(1, 503, null));
        assertTrue(GithubClient.mayRetry(0, 0, new java.net.SocketTimeoutException("Read timed out")));
        assertFalse(GithubClient.mayRetry(0, 0, new java.io.IOException("Socket closed")));
    }

    @Test public void nextPageIsDetectedAndADeadLinkIsNot() {
        assertEquals("https://api.github.com/repos/a/b/branches?page=2",
                GithubClient.nextLink("<https://api.github.com/repos/a/b/branches?page=2>; rel=\"next\""));
        assertNull(GithubClient.nextLink("<https://api.github.com/repos/a/b/branches?page=1>; rel=\"prev\""));
        java.util.List<String> warnings = java.util.Collections.singletonList("ветки: предел 5 страниц");
        assertTrue(GithubClient.coverageBroken(warnings, "ветки"));
        assertFalse(GithubClient.coverageBroken(java.util.Collections.emptyList(), "ветки"));
    }

    @Test public void permissionTextDoesNotDeleteTheToken() {
        String m = GithubClient.errorForStatus(403, "5");
        assertTrue(m.contains("не автоматически"));
        assertFalse(m.toLowerCase().contains("удалите"));
        assertTrue(GithubClient.errorForStatus(401, null).contains("не удалён"));
    }

    @Test public void continuationStaysInsideTheSameRepository() {
        assertTrue(GithubClient.safeContinuation(
                "https://api.github.com/repositories/1296269/issues?page=2", "octocat/Hello-World", 1296269));
        assertTrue(GithubClient.safeContinuation(
                "https://api.github.com/repos/octocat/Hello-World/commits?page=2", "octocat/Hello-World", 0));
        assertFalse(GithubClient.safeContinuation("https://evil.example/repos/octocat/Hello-World", "octocat/Hello-World", 1));
        assertFalse(GithubClient.safeContinuation(
                "https://api.github.com/repos/other/project/issues?page=2", "octocat/Hello-World", 1296269));
        assertFalse(GithubClient.safeContinuation(
                "https://api.github.com/repositories/1/issues?page=2", "octocat/Hello-World", 1296269));
    }

    @Test public void continuationRejectsTraversalAndUnverifiedNumericAlias() {
        String base = "https://api.github.com/repos/a/b";
        assertFalse(GithubClient.safeContinuation(base + "/../other/issues", "a/b", 0));
        assertFalse(GithubClient.safeContinuation(base + "/%2e%2e/other/issues", "a/b", 0));
        assertFalse(GithubClient.safeContinuation(base + "/issues#ignored", "a/b", 0));
        assertFalse(GithubClient.safeContinuation("https://api.github.com/repositories/77/issues?page=2", "a/b", 0));
    }

    @Test public void paginationCannotSwitchEndpoint() {
        String first = "https://api.github.com/repos/a/b/issues?per_page=100";
        assertTrue(GithubClient.samePageEndpoint("https://api.github.com/repositories/77/issues?page=2", first, "a/b", 77));
        assertFalse(GithubClient.samePageEndpoint("https://api.github.com/repositories/78/issues?page=2", first, "a/b", 77));
        assertFalse(GithubClient.samePageEndpoint("https://api.github.com/repos/a/b/pulls?page=2", first, "a/b", 77));
    }
}
