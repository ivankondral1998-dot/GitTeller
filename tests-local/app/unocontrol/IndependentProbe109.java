package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

/** Host-side regression probes for audit 1.0.9. */
public final class IndependentProbe109 {
    private static int pass = 0;
    private static int failed = 0;
    static void expect(boolean value, String name) {
        if (!value) { System.err.println("FAIL: " + name); failed++; }
        else { pass++; System.out.println("PASS: " + name); }
    }
    static JSONObject branch(String name, String sha) {
        return new JSONObject().put("name",name).put("sha",sha);
    }
    static JSONObject snapshot(boolean complete, JSONArray branches) {
        return new JSONObject().put("repo","demo/proj")
                .put("branches",branches).put("branchesComplete",complete)
                .put("issues",new JSONArray()).put("pulls",new JSONArray())
                .put("issuesComplete",true).put("pullsComplete",true);
    }
    static boolean containsKind(JSONArray rows,String kind) {
        for (int i = 0; i < rows.length(); ++i) {
            JSONObject r = rows.optJSONObject(i);
            if (r != null && kind.equals(r.optString("kind"))) return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        String a="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String b="bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String c="cccccccccccccccccccccccccccccccccccccccc";
        JSONObject old = snapshot(false,new JSONArray().put(branch("research/test",a)));
        JSONObject now = snapshot(false,new JSONArray().put(branch("research/test",b)));
        Summary s=Summary.of(now,old);
        expect(s.advanced==1,"known moved pointer counted even in partial branch samples");
        expect(containsKind(ChangeSet.between(old,now),"branch_moved"),"known moved pointer appears in feed on partial list");
        JSONObject fullOld = snapshot(true,new JSONArray().put(branch("main",a)));
        JSONObject partialNow = snapshot(false,new JSONArray().put(branch("main",a)).put(branch("feature/new",b)));
        Summary s2=Summary.of(partialNow,fullOld);
        expect(s2.added==1,"newly seen branch against full old list recognized");
        String home=HumanReport.whatChanged(partialNow,fullOld,s2);
        expect(home.contains("изменил") || home.contains("впервые") || home.contains("появил"),"home does not hide observed new branch");
        JSONObject rollbackOld=snapshot(true,new JSONArray().put(branch("main",c)));
        JSONObject rollbackNow=snapshot(true,new JSONArray().put(branch("main",a)));
        Summary roll=Summary.of(rollbackNow,rollbackOld);
        expect(roll.advanced==1,"pointer rollback is a detected change");
        expect(!roll.plain.contains("появились новые записи"),"pointer rollback not called new history records");
        expect(containsKind(ChangeSet.between(rollbackOld,rollbackNow),"branch_moved"),"full sample still emits moved event");
        JSONObject noBranch=snapshot(true,new JSONArray());
        expect(containsKind(ChangeSet.between(fullOld,noBranch),"branch_absent"),"branch absent in two complete lists appears in feed without a deletion claim");
        System.out.println("CHECKS PASS="+pass+" FAIL="+failed);
        if (failed != 0) System.exit(2);
    }
}
