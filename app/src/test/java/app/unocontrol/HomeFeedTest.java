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

    @Test public void relativeTimeStaysShort() {
        long now = 10_000_000_000L;
        assertEquals("только что", HomeFeed.ago(now - 5_000L, now));
        assertEquals("12 мин назад", HomeFeed.ago(now - 12L * 60_000L, now));
        assertEquals("3 ч назад", HomeFeed.ago(now - 3L * 3_600_000L, now));
        assertEquals("2 д назад", HomeFeed.ago(now - 2L * 86_400_000L, now));
        assertEquals("", HomeFeed.ago(0L, now));
    }
}
