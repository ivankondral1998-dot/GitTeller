package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Newest lines already stored in a snapshot. Not a live GitHub feed and not a full history. */
final class HomeFeed {
    static final class Row {
        final String title;
        final String meta;
        final String page;
        final long when;

        Row(String title, String meta, String page, long when) {
            this.title = title;
            this.meta = meta;
            this.page = page;
            this.when = when;
        }
    }

    private HomeFeed() {}

    static List<Row> latest(JSONObject snapshot, long nowMs, int limit) {
        if (snapshot == null || limit <= 0) return Collections.emptyList();
        List<Row> rows = new ArrayList<>();
        addCommits(rows, snapshot.optJSONArray("commits"), nowMs);
        addIssues(rows, snapshot.optJSONArray("issues"), nowMs);
        addPulls(rows, snapshot.optJSONArray("pulls"), nowMs);
        rows.sort((a, b) -> Long.compare(b.when, a.when));
        if (rows.size() > limit) return new ArrayList<>(rows.subList(0, limit));
        return rows;
    }

    static String ago(long whenMs, long nowMs) {
        if (whenMs <= 0) return "";
        long delta = nowMs - whenMs;
        if (delta < 0) delta = 0;
        if (delta < 60_000L) return "только что";
        if (delta < 3_600_000L) return (delta / 60_000L) + " мин назад";
        if (delta < 86_400_000L) return (delta / 3_600_000L) + " ч назад";
        if (delta < 7L * 86_400_000L) return (delta / 86_400_000L) + " д назад";
        return "";
    }

    static long parseIso(String iso) {
        if (iso == null || iso.isEmpty() || "null".equals(iso)) return 0L;
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static void addCommits(List<Row> rows, JSONArray commits, long nowMs) {
        if (commits == null) return;
        int taken = 0;
        for (int i = 0; i < commits.length() && taken < 3; i++) {
            JSONObject row = commits.optJSONObject(i);
            if (row == null) continue;
            String message = firstLine(row.optString("message"));
            String title = message.isEmpty() ? "Запись без описания" : clip(message);
            long when = parseIso(row.optString("date"));
            rows.add(new Row(title, join(who(row.optString("author")), ago(when, nowMs)), "commits", when));
            taken++;
        }
    }

    private static void addIssues(List<Row> rows, JSONArray issues, long nowMs) {
        if (issues == null) return;
        int taken = 0;
        for (int i = 0; i < issues.length() && taken < 3; i++) {
            JSONObject row = issues.optJSONObject(i);
            if (row == null || !"open".equals(row.optString("state"))) continue;
            String title = clip(row.optString("title").trim());
            if (title.isEmpty()) title = "Задача без названия";
            int number = row.optInt("number", 0);
            String head = number > 0 ? "Задача #" + number + " · " + title : "Задача · " + title;
            long when = parseIso(row.optString("updated_at"));
            rows.add(new Row(head, join("открыта", ago(when, nowMs)), "tasks", when));
            taken++;
        }
    }

    private static void addPulls(List<Row> rows, JSONArray pulls, long nowMs) {
        if (pulls == null) return;
        int taken = 0;
        for (int i = 0; i < pulls.length() && taken < 3; i++) {
            JSONObject row = pulls.optJSONObject(i);
            if (row == null) continue;
            boolean open = "open".equals(row.optString("state"));
            boolean merged = row.optBoolean("merged", false);
            if (!open && !merged) continue;
            String title = clip(row.optString("title").trim());
            if (title.isEmpty()) title = "Предложение без названия";
            int number = row.optInt("number", 0);
            String kind = merged ? "Отмечено как внесённое" : "Предложение";
            String head = number > 0 ? kind + " #" + number + " · " + title : kind + " · " + title;
            String whenField = merged && !row.optString("merged_at").isEmpty()
                    ? row.optString("merged_at") : row.optString("updated_at");
            long when = parseIso(whenField);
            String state = merged ? "внесение не проверено на устройстве" : "ещё открыто";
            rows.add(new Row(head, join(state, ago(when, nowMs)), "pulls", when));
            taken++;
        }
    }

    private static String who(String author) {
        if (author == null) return "";
        String name = author.trim();
        return name.length() > 40 ? name.substring(0, 40) : name;
    }

    private static String join(String left, String right) {
        if (left == null || left.isEmpty()) return right == null ? "" : right;
        if (right == null || right.isEmpty()) return left;
        return left + " · " + right;
    }

    private static String firstLine(String message) {
        if (message == null) return "";
        String line = message.trim();
        int cut = line.indexOf('\n');
        if (cut >= 0) line = line.substring(0, cut).trim();
        return line;
    }

    private static String clip(String text) {
        if (text == null) return "";
        String clean = text.replace('\n', ' ').trim();
        return clean.length() > 72 ? clean.substring(0, 69) + "…" : clean;
    }
}
