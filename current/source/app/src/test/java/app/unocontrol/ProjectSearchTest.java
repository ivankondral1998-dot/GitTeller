package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectSearchTest {
    private static JSONObject snapshot() throws Exception {
        return new JSONObject().put("repo", "demo/app").put("origin", "PARTIAL")
                .put("branchesFetched", true).put("branchesComplete", false)
                .put("issuesFetched", true).put("issuesComplete", true)
                .put("pullsFetched", true).put("pullsComplete", true)
                .put("commitsFetched", true)
                .put("warnings", new JSONArray().put("ветки: предел 5 страниц"))
                .put("branches", new JSONArray()
                        .put(new JSONObject().put("name", "fix/login").put("sha", "abc1111111111111111111111111111111111111"))
                        .put(new JSONObject().put("name", "fix/login").put("sha", "def2222222222222222222222222222222222222"))
                        .put(new JSONObject().put("name", "исправление-окна").put("sha", "abc9999999999999999999999999999999999999")))
                .put("issues", new JSONArray()
                        .put(new JSONObject().put("number", 6).put("title", "Открыть отчёт"))
                        .put(new JSONObject().put("number", 16).put("title", "Другая задача")))
                .put("pulls", new JSONArray().put(new JSONObject().put("number", 4).put("title", "fix/login proposal")))
                .put("commits", new JSONArray().put(new JSONObject()
                        .put("sha", "abc1111111111111111111111111111111111111")
                        .put("message", "очень-длинное-имя-".concat(repeat("х", 240)))
                        .put("author", "octocat")));
    }

    private static String repeat(String piece, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(piece);
        return b.toString();
    }

    private static int group(ProjectSearch.Report report, String name) {
        int n = 0;
        for (ProjectSearch.Hit hit : report.hits) if (name.equals(hit.group)) n++;
        return n;
    }

    @Test public void emptyQueryDoesNotClaimTheRemoteIsEmpty() throws Exception {
        ProjectSearch.Report report = ProjectSearch.find(snapshot(), new JSONArray(), "   ", null);
        assertTrue(report.emptyQuery);
        assertTrue(report.hits.isEmpty());
        assertTrue(ProjectSearch.emptyHint().contains("не весь GitHub"));
        assertFalse(ProjectSearch.emptyHint().contains("на GitHub нет"));
    }

    @Test public void cyrillicPrefixShaAndHashStayOnTheSavedRecord() throws Exception {
        JSONObject snapshot = snapshot();
        ProjectSearch.Report cyr = ProjectSearch.find(snapshot, new JSONArray(), "ИСПРАВЛЕНИЕ", null);
        assertEquals(1, group(cyr, "Ветки"));
        assertEquals("branch-detail", cyr.hits.get(0).page);
        assertEquals("исправление-окна", cyr.hits.get(0).key);

        ProjectSearch.Report fix = ProjectSearch.find(snapshot, new JSONArray(), "fix/", null);
        assertEquals(2, group(fix, "Ветки"));
        assertEquals(0, fix.hits.get(0).index);
        assertEquals(1, fix.hits.get(1).index);

        ProjectSearch.Report number = ProjectSearch.find(snapshot, new JSONArray(), "#6", null);
        assertEquals(1, group(number, "Задачи"));
        assertEquals("6", number.hits.get(0).key);
        assertEquals("issue-detail", number.hits.get(0).page);

        ProjectSearch.Report sha = ProjectSearch.find(snapshot, new JSONArray(), "abc1111", null);
        assertTrue(group(sha, "Ветки") >= 1);
        assertTrue(group(sha, "Коммиты") >= 1);
        boolean longName = false;
        for (ProjectSearch.Hit hit : sha.hits) {
            assertTrue(hit.title.length() <= 161);
            if ("Коммиты".equals(hit.group)) longName = true;
        }
        assertTrue(longName);

        ProjectSearch.Report missing = ProjectSearch.find(new JSONObject().put("repo", "demo/app"), null, "#6", null);
        assertEquals(0, group(missing, "Задачи"));
        assertTrue(missing.scope.contains("demo/app"));
    }

    @Test public void importedStaleRepoChangeAndOverflowDoNotInventARemoteMiss() throws Exception {
        JSONObject imported = snapshot().put("origin", "IMPORTED").put("issuesStale", true);
        ProjectSearch.Report report = ProjectSearch.find(imported, null, "отчёт", Collections.singletonList("other/repo"));
        assertTrue(report.scope.contains("IMPORTED"));
        assertTrue(report.scope.contains("устаревшие"));
        assertEquals(0, group(report, "Проекты"));

        ProjectSearch.Report other = ProjectSearch.find(snapshot().put("repo", "other/repo"), new JSONArray(), "other/repo",
                Arrays.asList("demo/app", "other/repo"));
        assertEquals("other/repo", other.scope.contains("other/repo") ? "other/repo" : "");
        assertEquals(1, group(other, "Проекты"));
        assertEquals("switch-repo", other.hits.get(0).page);

        JSONArray many = new JSONArray();
        for (int i = 0; i < 120; i++) many.put(new JSONObject().put("name", "branch-" + i).put("sha", "a"));
        JSONObject wide = snapshot().put("branches", many);
        ProjectSearch.Report overflow = ProjectSearch.find(wide, new JSONArray(), "branch-", null);
        assertEquals(ProjectSearch.PER_GROUP, group(overflow, "Ветки"));
        assertEquals(80, overflow.omitted);

        JSONArray feed = new JSONArray().put(new JSONObject().put("id", "e1").put("kind", "branch_moved").put("event", "указатель сдвинулся"));
        ProjectSearch.Report events = ProjectSearch.find(snapshot(), feed, "указатель", null);
        assertEquals(1, group(events, "Изменения"));
        assertEquals("e1", events.hits.get(0).marker);
        assertEquals("changes", events.hits.get(0).page);

        ProjectSearch.Report none = ProjectSearch.find(snapshot(), new JSONArray(), "нет-такого-слова", null);
        assertTrue(none.hits.isEmpty());
        assertFalse(none.emptyQuery);
        assertTrue(ProjectSearch.noMatch().contains("не доказательство"));
        assertFalse(ProjectSearch.noMatch().contains("на GitHub нет"));
    }
}
