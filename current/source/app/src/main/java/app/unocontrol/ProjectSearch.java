package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Search over data already stored for the active project. Never calls GitHub. */
final class ProjectSearch {
    static final int PER_GROUP = 40;

    static final class Hit {
        final String group;
        final String title;
        final String detail;
        final String page;
        final int index;
        final String key;
        final String marker;

        Hit(String group, String title, String detail, String page, int index, String key, String marker) {
            this.group = group;
            this.title = title;
            this.detail = detail;
            this.page = page;
            this.index = index;
            this.key = key == null ? "" : key;
            this.marker = marker == null ? "" : marker;
        }
    }

    static final class Report {
        final String scope;
        final List<Hit> hits;
        final int omitted;
        final boolean emptyQuery;

        Report(String scope, List<Hit> hits, int omitted, boolean emptyQuery) {
            this.scope = scope;
            this.hits = hits;
            this.omitted = omitted;
            this.emptyQuery = emptyQuery;
        }
    }

    private ProjectSearch() {}

    static Report find(JSONObject snapshot, JSONArray feed, String rawQuery, List<String> localRepos) {
        String query = norm(rawQuery);
        String scope = scope(snapshot);
        if (query.isEmpty()) return new Report(scope, Collections.<Hit>emptyList(), 0, true);
        List<Hit> hits = new ArrayList<>();
        int omitted = 0;
        omitted += take(hits, "Разделы", sections(query));
        if (localRepos != null) omitted += take(hits, "Проекты", projects(localRepos, query));
        omitted += take(hits, "Ветки", rows(snapshot, "branches", "Ветки", "branch-detail", query, "name", "sha"));
        omitted += take(hits, "Задачи", numbered(snapshot, "issues", "Задачи", "issue-detail", query));
        omitted += take(hits, "PR", numbered(snapshot, "pulls", "PR", "pull-detail", query));
        omitted += take(hits, "Коммиты", commits(snapshot, query));
        omitted += take(hits, "Изменения", events(feed, query));
        omitted += take(hits, "Диагностика", diagnostics(snapshot, query));
        return new Report(scope, hits, omitted, false);
    }

    static String scope(JSONObject snapshot) {
        if (snapshot == null)
            return "Снимок не сохранён. Поиск идёт только по разделам приложения и не обращается к GitHub.";
        String repo = snapshot.optString("repo", "проект не указан");
        String origin = SnapshotCodec.isImported(snapshot) ? "IMPORTED" : snapshot.optString("origin", "не определён");
        boolean stale = snapshot.optBoolean("branchesStale", false) || snapshot.optBoolean("issuesStale", false)
                || snapshot.optBoolean("pullsStale", false) || snapshot.optBoolean("commitsStale", false);
        return "Проект: " + repo + ". Источник: " + origin + "."
                + (stale ? " Есть устаревшие списки." : "")
                + " Это сохранённые данные, не весь GitHub.";
    }

    static String noMatch() {
        return "В сохранённых данных совпадений нет. Это не доказательство, что на GitHub такого нет.";
    }

    static String emptyHint() {
        return "Введите запрос. Поиск смотрит сохранённые данные этого проекта, а не весь GitHub.";
    }

    private static int take(List<Hit> into, String group, List<Hit> found) {
        int extra = 0;
        for (int i = 0; i < found.size(); i++) {
            if (i < PER_GROUP) into.add(found.get(i));
            else extra++;
        }
        return extra;
    }

    private static List<Hit> sections(String query) {
        String[][] table = {
                {"Разделы проекта", "index"},
                {"Обзор", "overview"},
                {"Изменения", "changes"},
                {"Направления работы", "branches"},
                {"Проблемы", "problems"},
                {"История изменений", "commits"},
                {"Задачи", "tasks"},
                {"Предложения изменений", "pulls"},
                {"Надёжность сведений", "accuracy"},
                {"Подключение GitHub", "access"},
                {"Настройки", "settings"},
                {"Почему данные неполные", "incomplete"},
                {"О приложении", "about"},
                {"Поиск", "search"}
        };
        List<Hit> out = new ArrayList<>();
        for (String[] row : table) {
            if (contains(row[0], query))
                out.add(new Hit("Разделы", row[0], "Раздел приложения", row[1], -1, "", ""));
        }
        return out;
    }

    private static List<Hit> projects(List<String> repos, String query) {
        List<Hit> out = new ArrayList<>();
        for (String repo : repos) {
            if (repo == null || repo.isEmpty()) continue;
            if (contains(repo, query))
                out.add(new Hit("Проекты", clip(repo), "Сохранён на этом устройстве. Откроет этот проект, не чужие записи.",
                        "switch-repo", -1, repo, ""));
        }
        return out;
    }

