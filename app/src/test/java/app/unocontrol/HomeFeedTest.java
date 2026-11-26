package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeFeedTest {
    @Test public void emptySnapshotHasNoRows() {
        assertTrue(HomeFeed.latest(null, 1L, 5).isEmpty());
        assertTrue(HomeFeed.latest(new JSONObject(), 1L, 5).isEmpty());
    }

    @Test public void newestComesFirstAndClosedIssueIsSkipped() throws Exception {
        JSONObject snap = new JSONObject();
        snap.put("commits", new JSONArray()
                .put(new JSONObject().put("message", "older note").put("date", "2026-11-01T00:00:00Z").put("author", "ann"))
                .put(new JSONObject().put("message", "newer note").put("date", "2026-11-10T00:00:00Z").put("author", "bob")));
        snap.put("issues", new JSONArray()
                .put(new JSONObject().put("number", 4).put("title", "closed one").put("state", "closed").put("updated_at", "2026-11-12T00:00:00Z"))
                .put(new JSONObject().put("number", 9).put("title", "still open").put("state", "open").put("updated_at", "2026-11-15T00:00:00Z")));
        snap.put("pulls", new JSONArray()
                .put(new JSONObject().put("number", 2).put("title", "landed").put("state", "closed").put("merged", true)
                        .put("merged_at", "2026-11-14T00:00:00Z").put("updated_at", "2026-11-14T00:00:00Z")));
        List<HomeFeed.Row> rows = HomeFeed.latest(snap, HomeFeed.parseIso("2026-11-16T00:00:00Z"), 5);
        assertEquals(4, rows.size());
        assertTrue(rows.get(0).title.contains("still open"));
        assertEquals("tasks", rows.get(0).page);
        assertTrue(rows.get(1).title.contains("Отмечено как внесённое"));
        assertFalse(rows.get(1).title.toLowerCase().contains("работает"));
        assertTrue(rows.get(1).meta.contains("не проверено"));
        for (HomeFeed.Row row : rows) assertFalse(row.title.contains("closed one"));
    }

    @Test public void newestPastTheArrayCapIsKept() throws Exception {
        JSONArray issues = new JSONArray();
        issues.put(openIssue(1, "old a", "2026-11-01T00:00:00Z"));
        issues.put(openIssue(2, "old b", "2026-11-02T00:00:00Z"));
        issues.put(openIssue(3, "old c", "2026-11-03T00:00:00Z"));
        issues.put(openIssue(8, "fresh later", "2026-11-15T00:00:00Z"));
        JSONArray pulls = new JSONArray();
        pulls.put(openPull(10, "early open", "2026-11-01T00:00:00Z"));
        pulls.put(openPull(11, "second open", "2026-11-02T00:00:00Z"));
        pulls.put(openPull(12, "third open", "2026-11-03T00:00:00Z"));
        pulls.put(new JSONObject().put("number", 20).put("title", "just landed").put("state", "closed").put("merged", true)
                .put("merged_at", "2026-11-14T00:00:00Z").put("updated_at", "2026-11-14T00:00:00Z"));
        JSONObject snap = new JSONObject().put("issues", issues).put("pulls", pulls);
        List<HomeFeed.Row> rows = HomeFeed.latest(snap, HomeFeed.parseIso("2026-11-16T00:00:00Z"), 5);
        boolean sawFresh = false;
        boolean sawLanded = false;
        boolean sawOld = false;
        for (HomeFeed.Row row : rows) {
            if (row.title.contains("fresh later")) sawFresh = true;
            if (row.title.contains("just landed")) sawLanded = true;
            if (row.title.contains("old a") || row.title.contains("early open")) sawOld = true;
        }
        assertTrue(sawFresh);
        assertTrue(sawLanded);
        assertFalse(sawOld);
        assertTrue(rows.get(0).title.contains("fresh later"));
    }

    private static JSONObject openIssue(int number, String title, String updated) throws Exception {
        return new JSONObject().put("number", number).put("title", title).put("state", "open").put("updated_at", updated);
    }

    private static JSONObject openPull(int number, String title, String updated) throws Exception {
        return new JSONObject().put("number", number).put("title", title).put("state", "open").put("merged", false).put("updated_at", updated);
    }

    @Test public void relativeTimeStaysShort() {
        long now = 10_000_000_000L;
        assertEquals("только что", HomeFeed.ago(now - 5_000L, now));
        assertEquals("12 мин назад", HomeFeed.ago(now - 12L * 60_000L, now));
        assertEquals("3 ч назад", HomeFeed.ago(now - 3L * 3_600_000L, now));
        assertEquals("2 д назад", HomeFeed.ago(now - 2L * 86_400_000L, now));
        assertEquals("", HomeFeed.ago(0L, now));
    }

    @Test public void laterItemWinsThePerSourceCap() throws Exception {
        JSONArray issues = new JSONArray();
        issues.put(issue(1, "2026-01-01T00:00:00Z", "first old"));
        issues.put(issue(2, "2026-01-02T00:00:00Z", "second old"));
        issues.put(issue(3, "2026-01-03T00:00:00Z", "third old"));
        issues.put(issue(8, "2026-11-16T00:00:00Z", "newest open"));
        JSONObject snap = new JSONObject().put("issues", issues);
        List<HomeFeed.Row> rows = HomeFeed.latest(snap, HomeFeed.parseIso("2026-11-17T00:00:00Z"), 5);
        assertEquals(3, rows.size());
        assertTrue(rows.get(0).title.contains("newest open"));
        assertFalse(rows.get(2).title.contains("first old"));
    }

    @Test public void badMergedStampFallsBackToUpdateTime() throws Exception {
        JSONArray pulls = new JSONArray();
        pulls.put(openPull(1, "older one", "2026-11-01T00:00:00Z"));
        pulls.put(openPull(2, "older two", "2026-11-02T00:00:00Z"));
        pulls.put(openPull(3, "older three", "2026-11-03T00:00:00Z"));
        pulls.put(new JSONObject().put("number", 9).put("title", "just landed").put("state", "closed").put("merged", true)
                .put("merged_at", "null").put("updated_at", "2026-11-16T00:00:00Z"));
        JSONObject snap = new JSONObject().put("pulls", pulls);
        List<HomeFeed.Row> rows = HomeFeed.latest(snap, HomeFeed.parseIso("2026-11-17T00:00:00Z"), 5);
        assertTrue(rows.get(0).title.contains("just landed"));
        assertTrue(rows.get(0).meta.contains("не проверено"));
    }

    private static JSONObject issue(int number, String at, String title) throws Exception {
        return new JSONObject().put("number", number).put("title", title).put("state", "open").put("updated_at", at);
    }
    @Test public void staleAndImportedRowsAreExplicitAndFutureDateIsNotRecent() throws Exception {
        JSONObject snap = new JSONObject().put("issuesStale", true)
                .put("issues", new JSONArray().put(openIssue(1, "old", "2026-11-15T00:00:00Z")));
        long now = HomeFeed.parseIso("2026-11-16T00:00:00Z");
        assertTrue(HomeFeed.latest(snap, now, 5).get(0).meta.contains("старые данные"));
        snap.put("origin", "IMPORTED");
        assertTrue(HomeFeed.latest(snap, now, 5).get(0).meta.contains("импорт"));
        assertFalse(HomeFeed.ago(now + 60_000L, now).contains("только что"));
    }

    @Test public void mergeTimestampAloneIsRecognized() throws Exception {
        JSONObject pull = new JSONObject().put("number", 9).put("state", "closed")
                .put("merged_at", "2026-11-15T00:00:00Z");
        JSONObject snap = new JSONObject().put("pulls", new JSONArray().put(pull));
        assertEquals(1, HomeFeed.latest(snap, HomeFeed.parseIso("2026-11-16T00:00:00Z"), 5).size());
        assertTrue(HumanReport.pullListLabel(pull).contains("целевую ветку"));
    }
}
