package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoverageTest {
    private static JSONObject live() throws Exception {
        return new JSONObject().put("repo", "demo/app").put("origin", "LIVE")
                .put("capturedAt", 1_700_000_000_000L)
                .put("branches", new JSONArray().put(new JSONObject().put("name", "main")))
                .put("issues", new JSONArray())
                .put("pulls", new JSONArray())
                .put("commits", new JSONArray())
                .put("branchesFetched", true).put("issuesFetched", true).put("pullsFetched", true).put("commitsFetched", true)
                .put("branchesComplete", true).put("issuesComplete", true).put("pullsComplete", true)
                .put("commitsComplete", false)
                .put("warnings", new JSONArray());
    }

    private static Coverage.Row row(JSONObject snapshot, String area) {
        return Coverage.one(snapshot, area);
    }

    @Test public void completeListStaysInsideTheRequestAndChecksAreNotPass() throws Exception {
        JSONObject snapshot = live();
        Coverage.Row branches = row(snapshot, "branches");
        assertEquals(Coverage.COMPLETE_WITHIN_REQUEST, branches.code);
        assertTrue(branches.developer.contains("не весь"));
        assertEquals(Coverage.SAMPLED_BY_DESIGN, row(snapshot, "commits").code);
        assertTrue(row(snapshot, "commits").novice.contains("не ошибка"));
        assertEquals(Coverage.NOT_VERIFIED, row(snapshot, "checks").code);
        assertFalse(row(snapshot, "checks").developer.contains("проверки прошли"));
        assertTrue(Coverage.compact(snapshot, false).contains("не подтверждены"));
        assertFalse(Coverage.compact(snapshot, true).contains("всё хорошо"));
    }

    @Test public void pageCapAndBrokenPageStayDistinct() throws Exception {
        JSONObject cap = live().put("branchesComplete", false)
                .put("warnings", new JSONArray().put("ветки: предел 5 страниц"));
        assertEquals(Coverage.TRUNCATED_BY_CAP, row(cap, "branches").code);
        assertTrue(row(cap, "branches").fact.contains("5"));
        assertTrue(row(cap, "branches").developer.contains("cap 5"));
        JSONObject broken = live().put("issuesComplete", false)
                .put("warnings", new JSONArray().put("задачи: загрузка прервана"));
        assertEquals(Coverage.PARTIAL_PAGE, row(broken, "issues").code);
    }

    @Test public void httpCodesAreNotMergedAndPlain403IsNotAProvenScopeError() throws Exception {
        JSONObject denied = live().put("pullsFetched", false).put("pullsComplete", false)
                .put("warnings", new JSONArray().put("предложения: Токен недействителен или истёк (401)."));
        assertEquals(Coverage.ACCESS_OR_API_FAILURE, row(denied, "pulls").code);
        assertEquals("access", row(denied, "pulls").action);
        assertTrue(row(denied, "pulls").fact.contains("401"));

        JSONObject forbidden = live().put("issuesFetched", false).put("issuesComplete", false)
                .put("warnings", new JSONArray().put("задачи: Доступ ограничен (403): не хватает прав или так закрыта эта точка API."));
        assertEquals(Coverage.ACCESS_OR_API_FAILURE, row(forbidden, "issues").code);
        assertTrue(row(forbidden, "issues").fact.contains("не установлена (403)"));
        assertFalse(row(forbidden, "issues").fact.contains("не хватает прав"));

        JSONObject limited = live().put("branchesFetched", false).put("branchesComplete", false)
                .put("warnings", new JSONArray().put("ветки: Лимит запросов GitHub исчерпан (429)."));
        assertTrue(row(limited, "branches").fact.contains("Лимит"));
        assertEquals("refresh", row(limited, "branches").action);

        JSONObject slow = live().put("pullsFetched", false).put("pullsComplete", false)
                .put("warnings", new JSONArray().put("предложения: GitHub не ответил вовремя."));
        assertTrue(row(slow, "pulls").fact.contains("Таймаут"));
        assertFalse(row(slow, "pulls").fact.contains("401"));
    }

    @Test public void staleCacheImportAndMissingMetadataDoNotBecomeComplete() throws Exception {
        JSONObject stale = live().put("issuesStale", true).put("issuesFetched", false).put("issuesComplete", false)
                .put("warnings", new JSONArray().put("задачи: показан прошлый список. Свежие данные этого раздела не получены.")
                        .put("задачи: Токен недействителен или истёк (401)."));
        assertEquals(Coverage.STALE_AFTER_FAILURE, row(stale, "issues").code);
        assertTrue(row(stale, "issues").fact.contains("401"));

        JSONObject cached = live().put("branchesCached", true);
        assertEquals(Coverage.CACHED_NOT_MODIFIED, row(cached, "branches").code);
        assertFalse(Coverage.COMPLETE_WITHIN_REQUEST.equals(row(cached, "branches").code));

        JSONObject imported = live().put("origin", "IMPORTED");
        assertEquals(Coverage.IMPORTED, row(imported, "branches").code);
        assertEquals(Coverage.IMPORTED, row(imported, "commits").code);

        JSONObject old = new JSONObject().put("repo", "demo/app").put("origin", "LIVE")
                .put("branchesComplete", true).put("branches", new JSONArray()).put("warnings", new JSONArray());
        assertEquals(Coverage.UNKNOWN, row(old, "branches").code);

        JSONObject quiet = live().put("issuesComplete", false);
        assertEquals(Coverage.UNKNOWN, row(quiet, "issues").code);

        JSONObject skipped = live().put("pullsFetched", false).put("pullsComplete", false)
                .put("warnings", new JSONArray().put("предложения: запрос не отправлялся, лимит GitHub ещё действует"));
        assertEquals(Coverage.NOT_REQUESTED, row(skipped, "pulls").code);
        assertEquals(Coverage.NOT_VERIFIED, row(null, "checks").code);
    }
}
