package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TruthTest {
    private static JSONObject snap(String... pairs) throws Exception {
        JSONObject s = new JSONObject().put("repo", "demo/app").put("branchesComplete", true)
                .put("issuesComplete", true).put("pullsComplete", true).put("warnings", new JSONArray());
        JSONArray branches = new JSONArray();
        for (int i = 0; i < pairs.length; i += 2)
            branches.put(new JSONObject().put("name", pairs[i]).put("sha", pairs[i + 1]));
        return s.put("branches", branches);
    }

    @Test public void researchBranchMoveIsVisibleWhenMainIsStill() throws Exception {
        JSONObject old = snap("main", "aaa", "research/idea", "bbb");
        JSONObject now = snap("main", "aaa", "research/idea", "ccc");
        Summary s = Summary.of(now, old);
        assertTrue(s.advanced >= 1);
        String line = HumanReport.whatChanged(now, old, s);
        assertTrue(line.contains("исследователь"));
        assertTrue(line.contains("не проверена"));
        assertFalse(line.toLowerCase().contains("sha"));
    }

    @Test public void firstSnapshotIsNotAComparison() throws Exception {
        JSONObject now = snap("main", "aaa");
        String line = HumanReport.whatChanged(now, null, Summary.of(now, null));
        assertTrue(line.contains("Базы") || line.contains("первая"));
        assertFalse(line.contains("не сдвинулись"));
        assertFalse(line.contains("изменений нет"));
    }

    @Test public void titleOnlyDocsDoesNotNameFiles() {
        String text = HumanReport.explainClaim("docs(agents): restore canonical work log route [skip ci]", null);
        assertTrue(text.contains("документы") || text.contains("документ"));
        assertTrue(text.contains("не получен") || text.contains("Недостаточно данных"));
        assertTrue(text.contains("skip ci") || text.contains("[skip ci]"));
        assertFalse(text.contains("AGENTS.md"));
        assertFalse(text.contains("PASS"));
    }

    @Test public void docsWithRealFilesMayNameThoseFilesOnly() throws Exception {
        JSONArray files = new JSONArray().put(new JSONObject().put("filename", "AGENTS.md").put("status", "modified"))
                .put(new JSONObject().put("filename", "README.md").put("status", "modified"));
        String text = HumanReport.explainClaim("docs(agents): restore canonical work log route [skip ci]", files);
        assertTrue(text.contains("AGENTS.md"));
        assertTrue(text.contains("README.md"));
        assertTrue(text.contains("не доказательство"));
        assertTrue(text.contains("skip ci") || text.contains("[skip ci]"));
    }

    @Test public void fixPrefixIsNotAPassingRepair() {
        String text = HumanReport.explainClaim("fix: crash on start", null);
        assertTrue(text.contains("заявление") || text.contains("сообщил"));
        assertFalse(text.contains("исправлено и работает"));
        assertTrue(text.contains("Недостаточно данных") || text.contains("не получен"));
    }

    @Test public void skippedAndNeutralAreNotSuccess() throws Exception {
        String checks = GithubClient.formatChecks(new JSONObject().put("total_count", 2).put("check_runs", new JSONArray()
                .put(new JSONObject().put("conclusion", "skipped"))
                .put(new JSONObject().put("conclusion", "neutral"))));
        assertTrue(checks.contains("пропущенных или нейтральных"));
        assertFalse(checks.contains("2 с результатом success"));
    }

    @Test public void closedPullIsNotMerged() throws Exception {
        String text = GithubClient.formatPull(new JSONObject().put("merged", false).put("state", "closed"),
                new JSONArray(), false, null, "Результаты автоматических проверок получить не удалось.").text;
        assertTrue(text.contains("не сообщает"));
        assertFalse(text.contains("отмечает, что предложение слито"));
    }

    @Test public void realMergeIsNotADeviceTest() throws Exception {
        String text = GithubClient.formatPull(new JSONObject().put("merged", true).put("merged_at", "2026-01-01T00:00:00Z"),
                new JSONArray().put(new JSONObject().put("filename", "A.java").put("status", "modified")),
                false, null, "В загруженной части GitHub-проверок: 1 с результатом success, 0 пропущенных или нейтральных, 0 с ошибкой, 0 без итога.").text;
        assertTrue(text.contains("слито"));
        assertTrue(text.contains("не заменяет проверку"));
    }

    @Test public void cutFileListIsDisclosed() throws Exception {
        String text = GithubClient.formatCommit(new JSONObject().put("files", new JSONArray()
                .put(new JSONObject().put("filename", "A.java").put("status", "modified"))), "нет", true).text;
        assertTrue(text.contains("неполный"));
    }

    @Test public void missingStatusesAreNotSuccess() throws Exception {
        String text = GithubClient.formatStatuses(new JSONObject().put("state", "success").put("statuses", new JSONArray()));
        assertTrue(text.contains("получить не удалось"));
        assertFalse(text.contains("успешн"));
    }

    @Test public void timeoutIsNotRawEnglish() {
        String text = GithubClient.safeMessage(new java.net.SocketTimeoutException("Read timed out"));
        assertTrue(text.contains("не ответил"));
        assertFalse(text.contains("Read timed out"));
        assertTrue(text.contains("сохранены"));
    }

    @Test public void permission403IsNotCalledABadToken() {
        String text = GithubClient.errorForStatus(403, "5");
        assertTrue(text.contains("403"));
        assertTrue(text.contains("не автоматически"));
        assertFalse(text.contains("Лимит"));
    }

    @Test public void issueChangeSurvivesEqualBranchPointers() throws Exception {
        JSONObject old = snap("main", "aaa", "research/idea", "bbb");
        JSONObject now = snap("main", "aaa", "research/idea", "bbb");
        old.put("issues", new JSONArray().put(new JSONObject().put("number", 4).put("title", "Сборка падает").put("state", "open")));
        now.put("issues", new JSONArray().put(new JSONObject().put("number", 4).put("title", "Сборка падает").put("state", "closed")));
        Summary s = Summary.of(now, old);
        assertEquals(0, s.advanced);
        assertEquals(1, s.issueChanges);
        String line = HumanReport.whatChanged(now, old, s);
        assertTrue(line.contains("статус"));
        assertFalse(line.contains("не сдвинулись"));
    }

    @Test public void attentionNamesAnOpenTaskAndDoesNotInventOne() throws Exception {
        JSONObject now = snap("main", "aaa");
        now.put("issues", new JSONArray().put(new JSONObject().put("number", 3).put("title", "Не открывается архив").put("state", "open")));
        String line = HumanReport.needsAttention(now, Summary.of(now, null));
        assertTrue(line.contains("Не открывается архив"));
        assertFalse(line.contains("следующая задача"));
        JSONObject empty = snap("main", "aaa");
        empty.put("issues", new JSONArray());
        String none = HumanReport.needsAttention(empty, Summary.of(empty, null));
        assertTrue(none.contains("проверк"));
        assertFalse(none.contains("«"));
    }

    @Test public void failedIssueRefreshKeepsPreviousTasks() throws Exception {
        JSONArray issues = new JSONArray().put(new JSONObject().put("number", 9).put("title", "Доступ").put("state", "open"));
        JSONObject previous = new JSONObject().put("repo", "demo/app").put("issues", issues).put("issuesComplete", true);
        JSONObject fresh = new JSONObject().put("repo", "demo/app")
                .put("issues", new JSONArray()).put("issuesFetched", false).put("issuesComplete", true);
        GithubClient.preserveSources(fresh, previous);
        assertEquals(1, fresh.getJSONArray("issues").length());
        assertTrue(fresh.getBoolean("issuesStale"));
        assertFalse(fresh.getBoolean("issuesComplete"));
        assertTrue(fresh.getJSONArray("warnings").getString(0).contains("прошлый список"));
    }

    @Test public void freshIssuesAreNotReplacedByOldOnes() throws Exception {
        JSONObject previous = new JSONObject().put("repo", "demo/app")
                .put("issues", new JSONArray().put(new JSONObject().put("number", 1).put("state", "open")));
        JSONObject fresh = new JSONObject().put("repo", "demo/app")
                .put("issues", new JSONArray()).put("issuesFetched", true).put("issuesComplete", true);
        GithubClient.preserveSources(fresh, previous);
        assertEquals(0, fresh.getJSONArray("issues").length());
        assertFalse(fresh.optBoolean("issuesStale", false));
    }

    @Test public void otherRepositoryDoesNotReceiveKeptTasks() throws Exception {
        JSONObject previous = new JSONObject().put("repo", "other/app")
                .put("issues", new JSONArray().put(new JSONObject().put("number", 1).put("state", "open")));
        JSONObject fresh = new JSONObject().put("repo", "demo/app")
                .put("issues", new JSONArray()).put("issuesFetched", false);
        GithubClient.preserveSources(fresh, previous);
        assertEquals(0, fresh.getJSONArray("issues").length());
    }

    @Test public void missingCiSourcesAreNotAPass() {
        assertEquals(GithubClient.CI_UNAVAILABLE, GithubClient.describeCi(null, null, null));
        String text = GithubClient.describeCi(null, GithubClient.CI_UNAVAILABLE, null);
        assertTrue(text.contains("получить не удалось"));
        assertFalse(text.contains("успешн"));
    }

    @Test public void statusFallbackIsUsedWhenChecksAreEmpty() throws Exception {
        String checks = GithubClient.formatChecks(new JSONObject().put("check_runs", new JSONArray()));
        String statuses = GithubClient.formatStatuses(new JSONObject().put("statuses", new JSONArray()
                .put(new JSONObject().put("state", "failure"))));
        String text = GithubClient.describeCi(checks, statuses, null);
        assertTrue(text.contains("с ошибкой"));
        assertTrue(text.contains("не означают успех"));
        assertFalse(text.contains("всё успешно"));
    }

    @Test public void actionsSkippedIsNotSuccess() throws Exception {
        String actions = GithubClient.formatActions(new JSONObject().put("workflow_runs", new JSONArray()
                .put(new JSONObject().put("status", "completed").put("conclusion", "skipped"))
                .put(new JSONObject().put("status", "completed").put("conclusion", "neutral"))));
        assertTrue(actions.contains("пропущенных или нейтральных"));
        assertFalse(actions.contains("2 с результатом success"));
        String text = GithubClient.describeCi(null, null, actions);
        assertTrue(text.contains("пропущенных"));
        assertFalse(text.contains("PASS"));
    }

    @Test public void closedUnmergedPullLabelDoesNotSayMerged() throws Exception {
        String label = HumanReport.pullListLabel(new JSONObject().put("state", "closed").put("merged", false));
        assertTrue(label.contains("не отмечает"));
        assertFalse(label.contains("Внесено"));
        String merged = HumanReport.pullListLabel(new JSONObject().put("state", "closed").put("merged", true));
        assertTrue(merged.contains("Внесено"));
        assertTrue(merged.contains("не проверка"));
    }

    @Test public void branchPrefixIsAnAssumption() {
        String text = HumanReport.branchAssumption("research/idea");
        assertTrue(text.contains("Предположение по названию"));
        assertTrue(text.contains("не установленный факт"));
        assertEquals("Исследования", HumanReport.branchKind("research/idea"));
    }

    @Test public void coverageDoesNotCallPartialHistoryComplete() throws Exception {
        JSONObject now = snap("main", "aaa");
        now.put("branchesComplete", false);
        String line = HumanReport.coverageLine(now);
        assertTrue(line.contains("не полностью"));
        assertTrue(line.contains("История изучена лишь частично"));
        now.put("branchesComplete", true);
        assertTrue(HumanReport.coverageLine(now).contains("проверены полностью"));
        assertTrue(HumanReport.coverageLine(now).contains("лишь частично"));
    }
}
