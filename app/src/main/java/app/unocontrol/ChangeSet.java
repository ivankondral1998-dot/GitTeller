package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Typed differences between two snapshots. Absence in an incomplete list is not deletion. */
final class ChangeSet {
    private ChangeSet() {}

    static int issueChanges(JSONObject previous, JSONObject current) {
        return count(previous, current, "issues", "issuesComplete", false);
    }

    static int pullChanges(JSONObject previous, JSONObject current) {
        return count(previous, current, "pulls", "pullsComplete", true);
    }

    static String homeLine(JSONObject previous, JSONObject current) {
        try {
            JSONArray events = between(previous, current);
            if (events.length() == 0) return null;
            StringBuilder b = new StringBuilder();
            int n = 0;
            for (int i = 0; i < events.length() && n < 2; i++) {
                JSONObject e = events.optJSONObject(i);
                if (e == null) continue;
                String kind = e.optString("kind");
                if (kind.startsWith("branch")) continue;
                if (n > 0) b.append(" ");
                b.append(e.optString("event"));
                n++;
            }
            if (n == 0) return null;
            b.append(" Это не доказательство, что программа работает.");
            return b.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    static JSONArray between(JSONObject previous, JSONObject current) throws Exception {
        JSONArray out = new JSONArray();
        if (previous == null || current == null) return out;
        if (current.optBoolean("restoredFromBackup", false) || previous.optBoolean("restoredFromBackup", false))
            return out;
        if (!current.optString("repo").equalsIgnoreCase(previous.optString("repo"))) return out;
        String repo = current.optString("repo");
        boolean issuesOk = previous.optBoolean("issuesComplete", false) && current.optBoolean("issuesComplete", false);
        boolean pullsOk = previous.optBoolean("pullsComplete", false) && current.optBoolean("pullsComplete", false);
        boolean branchesOk = previous.optBoolean("branchesComplete", false) && current.optBoolean("branchesComplete", false);
        String origin = originOf(current);
        diffRecords(out, previous.optJSONArray("issues"), current.optJSONArray("issues"), false, repo, "issue", origin, issuesOk);
        diffRecords(out, previous.optJSONArray("pulls"), current.optJSONArray("pulls"), true, repo, "pull", origin, pullsOk);
        if (branchesOk) diffBranches(out, previous.optJSONArray("branches"), current.optJSONArray("branches"), repo, origin);
        return out;
    }

    private static int count(JSONObject previous, JSONObject current, String array, String flag, boolean pulls) {
        if (previous == null || current == null) return 0;
        boolean full = previous.optBoolean(flag, false) && current.optBoolean(flag, false);
        JSONArray old = previous.optJSONArray(array);
        JSONArray now = current.optJSONArray(array);
        if (old == null || now == null) return 0;
        Map<Integer, JSONObject> prior = index(old);
        int changed = 0;
        for (int i = 0; i < now.length(); i++) {
            JSONObject row = now.optJSONObject(i);
            if (row == null) continue;
            JSONObject was = prior.get(row.optInt("number"));
            if ((was == null && full) || (was != null && differs(was, row, pulls))) changed++;
        }
        return changed;
    }

    private static void diffRecords(JSONArray out, JSONArray old, JSONArray now, boolean pulls, String repo, String kind, String origin, boolean complete) throws Exception {
        if (old == null || now == null) return;
        Map<Integer, JSONObject> prior = index(old);
        for (int i = 0; i < now.length(); i++) {
            JSONObject row = now.optJSONObject(i);
            if (row == null) continue;
            int number = row.optInt("number");
            JSONObject was = prior.get(number);
            if (was == null) {
                if (!complete) continue; // Unknown whether the record existed outside an earlier partial list.
                out.put(event(kind + "_new", kind + "#" + number,
                        ("issue".equals(kind) ? "Появилась задача #" : "Появилось предложение #") + number + ".",
                        "В прошлом полном списке этого номера не было. Сейчас заголовок: «" + clip(row.optString("title")) + "», состояние " + row.optString("state") + ".",
                        url(repo, kind, number), origin));
                continue;
            }
            String from = was.optString("state");
            String to = row.optString("state");
            if (!from.equals(to)) {
                String verb = "open".equals(to) && "closed".equals(from) ? "снова открыта"
                        : "closed".equals(to) ? "закрыта" : "в состоянии «" + to + "»";
                out.put(event(kind + "_state", kind + "#" + number,
                        ("issue".equals(kind) ? "У задачи #" : "У предложения #") + number + " изменился статус: " + verb + ".",
                        "Поле state: " + from + " → " + to + ". Закрытие не означает, что работу проверили.",
                        url(repo, kind, number), origin));
            }
            if (pulls && merged(row) && !merged(was)) {
                String when = row.optString("merged_at");
                out.put(event("pull_merged", "pull#" + number,
                        "Предложение #" + number + " слито по отметке GitHub.",
                        "merged или merged_at изменились" + (when.isEmpty() || "null".equals(when) ? "." : " (" + when + ").") + " state при этом мог остаться closed.",
                        url(repo, kind, number), origin));
            } else if (pulls && merged(was) && !merged(row)) {
                out.put(event("pull_unmerged", "pull#" + number,
                        "У предложения #" + number + " снята отметка слияния.",
                        "Поле merged или merged_at больше не подтверждает слияние. Это не оценка кода.",
                        url(repo, kind, number), origin));
            }
            if (!was.optString("title").equals(row.optString("title"))) {
                out.put(event(kind + "_title", kind + "#" + number,
                        ("issue".equals(kind) ? "Изменено название задачи #" : "Изменено название предложения #") + number + ".",
                        "Было: «" + clip(was.optString("title")) + "». Стало: «" + clip(row.optString("title")) + "». Это не завершение работы.",
                        url(repo, kind, number), origin));
            } else if (touched(was, row) && from.equals(to) && merged(was) == merged(row)) {
                out.put(event(kind + "_touch", kind + "#" + number,
                        ("issue".equals(kind) ? "Запись задачи #" : "Запись предложения #") + number + " обновлена.",
                        "Поле updated_at изменилось. Текст описания в снимок не входит, поэтому содержание правки неизвестно.",
                        url(repo, kind, number), origin));
            }
        }
        if (!complete) return; // Absence from a partial list cannot imply removal.
        for (Integer number : prior.keySet()) {
            boolean found = false;
            for (int i = 0; i < now.length(); i++) {
                JSONObject row = now.optJSONObject(i);
                if (row != null && row.optInt("number") == number) { found = true; break; }
            }
            if (found || number == 0) continue;
            out.put(event(kind + "_absent", kind + "#" + number,
                    ("issue".equals(kind) ? "Задачи #" : "Предложения #") + number + " нет в новом полном списке.",
                    "В прошлой полной выборке номер был, в новой полной выборке его нет. Это не доказательство удаления: GitHub мог не вернуть запись.",
                    url(repo, kind, number), origin));
        }
    }

    private static void diffBranches(JSONArray out, JSONArray old, JSONArray now, String repo, String origin) throws Exception {
        if (old == null || now == null) return;
        Map<String, String> prior = new HashMap<>();
        for (int i = 0; i < old.length(); i++) {
            JSONObject b = old.optJSONObject(i);
            if (b != null && !b.optString("name").isEmpty()) prior.put(b.optString("name"), b.optString("sha"));
        }
        for (int i = 0; i < now.length(); i++) {
            JSONObject b = now.optJSONObject(i);
            if (b == null || b.optString("name").isEmpty()) continue;
            String name = b.optString("name");
            String sha = b.optString("sha");
            String link = branchUrl(repo, name);
            if (!prior.containsKey(name)) {
                out.put(event("branch_seen", "branch:" + name,
                        "Направление «" + name + "» впервые обнаружено между снимками.",
                        "В прошлой полной проверке этого имени не было. Это не гарантия, что ветку только что создали.",
                        link, origin));
            } else if (!prior.get(name).equals(sha)) {
                out.put(event("branch_moved", "branch:" + name,
                        "Указатель направления «" + name + "» изменился.",
                        "Сдвиг указателя сам по себе не прогресс. Сравнение вперёд, назад или врозь запрашивается при открытии направления.",
                        link, origin));
            }
        }
    }

    private static boolean differs(JSONObject was, JSONObject now, boolean pulls) {
        if (!was.optString("state").equals(now.optString("state"))) return true;
        if (!was.optString("title").equals(now.optString("title"))) return true;
        if (pulls && merged(now) != merged(was)) return true;
        return touched(was, now);
    }

    private static boolean merged(JSONObject row) {
        if (row.optBoolean("merged", false)) return true;
        String at = row.optString("merged_at");
        return !at.isEmpty() && !"null".equals(at);
    }

    private static boolean touched(JSONObject was, JSONObject now) {
        String a = was.optString("updated_at");
        String b = now.optString("updated_at");
        return !a.isEmpty() && !b.isEmpty() && !"null".equals(a) && !"null".equals(b) && !a.equals(b);
    }

    private static Map<Integer, JSONObject> index(JSONArray list) {
        Map<Integer, JSONObject> map = new HashMap<>();
        if (list == null) return map;
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row != null) map.put(row.optInt("number"), row);
        }
        return map;
    }

