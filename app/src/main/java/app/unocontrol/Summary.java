package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** No AI inference: only exact and conservative facts about GitHub snapshots. */
final class Summary {
    final String heading;
    final String description;
    final String attention;
    final int advanced, added, missing;

    private Summary(String heading, String description, String attention, int advanced, int added, int missing) {
        this.heading=heading;this.description=description;this.attention=attention;
        this.advanced=advanced;this.added=added;this.missing=missing;
    }

    static Summary of(JSONObject current, JSONObject previous) {
        if (current == null) return new Summary("Пока нет свежей проверки",
                "Подключите репозиторий и нажмите «Обновить». Данные появятся после ответа GitHub.",
                "Без синхронизации нельзя утверждать, что проект изменился или завершён.",0,0,0);
        if (previous == null || !current.optString("repo").equalsIgnoreCase(previous.optString("repo")))
            return new Summary("Первый снимок сохранён",
                    "Получены сведения о " + (current.optJSONArray("branches") == null ? 0 : current.optJSONArray("branches").length()) + " ветках. Сравнение изменений будет после следующего обновления.",
                    "Коммиты и новые ветки подтверждают изменения истории, а не успешность тестов.",0,0,0);

        Map<String,String> old = map(previous.optJSONArray("branches"));
        Map<String,String> now = map(current.optJSONArray("branches"));
        int moved=0, added=0, removed=0;
        for (Map.Entry<String,String> e:now.entrySet()) {
            if (!old.containsKey(e.getKey())) added++;
            else if (!old.get(e.getKey()).equals(e.getValue())) moved++;
        }
        boolean complete = current.optBoolean("branchesComplete",false) &&
                previous.optBoolean("branchesComplete",false);
        if (complete) for (String name:old.keySet()) if (!now.containsKey(name)) removed++;
        String headline = (moved+added+removed)==0 ?
                (complete ? "SHA веток не изменились" : "Сравнение неполное") : "В истории проекта есть изменения";
        String desc = moved + " веток изменили указатель, " + added + " появилось впервые.";
        if (complete) desc += " Ещё " + removed + " веток не обнаружены в новом списке.";
        else desc += " Список мог загрузиться не полностью, исчезновение веток не оценивается.";
        String attention = "Изменения веток не показывают, прошли ли тесты и готово ли приложение.";
        if (current.optJSONArray("warnings") != null && current.optJSONArray("warnings").length() > 0)
            attention = "Часть данных не удалось загрузить. Выводы ограничены. " + attention;
        return new Summary(headline, desc, attention, moved, added, removed);
    }

    private static Map<String,String> map(JSONArray branches) {
        Map<String,String> m=new HashMap<>();
        if(branches==null)return m;
        for(int i=0;i<branches.length();i++){
            JSONObject b=branches.optJSONObject(i);
            if(b!=null && !b.optString("name").isEmpty())m.put(b.optString("name"),b.optString("sha"));
        }
        return m;
    }
}
