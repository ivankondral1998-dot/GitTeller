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
        assertEquals("Новых изменений не обнаружено",s.heading);
    }
}
