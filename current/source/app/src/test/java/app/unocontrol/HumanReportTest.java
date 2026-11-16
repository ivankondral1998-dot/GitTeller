package app.unocontrol;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HumanReportTest {
    @Test public void unknownBranchIsNotAProjectGuess() {
        assertEquals("Тема по названию не определена", HumanReport.branchTopic("research/uno-train"));
        assertFalse(HumanReport.branchTopic("feat/memory-core").contains("Память"));
        assertEquals("Основная ветка проекта", HumanReport.branchKind("main"));
    }

    @Test public void closedIsNotMergedAndChecksAreAbsent() {
        assertTrue(HumanReport.state("closed").contains("не проверен"));
        assertFalse(HumanReport.state("closed").toLowerCase().contains("merge"));
        assertEquals("Недостаточно данных", HumanReport.NOT_ENOUGH);
        assertEquals("Проверка работоспособности отсутствует", HumanReport.NO_CHECK);
    }

    @Test public void commitsAreScopedToTheDefaultBranch() throws Exception {
        JSONObject snap = new JSONObject();
        snap.put("defaultBranch", "release");
        assertEquals("ветки «release»", HumanReport.commitScope(snap));
    }

    @Test public void ownerAnswersStayTiedToEvidence() throws Exception {
        JSONObject first = new JSONObject().put("repo", "demo/proj").put("capturedAt", 1_700_000_000_000L)
                .put("branchesComplete", true).put("issuesComplete", true).put("pullsComplete", true)
                .put("defaultBranch", "main")
                .put("branches", new org.json.JSONArray().put(new JSONObject().put("name", "main").put("sha", "a")))
                .put("issues", new org.json.JSONArray())
                .put("pulls", new org.json.JSONArray())
                .put("commits", new org.json.JSONArray().put(new JSONObject()
                        .put("message", "Initial").put("date", "2026-01-01T00:00:00Z").put("author", "octocat")));
        Summary none = Summary.of(first, null);
        String status = HumanReport.ownerStatus(first, null, none);
        assertTrue(status.contains("Нет достоверного сравнения"));
        assertTrue(status.contains("Сравнивать не с чем"));
        String changed = HumanReport.ownerChanged(first, null, none);
        assertTrue(changed.contains("Базы") || changed.contains("первая"));
        String who = HumanReport.ownerWho(first).toLowerCase();
        assertTrue(who.contains("octocat"));
        assertFalse(who.contains("grok"));
        assertFalse(who.contains("codex"));
        assertFalse(who.contains("агент работает"));
        assertTrue(who.contains("не установлен"));

        JSONObject before = new JSONObject(first.toString());
        JSONObject after = new JSONObject(first.toString());
        after.put("capturedAt", 1_700_000_100_000L);
        after.put("branches", new org.json.JSONArray().put(new JSONObject().put("name", "fix/android-summary-wording").put("sha", "b")));
        before.put("branches", new org.json.JSONArray().put(new JSONObject().put("name", "fix/android-summary-wording").put("sha", "a")));
        Summary moved = Summary.of(after, before);
        String line = HumanReport.ownerChanged(after, before, moved).toLowerCase();
        assertFalse(line.contains("sha"));
        assertTrue(line.contains("не запрашивалось") || line.contains("файлы"));
        String where = HumanReport.ownerWhere(after, before);
        assertTrue(where.contains("fix/android-summary-wording"));
        String pointer = HumanReport.branchPointerLine("fix/android-summary-wording", after, before);
        assertTrue(pointer.contains("ещё неизвестен") || pointer.contains("сдвинулась"));
        assertFalse(pointer.toLowerCase().contains("sha"));

        JSONObject tasks = new JSONObject(after.toString());
        tasks.put("issues", new org.json.JSONArray()
                .put(new JSONObject().put("number", 6).put("title", "Write the notes").put("state", "open"))
                .put(new JSONObject().put("number", 7).put("title", "Crash on launch").put("state", "open").put("labels", "bug")));
        String problems = HumanReport.ownerProblems(tasks);
        String problemsLower = problems.toLowerCase();
        assertTrue(problems.contains("не подтверждённая ошибка"));
        assertTrue(problems.contains("Write the notes"));
        assertTrue(problemsLower.contains("по названию"));
        assertTrue(problems.contains("Crash on launch"));
        assertFalse(problems.contains("ошибка исправлена"));
        assertEquals("task", HumanReport.problemKind("Write the notes", ""));
        assertEquals("defect", HumanReport.problemKind("Crash on launch", "bug"));
        assertEquals("blocker", HumanReport.problemKind("Release blocked", ""));
        assertTrue(problems.contains("не означает успех") || problems.contains("не означает, что проверки"));

        JSONObject oldIssue = new JSONObject().put("repo", "demo/proj").put("branchesComplete", true)
                .put("issuesComplete", true).put("pullsComplete", true)
                .put("branches", new org.json.JSONArray())
                .put("issues", new org.json.JSONArray().put(new JSONObject().put("number", 6).put("title", "Notes")
                        .put("state", "open").put("updated_at", "2026-01-01T00:00:00Z")))
                .put("pulls", new org.json.JSONArray());
        JSONObject newIssue = new JSONObject(oldIssue.toString());
        newIssue.getJSONArray("issues").getJSONObject(0).put("updated_at", "2026-02-01T00:00:00Z");
        String touch = ChangeSet.between(oldIssue, newIssue).getJSONObject(0).getString("event");
        assertTrue(touch.contains("неизвестно"));
        assertFalse(touch.contains("исправлен"));
        String once = HumanReport.unknownOnce("Не подтверждено: Не подтверждено");
        assertTrue(once.startsWith("Что не проверено:"));
        assertFalse(once.contains("Не подтверждено: Не"));
        assertFalse(once.toLowerCase().contains("не подтверждено: не подтверждено"));
    }
}
