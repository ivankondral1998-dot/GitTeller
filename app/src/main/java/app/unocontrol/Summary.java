package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Conservative plain-language summary from GitHub metadata. No AI, no unverified claims. */
final class Summary {
    final String heading;
    final String description;
    final String attention;
    final int advanced, added, missing;

    private Summary(String heading, String description, String attention, int advanced, int added, int missing) {
        this.heading = heading;
        this.description = description;
        this.attention = attention;
        this.advanced = advanced;
        this.added = added;
        this.missing = missing;
    }

    static Summary of(JSONObject current, JSONObject previous) {
        if (current == null) {
            return new Summary(
                "Нет данных",
                "Подключите репозиторий и нажмите «Обновить». После ответа GitHub здесь появится краткое объяснение изменений.",
                "Без свежей проверки нельзя утверждать, что в проекте что-то изменилось.",
                0, 0, 0);
        }

        if (previous == null || !current.optString("repo").equalsIgnoreCase(previous.optString("repo"))) {
            int branches = current.optJSONArray("branches") == null ? 0 : current.optJSONArray("branches").length();
            return new Summary(
                "Первый снимок сохранён",
                "Получены данные о " + branches + " ветках. Следующее обновление позволит сравнить, что изменилось.",
                "Коммиты и новые ветки фиксируют изменения истории, но не доказывают, что код работает или тесты прошли.",
                0, 0, 0);
        }

        // Branch analysis (existing logic, kept)
        Map<String, String> old = mapBranches(previous.optJSONArray("branches"));
        Map<String, String> now = mapBranches(current.optJSONArray("branches"));
        int moved = 0, added = 0, removed = 0;
        for (Map.Entry<String, String> e : now.entrySet()) {
            if (!old.containsKey(e.getKey())) added++;
            else if (!old.get(e.getKey()).equals(e.getValue())) moved++;
        }
        boolean complete = current.optBoolean("branchesComplete", false) && previous.optBoolean("branchesComplete", false);
        if (complete) {
            for (String name : old.keySet()) if (!now.containsKey(name)) removed++;
        }

        // Plain-language analysis
        List<String> changes = new ArrayList<>();
        if (moved > 0) changes.add(moved + " веток обновили указатель (появились новые коммиты)");
        if (added > 0) changes.add("появилось " + added + " новых веток");
        if (complete && removed > 0) changes.add(removed + " веток больше не обнаруживаются");

        // Commits (conservative: only count and note that messages are not proof)
        JSONArray commits = current.optJSONArray("commits");
        int commitCount = commits == null ? 0 : commits.length();
        if (commitCount > 0) {
            changes.add("загружено " + commitCount + " последних сообщений разработчиков");
        }

        // Issues / PRs (very conservative)
        JSONArray issues = current.optJSONArray("issues");
        JSONArray pulls = current.optJSONArray("pulls");
        int openIssues = countOpen(issues);
        int openPrs = countOpen(pulls);
        if (openIssues > 0) changes.add("открытых задач: " + openIssues);
        if (openPrs > 0) changes.add("открытых запросов изменений: " + openPrs);

        String heading;
        String description;
        if (changes.isEmpty()) {
            heading = "Новых изменений не обнаружено";
            description = "По доступным данным ветки и снимки не изменились.";
        } else {
            heading = "В проекте есть изменения";
            description = String.join(". ", changes) + ".";
            if (description.length() > 220) description = description.substring(0, 217) + "…";
        }

        String attention = "Изменения в истории Git не подтверждают, что код работает, тесты прошли или функция готова. ";
        if (!complete) attention += "Список веток мог загрузиться не полностью — исчезновение веток не оценивается. ";
        JSONArray warnings = current.optJSONArray("warnings");
        if (warnings != null && warnings.length() > 0) {
            attention = "Часть данных не удалось загрузить. Выводы ограничены. " + attention;
        }
        if (attention.length() > 180) attention = attention.substring(0, 177) + "…";

        return new Summary(heading, description, attention, moved, added, removed);
    }

    private static int countOpen(JSONArray arr) {
        if (arr == null) return 0;
        int n = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && "open".equalsIgnoreCase(o.optString("state"))) n++;
        }
        return n;
    }

    private static Map<String, String> mapBranches(JSONArray branches) {
        Map<String, String> m = new HashMap<>();
        if (branches == null) return m;
        for (int i = 0; i < branches.length(); i++) {
            JSONObject b = branches.optJSONObject(i);
            if (b != null && !b.optString("name").isEmpty())
                m.put(b.optString("name"), b.optString("sha"));
        }
        return m;
    }
}
