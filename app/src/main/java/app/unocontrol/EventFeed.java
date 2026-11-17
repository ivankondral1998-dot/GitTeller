package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

/** The same kind of change can happen repeatedly; it must not be permanently deduplicated. */
final class EventFeed {
    private EventFeed() {}
    /** Fold a snapshot pair into the saved feed before the baseline moves forward. */
    static JSONArray remember(JSONArray prior, JSONObject older, JSONObject newer) throws Exception {
        long at = newer == null ? System.currentTimeMillis() : newer.optLong("capturedAt", System.currentTimeMillis());
        return merge(prior, ChangeSet.between(older, newer), at);
    }

    static JSONArray merge(JSONArray prior, JSONArray news, long capturedAt) throws Exception {
        JSONArray merged = new JSONArray();
        if (news != null) for (int i = 0; i < news.length() && merged.length() < 30; i++) {
            JSONObject row = news.optJSONObject(i);
            if (row == null) continue;
            row.put("at", capturedAt);
            merged.put(row);
        }
        if (prior != null) for (int i = 0; i < prior.length() && merged.length() < 30; i++) {
            JSONObject row = prior.optJSONObject(i);
            if (row != null) merged.put(row);
        }
        return merged;
    }
}
