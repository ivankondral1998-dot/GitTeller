package app.unocontrol;

import org.json.JSONObject;

/** Whether a shade notice is worth posting. Silence is the default. */
final class QuietNotice {
    private QuietNotice() {}

    static boolean worthTelling(JSONObject current, JSONObject previous) {
        if (current == null || previous == null) return false;
        if (SnapshotCodec.isImported(current) || SnapshotCodec.isImported(previous)) return false;
        if (!current.optString("repo").equalsIgnoreCase(previous.optString("repo"))) return false;
        Summary s = Summary.of(current, previous);
        return s.advanced + s.added + s.missing + s.issueChanges + s.pullChanges > 0;
    }

    /** One collapsed line. Not a verdict that the code works. */
    static String oneLine(JSONObject current, JSONObject previous) {
        Summary s = Summary.of(current, previous);
        String repo = current == null ? "Проект" : current.optString("repo", "Проект");
        if (repo.length() > 28) repo = repo.substring(0, 25) + "…";
        if (s.advanced + s.added + s.missing > 0) return repo + ": сдвинулись направления";
        if (s.issueChanges + s.pullChanges > 0) return repo + ": изменились задачи";
        return repo + ": есть изменение";
    }

    /** Expanded shade text. Still not a full-screen page. */
    static String detail(JSONObject current, JSONObject previous) {
        Summary s = Summary.of(current, previous);
        String text = HumanReport.whatChanged(current, previous, s);
        if (text == null) text = "";
        text = text.replace('\n', ' ').trim();
        if (text.length() > 180) text = text.substring(0, 177) + "…";
        return text;
    }
}
