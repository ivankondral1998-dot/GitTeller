package app.unocontrol;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHubLoad111Test {
    private static final String REPO = "example/gitteller";
    private static final ThreadLocal<Boolean> FAIL_COMMITS = new ThreadLocal<>();
    private static final ThreadLocal<String> ISSUE_BODY = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ISSUE_STATUS = new ThreadLocal<>();

    @BeforeClass public static void installFixtureTransport() {
        // Unit-test JVM only. Every request is intercepted and no credentials are used.
        URL.setURLStreamHandlerFactory(protocol -> {
            if (!"https".equals(protocol)) return null;
            return new URLStreamHandler() {
                @Override protected URLConnection openConnection(URL url) {
                    if (!"api.github.com".equals(url.getHost()) || FAIL_COMMITS.get() == null)
                        throw new AssertionError("Unexpected HTTPS request in fixture: " + url);
                    return response(url, FAIL_COMMITS.get());
                }
            };
        });
    }

    private static HttpURLConnection response(URL url, boolean failCommits) {
        String root = "/repos/" + REPO;
        String path = url.getPath();
        String body;
        if (path.equals(root)) body = "{\"full_name\":\"example/gitteller\",\"id\":1,\"default_branch\":\"main\"}";
        else if (path.equals(root + "/issues") && ISSUE_BODY.get() != null) body = ISSUE_BODY.get();
        else if (path.equals(root + "/commits") || path.equals(root + "/branches")
                || path.equals(root + "/issues") || path.equals(root + "/pulls")) body = "[]";
        else throw new AssertionError("Unexpected fixture path: " + path);
        int status = path.equals(root + "/issues") && ISSUE_STATUS.get() != null ? ISSUE_STATUS.get()
                : path.equals(root + "/commits") && failCommits ? 403 : 200;

        return new HttpURLConnection(url) {
            @Override public int getResponseCode() { return status; }
            @Override public InputStream getInputStream() {
                return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            }
            @Override public void connect() {}
            @Override public void disconnect() {}
            @Override public boolean usingProxy() { return false; }
        };
    }

    @Test public void historyFailureMarksFirstLoadPartial() throws Exception {
        JSONObject current = load(true, null);
        assertTrue(current.getBoolean("branchesComplete"));
        assertTrue(current.getBoolean("issuesComplete"));
        assertTrue(current.getBoolean("pullsComplete"));
        assertFalse(current.getBoolean("commitsFetched"));
        assertFalse(current.getBoolean("commitsStale"));
        assertEquals("PARTIAL", current.getString("origin"));
    }

    @Test public void historyFailureKeepsPreviousHistoryAndMarksItStale() throws Exception {
        JSONObject previous = load(false, null);
        previous.getJSONArray("commits").put(new JSONObject().put("message", "Earlier entry"));
        JSONObject current = load(true, previous);
        assertFalse(current.getBoolean("commitsFetched"));
        assertTrue(current.getBoolean("commitsStale"));
        assertEquals("PARTIAL", current.getString("origin"));
        assertEquals("Earlier entry", current.getJSONArray("commits").getJSONObject(0).getString("message"));
    }

    @Test public void successfulEmptyHistoryResponseStaysLive() throws Exception {
        JSONObject current = load(false, null);
        assertTrue(current.getBoolean("commitsFetched"));
        assertFalse(current.getBoolean("commitsComplete"));
        assertEquals("LIVE", current.getString("origin"));
    }

    @Test public void invalidIssueListKeepsPriorSnapshotInsteadOfInventingChanges() throws Exception {
        JSONObject before = load(false, null);
        before.getJSONArray("issues").put(new JSONObject().put("number", 3).put("state", "open"));
        ISSUE_BODY.set("[{\"number\":3,\"state\":\"closed\"},{\"number\":3,\"state\":\"open\"}]");
        try {
            JSONObject current = load(false, before);
            assertFalse(current.getBoolean("issuesFetched"));
            assertFalse(current.getBoolean("issuesComplete"));
            assertTrue(current.getBoolean("issuesStale"));
            assertEquals("open", current.getJSONArray("issues").getJSONObject(0).getString("state"));
            assertEquals(0, ChangeSet.issueChanges(before, current));
        } finally {
            ISSUE_BODY.remove();
        }
    }

    @Test public void unsolicited304CannotReuseForeignCache() throws Exception {
        JSONObject before = load(false, null).put("repo", "foreign/project")
                .put("issuesEtag", "\"abc\"").put("issuesEtagSinglePage", true);
        before.getJSONArray("issues").put(new JSONObject().put("number", 3).put("state", "open"));
        ISSUE_STATUS.set(304);
        try {
            JSONObject current = load(false, before);
            assertFalse(current.getBoolean("issuesFetched"));
            assertFalse(current.getBoolean("issuesComplete"));
            assertEquals(0, current.getJSONArray("issues").length());
        } finally {
            ISSUE_STATUS.remove();
        }
    }

    private static JSONObject load(boolean failCommits, JSONObject previous) throws Exception {
        FAIL_COMMITS.set(failCommits);
        try {
            return new GithubClient("").load(REPO, previous).snapshot;
        } finally {
            FAIL_COMMITS.remove();
        }
    }
}
