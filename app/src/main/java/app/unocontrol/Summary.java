package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** No AI inference: only exact and conservative facts about GitHub snapshots. */
final class Summary {
    final String heading;
    final String plain;
    final String description;
    final String attention;
    final int advanced, added, missing, issueChanges, pullChanges;

    private Summary(String heading, String plain, String description, String attention,
                    int advanced, int added, int missing, int issueChanges, int pullChanges) {
        this.heading=heading;this.plain=plain;this.description=description;this.attention=attention;
        this.advanced=advanced;this.added=added;this.missing=missing;
        this.issueChanges=issueChanges;this.pullChanges=pullChanges;
    }

    static Summary of(JSONObject current, JSONObject previous) {
        if (current == null) return new Summary("Пока нет свежей проверки",
                "Проверки ещё не было. Нажмите «Обновить», чтобы узнать, что происходит в проекте.",
                "Подключите репозиторий и нажмите «Обновить». Данные появятся после ответа GitHub.",
                "Без синхронизации нельзя утверждать, что проект изменился или завершён.",0,0,0,0,0);
        // A user-supplied backup is not authenticated GitHub evidence.
        // Never compare a newly fetched live snapshot against a forged/imported baseline.
        if (current.optBoolean("restoredFromBackup", false))
            return new Summary("Данные из резервной копии",
                    "Показан сохранённый файл, а не текущая проверка GitHub. Нажмите «Обновить» для получения свежих данных.",
                    "По этому файлу нельзя установить, какие изменения действительно произошли в GitHub.",
                    "Источник не проверен: импортированные сведения нельзя считать подтверждением готовности или прогресса.",0,0,0,0,0);
        if (previous == null || previous.optBoolean("restoredFromBackup", false) ||
                !current.optString("repo").equalsIgnoreCase(previous.optString("repo")))
            return new Summary("Первый снимок сохранён",
                    "Это первая достоверная проверка для сравнения. Обновите данные позже, чтобы увидеть изменения.",
                    "Получены сведения о " + (current.optJSONArray("branches") == null ? 0 : current.optJSONArray("branches").length()) +
                            " ветках." + (current.optBoolean("branchesComplete", false) ? "" : " Список веток неполный.") +
                            " Сравнение изменений будет после следующего обновления.",
                    "Коммиты и новые ветки не доказывают успешность тестов." +
                            (current.optJSONArray("warnings") != null && current.optJSONArray("warnings").length() > 0 ?
                                    " Часть данных не загрузилась: выводы ограничены." : ""),0,0,0,0,0);

        Map<String,String> old = map(previous.optJSONArray("branches"));
        Map<String,String> now = map(current.optJSONArray("branches"));
        boolean previousComplete = previous.optBoolean("branchesComplete", false);
        boolean currentComplete = current.optBoolean("branchesComplete", false);
        int moved=0, added=0, removed=0, previouslyUnseen=0;
        for (Map.Entry<String,String> e:now.entrySet()) {
            if (!old.containsKey(e.getKey())) {
                // Incomplete prior snapshot: absent does NOT mean newly created branch.
                if (previousComplete) added++;
                else previouslyUnseen++;
            } else if (!old.get(e.getKey()).equals(e.getValue())) moved++;
        }
        boolean complete = currentComplete && previousComplete;
        if (complete) for (String name:old.keySet()) if (!now.containsKey(name)) removed++;
        // Compare states only when BOTH complete result sets were fetched. Otherwise
        // an absent record is unknown, not a closed or deleted task.
        boolean issueScopeComplete = previous.optBoolean("issuesComplete", false)
                && current.optBoolean("issuesComplete", false);
        boolean pullScopeComplete = previous.optBoolean("pullsComplete", false)
                && current.optBoolean("pullsComplete", false);
        int issueChanges = ChangeSet.issueChanges(previous, current);
        int pullChanges = ChangeSet.pullChanges(previous, current);
        String headline = (moved+added+removed)==0 ?
                (complete ? "SHA веток не изменились" : "Сравнение неполное") : "В истории проекта есть изменения";
        String desc = moved + " веток изменили указатель, " + added + " появились в сравнении с полностью загруженным списком.";
        if (previouslyUnseen>0) desc += " Ещё " + previouslyUnseen +
                " веток впервые встретились в нашей выборке, но могли существовать раньше.";
        if (complete) desc += " Ещё " + removed + " веток не обнаружены в новом списке.";
        else desc += " Список мог загрузиться не полностью, исчезновение веток не оценивается.";
        if (issueChanges > 0) desc += " Статус изменился у " + issueChanges + " задач в доступной выборке.";
        if (pullChanges > 0) desc += " Статус изменился у " + pullChanges + " запросов изменений.";
        if (!issueScopeComplete || !pullScopeComplete)
            desc += " Сравнение статусов задач или предложений ограничено неполными данными.";
        String plain;
        if ((moved+added+removed)==0 && complete) {
            plain = "По двум последним проверкам указатели веток не сдвинулись. Это не значит, что продукт готов или что задачи закрыты.";
        } else if ((moved+added+removed)==0) {
            plain = "Сравнение неполное: часть списков не догрузилась. Нельзя утверждать, что ветки исчезли или что ничего не менялось.";
        } else {
            plain = "В истории проекта появились новые записи. Это видно по веткам, но не доказывает, что программа работает или тесты прошли.";
        }
        if (issueChanges + pullChanges > 0) {
            plain += " В доступной выборке изменился статус задач или запросов — это заявление GitHub, не проверка на устройстве.";
        }
        String attention = "Изменения веток не показывают, прошли ли тесты и готово ли приложение.";
        if (issueChanges + pullChanges > 0 && moved + added + removed == 0) {
            attention = "Указатели веток те же, но статусы задач или запросов в выборке изменились. Это не доказательство, что программа работает. " + attention;
        }
        if (!issueScopeComplete || !pullScopeComplete)
            attention = "Статусы задач или предложений не удалось сравнить полностью. " + attention;
        if (current.optJSONArray("warnings") != null && current.optJSONArray("warnings").length() > 0)
            attention = "Часть данных не удалось загрузить. Выводы ограничены. " + attention;
        return new Summary(headline, plain, desc, attention, moved, added, removed, issueChanges, pullChanges);
    }

    static int stateChanges(JSONArray old, JSONArray now) {
        if (old == null || now == null) return 0;
        Map<Integer,String> states = new HashMap<>();
        for (int i = 0; i < old.length(); i++) {
            JSONObject v = old.optJSONObject(i);
            if (v != null) states.put(v.optInt("number"), v.optString("state"));
        }
        int changed = 0;
        for (int i = 0; i < now.length(); i++) {
            JSONObject v = now.optJSONObject(i);
            if (v == null) continue;
            String prior = states.get(v.optInt("number"));
            if (prior != null && !prior.equals(v.optString("state"))) changed++;
        }
        return changed;
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
