package app.unocontrol;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.Assert.*;

public class SummaryTest {
    private static JSONObject snapshot(boolean complete, String... pairs) throws Exception {
        JSONObject s = new JSONObject();
        JSONArray branches = new JSONArray();
        for (int i=0;i<pairs.length;i+=2)
            branches.put(new JSONObject().put("name",pairs[i]).put("sha",pairs[i+1]));
        return s.put("repo","demo/project")
                .put("branches",branches).put("branchesComplete",complete)
                .put("warnings",new JSONArray());
    }

    @Test public void firstSnapshotNeedsAnotherRefresh() throws Exception {
        assertEquals("Первый снимок сохранён",Summary.of(snapshot(true,"main","aaa"),null).heading);
    }
    @Test public void distinguishesMovedAddedAndMissing() throws Exception {
        Summary s=Summary.of(snapshot(true,"main","bbb","feat","ccc"),
                snapshot(true,"main","aaa","old","zzz"));
        assertEquals(1,s.advanced);assertEquals(1,s.added);assertEquals(1,s.missing);
    }
    @Test public void incompleteResultMustNotClaimBranchDisappearance() throws Exception {
        Summary s=Summary.of(snapshot(false,"main","bbb"),
                snapshot(true,"main","aaa","old","zzz"));
        assertEquals(0,s.missing);
        assertTrue(s.description.contains("не оценивается"));
    }
    @Test public void unchangedBranchHeadDoesNotClaimProgress() throws Exception {
        Summary s=Summary.of(snapshot(true,"main","aaa"),snapshot(true,"main","aaa"));
        assertEquals("SHA веток не изменились",s.heading);
    }
    @Test public void partialComparisonCannotAssertNoChanges() throws Exception {
        Summary s=Summary.of(snapshot(false,"main","aaa"),snapshot(true,"main","aaa"));
        assertEquals("Сравнение неполное",s.heading);
    }
    @Test public void differentRepositoryIsNotADiff() throws Exception {
        JSONObject other = snapshot(true, "main", "aaa");
        other.put("repo", "other/project");
        Summary s = Summary.of(snapshot(true, "main", "bbb"), other);
        assertEquals("Первый снимок сохранён", s.heading);
        assertEquals(0, s.missing);
    }
    @Test public void issueStateChangeIsVisibleWithoutClaimingAPassingTest() throws Exception {
        JSONObject now = snapshot(true, "main", "aaa");
        JSONObject old = snapshot(true, "main", "aaa");
        now.put("issues", new JSONArray().put(new JSONObject().put("number", 7).put("title", "login").put("state", "closed")));
        old.put("issues", new JSONArray().put(new JSONObject().put("number", 7).put("title", "login").put("state", "open")));
        Summary s = Summary.of(now, old);
        assertEquals("SHA веток не изменились", s.heading);
        assertTrue(s.description.contains("1 задач"));
        assertTrue(s.plain.contains("не доказательство") || s.attention.contains("не доказательство"));
        assertFalse(s.plain.contains("PASS"));
    }
    @Test public void warningsLimitTheClaim() throws Exception {
        JSONObject now = snapshot(true, "main", "aaa");
        now.put("warnings", new JSONArray().put("задачи: загрузка прервана"));
        Summary s = Summary.of(now, snapshot(true, "main", "aaa"));
        assertTrue(s.attention.contains("Выводы ограничены"));
    }
    @Test public void emptyIssueListDoesNotInventWork() throws Exception {
        JSONObject now = snapshot(true, "main", "aaa");
        now.put("issues", new JSONArray());
        now.put("pulls", new JSONArray());
        Summary s = Summary.of(now, snapshot(true, "main", "aaa"));
        assertFalse(s.description.contains("Статус изменился"));
    }
}