    private static List<Hit> rows(JSONObject snapshot, String field, String group, String page, String query,
                                  String nameKey, String shaKey) {
        List<Hit> out = new ArrayList<>();
        JSONArray list = snapshot == null ? null : snapshot.optJSONArray(field);
        if (list == null) return out;
        boolean sha = shaQuery(query);
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row == null) continue;
            String name = row.optString(nameKey);
            String id = row.optString(shaKey);
            if (contains(name, query) || contains(id, query) || (sha && starts(id, query)))
                out.add(new Hit(group, clip(name.isEmpty() ? id : name), clip(id), page, i, name.isEmpty() ? id : name, ""));
        }
        return out;
    }

    private static List<Hit> numbered(JSONObject snapshot, String field, String group, String page, String query) {
        List<Hit> out = new ArrayList<>();
        JSONArray list = snapshot == null ? null : snapshot.optJSONArray(field);
        if (list == null) return out;
        Integer exact = hashNumber(query);
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row == null) continue;
            int number = row.optInt("number", -1);
            String title = row.optString("title");
            boolean match = contains(title, query) || contains(number < 0 ? "" : ("#" + number), query)
                    || (exact != null && exact.intValue() == number);
            if (!match) continue;
            String key = number < 0 ? "" : Integer.toString(number);
            out.add(new Hit(group, clip(title.isEmpty() ? ("#" + key) : title),
                    number < 0 ? "без номера" : ("#" + number), page, i, key, ""));
        }
        return out;
    }

    private static List<Hit> commits(JSONObject snapshot, String query) {
        List<Hit> out = new ArrayList<>();
        JSONArray list = snapshot == null ? null : snapshot.optJSONArray("commits");
        if (list == null) return out;
        boolean sha = shaQuery(query);
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row == null) continue;
            String id = row.optString("sha");
            String message = row.optString("message");
            String author = row.optString("author");
            if (contains(message, query) || contains(author, query) || contains(id, query) || (sha && starts(id, query)))
                out.add(new Hit("Коммиты", clip(message.isEmpty() ? id : message), clip(author + " " + id),
                        "commit-detail", i, id, ""));
        }
        return out;
    }

    private static List<Hit> events(JSONArray feed, String query) {
        List<Hit> out = new ArrayList<>();
        if (feed == null) return out;
        for (int i = 0; i < feed.length(); i++) {
            JSONObject event = feed.optJSONObject(i);
            if (event == null) continue;
            String text = event.optString("event");
            String id = event.optString("id");
            String kind = event.optString("kind");
            if (contains(text, query) || contains(id, query) || contains(kind, query))
                out.add(new Hit("Изменения", clip(text.isEmpty() ? kind : text), clip(kind), "changes", i, "", id));
        }
        return out;
    }

    private static List<Hit> diagnostics(JSONObject snapshot, String query) {
        List<Hit> out = new ArrayList<>();
        if (snapshot == null) return out;
        JSONArray warnings = snapshot.optJSONArray("warnings");
        if (warnings != null) {
            for (int i = 0; i < warnings.length(); i++) {
                String line = warnings.optString(i);
                if (line == null || line.isEmpty() || !contains(line, query)) continue;
                if (line.toLowerCase(Locale.ROOT).contains("token") && line.contains("ghp_")) continue;
                out.add(new Hit("Диагностика", clip(line), "Записанное предупреждение загрузки", "incomplete", -1, "", ""));
            }
        }
        for (Coverage.Row row : Coverage.explain(snapshot)) {
            if ("snapshot".equals(row.area)) continue;
            if (contains(row.novice, query) || contains(row.developer, query) || contains(row.code, query))
                out.add(new Hit("Диагностика", clip(row.title + ": " + row.novice), row.code, "incomplete", -1, row.area, ""));
        }
        return out;
    }

    static String norm(String raw) {
        if (raw == null) return "";
        String value = raw.replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!space && b.length() > 0) {
                    b.append(' ');
                    space = true;
                }
            } else {
                b.append(c);
                space = false;
            }
        }
        return b.toString();
    }

    private static boolean contains(String hay, String query) {
        return hay != null && !query.isEmpty() && norm(hay).contains(query);
    }

    private static boolean starts(String hay, String query) {
        return hay != null && norm(hay).startsWith(query);
    }

    static boolean shaQuery(String query) { return query != null && query.matches("[0-9a-f]{4,40}"); }

    static Integer hashNumber(String query) {
        if (query == null || !query.startsWith("#")) return null;
        String digits = query.substring(1);
        if (!digits.matches("\\d{1,9}")) return null;
        try { return Integer.valueOf(digits); }
        catch (NumberFormatException e) { return null; }
    }

    private static String clip(String value) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= 160 ? clean : clean.substring(0, 160) + "…";
    }
}
