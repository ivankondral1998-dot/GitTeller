package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PaletteQuietTest {
    @Test public void tenLooksEachHaveDayAndNight() {
        assertEquals(10, Palette.all().length);
        for (Palette.Look look : Palette.all()) {
            assertEquals(8, look.day.length);
            assertEquals(8, look.night.length);
            assertNotEquals(look.day[0], look.night[0]);
            assertFalse(look.title.isEmpty());
            assertFalse(look.alias.isEmpty());
        }
    }

    @Test public void dayButtonForcesLightEvenIfPhoneSaysNight() {
        assertFalse(Palette.dark(Palette.DAY, 0x20));
        assertTrue(Palette.dark(Palette.NIGHT, 0x10));
        assertFalse(Palette.dark(Palette.SYSTEM, 0));
        assertTrue(Palette.dark(Palette.SYSTEM, 0x20));
    }

    @Test public void quietScheduleRestoresTheSavedDeadline() {
        long now = 1_000_000L;
        assertEquals(Watch.INTERVAL_MS, Watch.scheduleDelay(now, now + Watch.INTERVAL_MS));
        assertEquals(Watch.INTERVAL_MS / 2, Watch.scheduleDelay(now, now + Watch.INTERVAL_MS / 2));
        assertEquals(60_000L, Watch.scheduleDelay(now, now + 1_000L));
        assertEquals(Watch.FIRST_MS, Watch.scheduleDelay(now, 0L));
        assertEquals(Watch.INTERVAL_MS, Watch.scheduleDelay(now, now));
        assertEquals(Watch.INTERVAL_MS, Watch.scheduleDelay(now, now - 1L));
    }

    @Test public void backgroundCheckKeepsItsBaselineOnlyWhileItIsStillSaved() {
        assertTrue(Watch.baselineStillSaved("same", "same"));
        assertFalse(Watch.baselineStillSaved("old snapshot", "newer manual refresh"));
        assertFalse(Watch.baselineStillSaved(null, "imported"));
        assertTrue(Watch.baselineStillSaved("", ""));
    }

    @Test public void quietNoticeStaysSilentWithoutARealChange() throws Exception {
        JSONObject first = snap("demo/app", "aaa");
        assertFalse(QuietNotice.worthTelling(first, null));
        JSONObject same = snap("demo/app", "aaa");
        assertFalse(QuietNotice.worthTelling(same, first));
        JSONObject moved = snap("demo/app", "bbb");
        assertTrue(QuietNotice.worthTelling(moved, first));
        String line = QuietNotice.oneLine(moved, first);
        assertTrue(line.contains("demo/app"));
        assertFalse(line.toLowerCase().contains("работает"));
        assertTrue(line.length() < 80);
    }

    private static JSONObject snap(String repo, String sha) throws Exception {
        return new JSONObject()
                .put("repo", repo)
                .put("origin", "LIVE")
                .put("branchesComplete", true)
                .put("issuesComplete", true)
                .put("pullsComplete", true)
                .put("branches", new JSONArray().put(new JSONObject().put("name", "main").put("sha", sha)))
                .put("issues", new JSONArray())
                .put("pulls", new JSONArray());
    }
}
