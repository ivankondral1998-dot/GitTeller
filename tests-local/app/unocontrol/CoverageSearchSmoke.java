package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;

public final class CoverageSearchSmoke {
    private static void ok(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }

    public static void main(String[] args) throws Exception {
        JSONObject live = new JSONObject().put("repo", "demo/app").put("origin", "LIVE")
                .put("branches", new JSONArray().put(new JSONObject().put("name", "fix/a").put("sha", "abcd111111111111111111111111111111111111")))
                .put("issues", new JSONArray().put(new JSONObject().put("number", 6).put("title", "исправление")))
                .put("pulls", new JSONArray()).put("commits", new JSONArray())
                .put("branchesFetched", true).put("issuesFetched", true).put("pullsFetched", true).put("commitsFetched", true)
                .put("branchesComplete", true).put("issuesComplete", true).put("pullsComplete", true)
                .put("warnings", new JSONArray());
        ok(Coverage.COMPLETE_WITHIN_REQUEST.equals(Coverage.one(live, "branches").code), "complete");
        ok(Coverage.SAMPLED_BY_DESIGN.equals(Coverage.one(live, "commits").code), "sample");
        ok(Coverage.NOT_VERIFIED.equals(Coverage.one(live, "checks").code), "checks");
        ok(!Coverage.compact(live, false).contains("всё хорошо"), "no all-clear");
        JSONObject old = new JSONObject().put("origin", "LIVE").put("branchesComplete", true).put("warnings", new JSONArray());
        ok(Coverage.UNKNOWN.equals(Coverage.one(old, "branches").code), "old schema");
        JSONObject cap = new JSONObject().put("origin", "LIVE").put("branchesFetched", true).put("branchesComplete", false)
                .put("warnings", new JSONArray().put("ветки: предел 5 страниц"));
        ok(Coverage.TRUNCATED_BY_CAP.equals(Coverage.one(cap, "branches").code), "cap");
        JSONObject denied = new JSONObject().put("origin", "LIVE").put("issuesFetched", false).put("issuesComplete", false)
                .put("warnings", new JSONArray().put("задачи: Доступ ограничен (403): не хватает прав."));
        ok(Coverage.one(denied, "issues").fact.contains("не установлена (403)"), "plain 403");
        ok(!Coverage.one(denied, "issues").fact.contains("не хватает прав"), "403 not treated as proof");
        ProjectSearch.Report empty = ProjectSearch.find(live, new JSONArray(), "  ", null);
        ok(empty.emptyQuery && empty.hits.isEmpty(), "empty query");
        ok(!ProjectSearch.noMatch().contains("на GitHub нет"), "no remote denial");
        ProjectSearch.Report found = ProjectSearch.find(live, new JSONArray(), "#6", Collections.<String>emptyList());
        ok(found.hits.size() == 1 && "issue-detail".equals(found.hits.get(0).page), "hash deep link");
        ProjectSearch.Report cyr = ProjectSearch.find(live, new JSONArray(), "ИСПРАВЛЕНИЕ", null);
        ok(!cyr.hits.isEmpty() && "issue-detail".equals(cyr.hits.get(0).page), "cyrillic");
        DrawerMotion drawer = new DrawerMotion();
        ok(drawer.requestOpen() && !drawer.requestOpen(), "no double drawer");
        drawer.markOpen();
        ok(drawer.consumeBack() && !drawer.consumeBack() == false || drawer.closing, "back closes drawer");
        System.out.println("CoverageSearchSmoke: PASS");
    }
}
