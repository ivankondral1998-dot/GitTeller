package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SnapshotCodecTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    private static JSONObject raw() throws Exception {
        JSONObject s = new JSONObject();
        s.put("repo", "demo/project");
        s.put("token", "ghp_SECRET_MUST_NOT_SURVIVE");
        s.put("authorization", "Bearer nope");
        s.put("capturedAt", 10);
        s.put("defaultBranch", "main");
        s.put("branchesComplete", true);
        s.put("issuesComplete", true);
        s.put("pullsComplete", false);
        JSONArray branches = new JSONArray();
        branches.put(new JSONObject().put("name", "main").put("sha", SHA).put("token", "nope"));
        s.put("branches", branches);
        s.put("issues", new JSONArray().put(new JSONObject().put("number", 3).put("title", "door").put("state", "open").put("body", "secret note")));
        s.put("warnings", new JSONArray().put("ok"));
        return s;
    }

    @Test public void tokenFieldsAreDropped() throws Exception {
        JSONObject clean = SnapshotCodec.sanitizeSnapshot(raw(), "demo/project");
        assertFalse(clean.toString().toLowerCase().contains("secret"));
        assertFalse(clean.toString().toLowerCase().contains("ghp_"));
        assertFalse(clean.toString().contains("authorization"));
        assertFalse(clean.toString().contains("secret note"));
        assertEquals(SHA, clean.getJSONArray("branches").getJSONObject(0).getString("sha"));
        assertEquals("open", clean.getJSONArray("issues").getJSONObject(0).getString("state"));
        assertTrue(clean.getBoolean("issuesComplete"));
        assertFalse(clean.getBoolean("pullsComplete"));
    }

    @Test public void badShaRejected() throws Exception {
        JSONObject s = raw();
        s.getJSONArray("branches").getJSONObject(0).put("sha", "not-a-sha");
        try {
            SnapshotCodec.sanitizeSnapshot(s, "demo/project");
            fail("bad sha must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ветки"));
        }
    }

    @Test public void repoMismatchRejected() throws Exception {
        try {
            SnapshotCodec.sanitizeSnapshot(raw(), "other/project");
            fail("mismatch must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("не совпадает"));
        }
    }

    @Test public void missingBranchListRejected() {
        try {
            SnapshotCodec.sanitizeSnapshot(new JSONObject().put("repo", "demo/project"), "demo/project");
            fail("missing branches must be rejected");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("веток"));
        }
    }
}
