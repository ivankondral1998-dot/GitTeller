package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

/** Backup schema. Token and unknown keys are never copied. */
final class SnapshotCodec {
    private SnapshotCodec() {}

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
        out.put("restoredFromBackup", raw.optBoolean("restoredFromBackup", false));
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
                    JSONObject item = new JSONObject();
                    for (String name : ("commits".equals(k) ? new String[]{"sha", "message", "date"}
                            : new String[]{"number", "title", "state"})) {
                        if ("number".equals(name)) item.put(name, row.optInt(name));
                        else {
                            String v = row.optString(name);
                            item.put(name, v.length() > 500 ? v.substring(0, 500) : v);
                        }
                    }
                    clean.put(item);
                }
            }
            out.put(k, clean);
        }
        return out;
    }
}
