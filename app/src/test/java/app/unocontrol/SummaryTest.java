package app.unocontrol;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.Assert.*;

public class SummaryTest {
    private static JSONObject snapshot(boolean complete, String... pairs) throws Exception {
        JSONObject s = new JSONObject();
        s.put("repo", "demo/project");
        JSONArray branches = new JSONArray();
        for (int i = 0; i < pairs.length; i += 2) {
            JSONObject b = new JSONObject();
            b.put("name", pairs[i]);
            b.put("sha", pairs[i + 1]);
            branches.put(b);
        }
        s.put("branches", branches);
        s.put("branchesComplete", complete);
        s.put("warnings", new JSONArray());
        return s;
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
        assertEquals("Новых изменений не обнаружено",s.heading);
    }
}
