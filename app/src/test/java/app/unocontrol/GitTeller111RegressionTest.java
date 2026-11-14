package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitTeller111RegressionTest {
    private static final String REPO = "example/gitteller";
    private static final String BRANCH = "research/topic";
    private static final String BEFORE = "a".repeat(40);
    private static final String AFTER = "b".repeat(40);

    private static JSONObject snapshot(String sha) throws Exception {
        return new JSONObject().put("repo", REPO)
                .put("branchesComplete", true).put("issuesComplete", true).put("pullsComplete", true)
                .put("commitsFetched", true)
                .put("branches", new JSONArray().put(new JSONObject().put("name", BRANCH).put("sha", sha)))
                .put("issues", new JSONArray()).put("pulls", new JSONArray()).put("commits", new JSONArray());
    }

    @Test public void ownerWhereRejectsImportedBaseline() throws Exception {
        JSONObject previous = snapshot(BEFORE).put("restoredFromBackup", true).put("origin", "IMPORTED");
        assertFalse(HumanReport.ownerWhere(snapshot(AFTER), previous).contains("Сдвинулись направления"));
    }

    @Test public void ownerWhereLabelsImportedCurrent() throws Exception {
        JSONObject current = snapshot(AFTER).put("restoredFromBackup", true).put("origin", "IMPORTED");
        assertTrue(HumanReport.ownerWhere(current, snapshot(BEFORE)).contains("импорт"));
    }

    @Test public void originAloneIdentifiesAnImportedSnapshot() throws Exception {
        JSONObject imported = snapshot(BEFORE).put("origin", "IMPORTED");
        assertFalse(HumanReport.ownerWhere(snapshot(AFTER), imported).contains("Сдвинулись направления"));
        assertTrue(HumanReport.ownerWhere(imported, snapshot(AFTER)).contains("импорт"));
        assertFalse(HumanReport.branchPointerLine(BRANCH, snapshot(AFTER), imported).contains("сдвинулась"));
        assertEquals(0, ChangeSet.between(imported, snapshot(AFTER)).length());
        assertEquals(0, Summary.of(snapshot(AFTER), imported).advanced);
    }

    @Test public void ownerWhereRejectsOtherReposAndKeepsLiveComparisons() throws Exception {
        assertFalse(HumanReport.ownerWhere(snapshot(AFTER), snapshot(BEFORE).put("repo", "other/repo"))
                .contains("Сдвинулись направления"));
        assertTrue(HumanReport.ownerWhere(snapshot(AFTER), snapshot(BEFORE).put("repo", "EXAMPLE/GitTeller"))
                .contains("Сдвинулись направления"));
    }

    @Test public void failedCommitFetchIsPartialInReportsEvenForOldLiveOrigin() throws Exception {
        JSONObject current = snapshot(AFTER).put("commitsFetched", false).put("origin", "LIVE");
        assertTrue(HumanReport.ownerStatus(current, snapshot(BEFORE), Summary.of(current, snapshot(BEFORE)))
                .contains("Часть данных недоступна"));
        assertTrue(HumanReport.syncStatus(current).contains("Проверка частичная"));
    }

    @Test public void unknownCommitFetchIsPartialInLegacyReports() throws Exception {
        JSONObject current = snapshot(AFTER);
        current.remove("commitsFetched");
        assertTrue(HumanReport.ownerStatus(current, null, Summary.of(current, null))
                .contains("Часть данных недоступна"));
        assertTrue(HumanReport.syncStatus(current).contains("Проверка частичная"));
    }

    @Test public void recentHistoryResponseAndCachedListsAreNotFetchFailures() throws Exception {
        JSONObject current = snapshot(AFTER).put("commitsComplete", false).put("historyPartial", true)
                .put("branchesCached", true);
        assertFalse(HumanReport.ownerStatus(current, current, Summary.of(current, current))
                .contains("Часть данных недоступна"));
        assertTrue(HumanReport.syncStatus(current).contains("GitHub подтвердил"));
    }

    @Test public void branchSeenExplainsWhichSnapshotHadTheBranch() throws Exception {
        JSONObject previous = snapshot(BEFORE).put("branches", new JSONArray());
        JSONObject current = snapshot(AFTER).put("branchesComplete", false);
        String kind = ChangeSet.between(previous, current).getJSONObject(0).getString("kind");
        assertEquals("branch_seen", kind);
        String meaning = HumanReport.eventMeaning(kind);
        assertTrue(meaning.contains("обнаружена в текущем") || meaning.contains("обнаружено сейчас"));
        assertTrue(meaning.contains("не было в предыдущем полном") || meaning.contains("отсутствовало в прошлом полном"));
        assertTrue(meaning.contains("Дата создания и автор неизвестны"));
        assertEquals(0, ChangeSet.between(previous.put("branchesComplete", false), current).length());
    }

    @Test public void branchDetailsShowKnownPointerMoveEvenWithPartialLists() throws Exception {
        for (boolean currentComplete : new boolean[]{false, true}) {
            for (boolean previousComplete : new boolean[]{false, true}) {
                JSONObject current = snapshot(AFTER).put("branchesComplete", currentComplete);
                JSONObject previous = snapshot(BEFORE).put("branchesComplete", previousComplete);
                assertTrue(branchDetail(current, previous).contains("сдвинулась"));
            }
        }
    }

    @Test public void partialListsSupportNeitherDeletionNorCreationFromAbsence() throws Exception {
        JSONObject previous = snapshot(BEFORE);
        JSONObject current = snapshot(AFTER).put("branchesComplete", false).put("branches", new JSONArray());
        assertEquals(0, ChangeSet.between(previous, current).length());
        assertTrue(HumanReport.branchPointerLine(BRANCH, current, previous).contains("Удаление не подтверждено"));
        assertTrue(branchDetail(previous, current).contains("Новым его называть нельзя"));
        assertTrue(branchDetail(previous, previous).contains("не сдвинулась"));
    }

    private static String branchDetail(JSONObject current, JSONObject previous) throws Exception {
        SectionActivity section = new SectionActivity();
        for (String name : new String[]{"current", "previous"}) {
            Field field = SectionActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(section, name.equals("current") ? current : previous);
        }
        for (Method delta : SectionActivity.class.getDeclaredMethods()) {
            if (!delta.getName().equals("branchDelta")) continue;
            delta.setAccessible(true);
            if (delta.getParameterCount() == 1) return (String) delta.invoke(section, BRANCH);
            return (String) delta.invoke(section, BRANCH,
                    current.getJSONArray("branches").getJSONObject(0).getString("sha"));
        }
        throw new AssertionError("Branch details have no branchDelta method");
    }
}
