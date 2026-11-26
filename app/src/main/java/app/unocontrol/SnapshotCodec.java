package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

/** Backup schema. Token and unknown keys are never copied. */
final class SnapshotCodec {
    private SnapshotCodec() {}

    static boolean isImported(JSONObject snapshot) {
        return snapshot != null && (snapshot.optBoolean("restoredFromBackup", false)
                || "IMPORTED".equals(snapshot.optString("origin")));
    }

    static void validateSnapshot(JSONObject snapshot, String expectedRepo) {
        if (snapshot == null) return;
        if (!expectedRepo.equalsIgnoreCase(snapshot.optString("repo")))
            throw new IllegalArgumentException("Репозиторий в снимке не совпадает с архивом.");
        JSONArray branches = snapshot.optJSONArray("branches");
        if (branches == null || branches.length() > 2000)
            throw new IllegalArgumentException("Некорректный список веток в архиве.");
        for (int i = 0; i < branches.length(); i++) {
            JSONObject branch = branches.optJSONObject(i);
            if (branch == null || branch.optString("name").isEmpty() ||
                    !branch.optString("sha").matches("[0-9a-fA-F]{40}"))
                throw new IllegalArgumentException("Некорректная запись ветки в архиве.");
        }
    }

    static JSONObject sanitizeSnapshot(JSONObject raw, String expectedRepo) throws Exception {
        validateSnapshot(raw, expectedRepo);
        JSONObject out = new JSONObject();
        out.put("repo", expectedRepo);
        out.put("capturedAt", raw.optLong("capturedAt", 0));
        out.put("defaultBranch", raw.optString("defaultBranch"));
        out.put("branchesComplete", raw.optBoolean("branchesComplete", false));
        // Missing fields in older archives mean unknown coverage, not an empty completed list.
        out.put("issuesComplete", raw.optBoolean("issuesComplete", false));
        out.put("pullsComplete", raw.optBoolean("pullsComplete", false));
        out.put("restoredFromBackup", isImported(raw));
        String origin = raw.optString("origin");
        if ("LIVE".equals(origin) || "CACHED".equals(origin) || "PARTIAL".equals(origin) || "IMPORTED".equals(origin))
            out.put("origin", origin);
        if (isImported(raw)) out.put("origin", "IMPORTED");
        copyTime(raw, out, "branchesFetchedAt");
        copyTime(raw, out, "issuesFetchedAt");
        copyTime(raw, out, "pullsFetchedAt");
        copyTime(raw, out, "commitsFetchedAt");
        for (String field : new String[]{"branches", "issues", "pulls", "commits"}) {
            if (raw.has(field + "Fetched")) out.put(field + "Fetched", raw.optBoolean(field + "Fetched", false));
        }
        out.put("commitsComplete", false);
        out.put("historyPartial", true);
        out.put("branchesStale", raw.optBoolean("branchesStale", false));
        out.put("issuesStale", raw.optBoolean("issuesStale", false));
        out.put("pullsStale", raw.optBoolean("pullsStale", false));
        out.put("commitsStale", raw.optBoolean("commitsStale", false));
        JSONArray branches = new JSONArray();
        JSONArray source = raw.optJSONArray("branches");
        for (int i = 0; i < source.length(); i++) {
            JSONObject b = source.optJSONObject(i);
            branches.put(new JSONObject().put("name", b.optString("name")).put("sha", b.optString("sha")));
        }
        out.put("branches", branches);
        String[] arrays = {"commits", "issues", "pulls", "warnings"};
        for (String k : arrays) {
            JSONArray rawArr = raw.optJSONArray(k);
            JSONArray clean = new JSONArray();
            if (rawArr != null) for (int i = 0; i < Math.min(rawArr.length(), 400); i++) {
                if ("warnings".equals(k)) {
                    String w = rawArr.optString(i);
                    clean.put(w.substring(0, Math.min(200, w.length())));
                } else {
                    JSONObject row = rawArr.optJSONObject(i);
                    if (row == null) continue;
                    if (("issues".equals(k) || "pulls".equals(k)) && row.optInt("number", -1) <= 0) continue;
                    JSONObject item = new JSONObject();
                    String[] names = "commits".equals(k) ? new String[]{"sha", "message", "date", "author"}
                            : new String[]{"number", "title", "state", "updated_at", "labels"};
                    for (String name : names) {
                        if ("number".equals(name)) item.put(name, row.optInt(name));
                        else {
                            String v = row.optString(name);
                            if (v.isEmpty() || "null".equals(v)) continue;
                            item.put(name, v.length() > 500 ? v.substring(0, 500) : v);
                        }
                    }
                    if ("pulls".equals(k)) {
                        String mergedAt = row.optString("merged_at");
                        boolean merged = row.optBoolean("merged", false)
                                || (!mergedAt.isEmpty() && !"null".equals(mergedAt));
                        item.put("merged", merged);
                        if (!mergedAt.isEmpty() && !"null".equals(mergedAt))
                            item.put("merged_at", mergedAt.length() > 80 ? mergedAt.substring(0, 80) : mergedAt);
                    }
                    clean.put(item);
                }
            }
            out.put(k, clean);
            if (("issues".equals(k) || "pulls".equals(k))
                    && (rawArr == null || clean.length() != rawArr.length())) {
                out.put(k + "Complete", false);
                if (!isImported(raw) && "LIVE".equals(out.optString("origin"))) out.put("origin", "PARTIAL");
            }
        }
        return out;
    }

    private static void copyTime(JSONObject raw, JSONObject out, String key) throws Exception {
        long value = raw.optLong(key, 0);
        if (value > 0) out.put(key, value);
    }
}