    private static JSONObject event(String kind, String id, String text, String basis, String url, String origin) throws Exception {
        JSONObject e = new JSONObject();
        e.put("kind", kind);
        e.put("id", id);
        e.put("event", text);
        e.put("basis", basis);
        e.put("unknown", "Не подтверждено: тесты, работоспособность функции и готовность на Android.");
        if (url != null && url.startsWith("https://github.com/")) e.put("url", url);
        e.put("origin", origin == null || origin.isEmpty() ? "LIVE" : origin);
        return e;
    }

    private static String originOf(JSONObject current) {
        String origin = current.optString("origin");
        if ("LIVE".equals(origin) || "CACHED".equals(origin) || "PARTIAL".equals(origin) || "IMPORTED".equals(origin))
            return origin;
        return "LIVE";
    }

    private static String branchUrl(String repo, String name) {
        if (repo == null || name == null) return null;
        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) return null;
        if (name.isEmpty() || name.contains("..") || name.indexOf(' ') >= 0 || name.indexOf('\\') >= 0
                || name.indexOf('?') >= 0 || name.indexOf('#') >= 0) return null;
        return "https://github.com/" + repo + "/tree/" + name;
    }

    private static String url(String repo, String kind, int number) {
        if (repo == null || !repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) return null;
        return "https://github.com/" + repo + ("pull".equals(kind) ? "/pull/" : "/issues/") + number;
    }

    private static String clip(String value) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= 80 ? clean : clean.substring(0, 77) + "…";
    }
}
