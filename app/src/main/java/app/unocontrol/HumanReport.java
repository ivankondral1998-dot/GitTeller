package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Conservative Russian UI labels. Source metadata is not proof of successful tests. */
final class HumanReport {
    private HumanReport() {}
    static String homeTitle(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Подключите проект";
        if (current.optBoolean("restoredFromBackup", false)) return "Сведения из сохранённого файла";
        if (previous == null || previous.optBoolean("restoredFromBackup", false)) return "Первая проверка выполнена";
        if (!current.optBoolean("branchesComplete", false) || !previous.optBoolean("branchesComplete", false))
            return "Не все данные удалось сравнить";
        if (s.advanced + s.added + s.missing > 0) return "В истории проекта есть изменения";
        return "Новых сдвигов веток не обнаружено";
    }
    static String homeDescription(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Укажите репозиторий GitHub и нажмите «Обновить».";
        if (current.optBoolean("restoredFromBackup", false)) return "Это сохранённая копия. Для проверки текущего состояния обновите данные с GitHub.";
        if (previous == null || previous.optBoolean("restoredFromBackup", false))
            return "Данные получены. Сейчас сравнивать их не с чем: понадобится ещё одна проверка позже.";
        if (!current.optBoolean("branchesComplete", false) || !previous.optBoolean("branchesComplete", false))
            return "Часть сведений могла не загрузиться. Поэтому точный вывод об изменениях пока невозможен.";
        if (s.advanced + s.added + s.missing == 0)
            return "С прошлой проверки указатели веток остались прежними. Это не означает, что работа над проектом закончена.";
        return "По сравнению с прошлой проверкой изменились указатели некоторых веток. Что именно сделано и работает ли это, нужно проверять отдельно.";
    }
    static String branchKind(String name) {
        if (name == null) return "Направление работы";
        if (name.equals("main") || name.equals("master")) return "Основная ветка проекта";
        if (name.startsWith("docs/")) return "Документация и инструкции";
        if (name.startsWith("audit/")) return "Проверки и аудит";
        if (name.startsWith("research/")) return "Исследования";
        if (name.startsWith("design/")) return "Проектирование";
        if (name.startsWith("feat/") || name.startsWith("feature/")) return "Разработка функций";
        if (name.startsWith("fix/") || name.startsWith("bugfix/")) return "Исправления";
        if (name.startsWith("integration/")) return "Объединение компонентов";
        if (name.startsWith("migration/")) return "Перенос данных или компонентов";
        return "Отдельное направление работы";
    }
    static final String NOT_ENOUGH = "Недостаточно данных";
    static final String NO_CHECK = "Проверка работоспособности отсутствует";

    static String branchTopic(String name) {
        if (name == null) return "Тема по названию не определена";
        String low = name.toLowerCase(Locale.ROOT);
        if (low.equals("main") || low.equals("master")) return "Основная версия проекта";
        return "Тема по названию не определена";
    }
    static String commitSummary(String raw) {
        String t = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (t.matches("^(docs|doc)(\\([^)]*\\))?!?:.*")) return "Обновлены документы или инструкции проекта";
        if (t.matches("^(fix|hotfix|bugfix)(\\([^)]*\\))?!?:.*")) return "Разработчик сообщил об исправлении";
        if (t.matches("^(feat|feature)(\\([^)]*\\))?!?:.*")) return "Разработчик сообщил о новой функции";
        if (t.matches("^(test|tests)(\\([^)]*\\))?!?:.*")) return "Изменены тесты или проверки";
        if (t.matches("^(refactor)(\\([^)]*\\))?!?:.*")) return "Изменена внутренняя организация кода";
        if (t.startsWith("merge")) return "Объединена история разработки";
        return "В истории проекта появилась запись";
    }
    static String state(String s) {
        if ("open".equals(s)) return "Открыто: работа или обсуждение продолжается";
        if ("closed".equals(s)) return "Закрыто в GitHub: результат не проверен";
        return "Состояние неизвестно";
    }
    static int countState(JSONArray a, String wanted) {
        if (a == null) return 0;
        int n=0; for(int i=0;i<a.length();i++) {
            JSONObject x=a.optJSONObject(i); if(x!=null && wanted.equals(x.optString("state"))) n++;
        } return n;
    }
    static String commitScope(JSONObject snapshot) {
        if (snapshot == null) return "основной ветки";
        String defaultBranch=snapshot.optString("defaultBranch");
        if(defaultBranch.isEmpty())defaultBranch="main";
        return "ветки «"+defaultBranch+"»";
    }
}
