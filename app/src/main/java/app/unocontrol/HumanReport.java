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
        if (s.advanced + s.added + s.missing > 0) return "В истории проекта есть изменения";
        if (s.issueChanges + s.pullChanges > 0) return "Изменились записи задач или предложений";
        if (!current.optBoolean("issuesComplete", false) || !current.optBoolean("pullsComplete", false))
            return "Часть данных GitHub недоступна";
        if (!current.optBoolean("branchesComplete", false) || !previous.optBoolean("branchesComplete", false))
            return "Не все данные удалось сравнить";
        if (!listsKnown(previous)) return "Статусы задач сравнить нельзя";
        return "Новых сдвигов веток не обнаружено";
    }
    static String homeDescription(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Укажите репозиторий GitHub и нажмите «Обновить».";
        if (current.optBoolean("restoredFromBackup", false)) return "Это сохранённая копия. Для проверки текущего состояния обновите данные с GitHub.";
        if (previous == null || previous.optBoolean("restoredFromBackup", false))
            return "Данные получены. Сейчас сравнивать их не с чем: понадобится ещё одна проверка позже."
                    + ((!current.optBoolean("issuesComplete", false) || !current.optBoolean("pullsComplete", false))
                    ? " Сведения о задачах или предложениях загрузились не полностью." : "");
        if (s.advanced + s.added + s.missing > 0)
            return "По сравнению с прошлой проверкой изменились указатели некоторых веток. Что именно сделано и работает ли это, нужно проверять отдельно."
                    + (listsKnown(current) && listsKnown(previous) ? "" : " Записи вне пересечения списков не оцениваются.");
        if (s.issueChanges + s.pullChanges > 0)
            return "В доступной выборке изменились записи задач или предложений. Это не доказывает готовность функций."
                    + ((!current.optBoolean("issuesComplete", false) || !current.optBoolean("pullsComplete", false)
                    || !previous.optBoolean("issuesComplete", false) || !previous.optBoolean("pullsComplete", false))
                    ? " Остальные записи вне пересечения списков не оцениваются." : "");
        if (!current.optBoolean("issuesComplete", false) || !current.optBoolean("pullsComplete", false))
            return "Ветки можно сравнить отдельно, но список задач или предложений загружен не полностью. Нельзя делать вывод о полном состоянии проекта.";
        if (!current.optBoolean("branchesComplete", false) || !previous.optBoolean("branchesComplete", false))
            return "Часть сведений могла не загрузиться. Поэтому точный вывод об изменениях пока невозможен.";
        if (!listsKnown(previous))
            return "Указатели веток те же. Статусы задач сравнить нельзя: в прошлой проверке нет признака полноты списка.";
        if (s.advanced + s.added + s.missing == 0 && s.issueChanges + s.pullChanges > 0)
            return "Ветки остались прежними, но в GitHub изменились записи задач или предложений. Это не доказывает готовность функций.";
        if (s.advanced + s.added + s.missing == 0)
            return "С прошлой проверки указатели веток остались прежними. Это не означает, что работа над проектом закончена.";
        return "По сравнению с прошлой проверкой изменились указатели некоторых веток. Что именно сделано и работает ли это, нужно проверять отдельно.";
    }
    static boolean listsKnown(JSONObject snap) {
        return snap != null && snap.has("issuesComplete") && snap.optBoolean("issuesComplete", false)
                && snap.has("pullsComplete") && snap.optBoolean("pullsComplete", false);
    }

    /** Human home line. Must not say the project is idle when there is no baseline. */
    static String whatChanged(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Проект ещё не прочитан.";
        if (current.optBoolean("restoredFromBackup", false))
            return "Показан сохранённый файл, а не свежая проверка.";
        if (previous == null || previous.optBoolean("restoredFromBackup", false)
                || !current.optString("repo").equalsIgnoreCase(previous.optString("repo")))
            return "Это первая проверка. Базы для сравнения нет, поэтому нельзя сказать, что проект стоит на месте или что в нём нет нового.";
        boolean branchesKnown = current.optBoolean("branchesComplete", false) && previous.optBoolean("branchesComplete", false);
        if (!branchesKnown && s.advanced == 0 && s.issueChanges + s.pullChanges == 0)
            return "Список направлений неполный. Считать его полной картиной изменений нельзя.";
        if (s.advanced + s.added + s.missing > 0) {
            if (researchMoved(current, previous))
                return "Указатель исследовательского направления изменился. Это могло быть продвижение, откат или переписывание истории. Работоспособность не проверена.";
            return "Указатели направлений разработки изменились. Это могло быть продвижение, откат или переписывание истории. Работоспособность не проверена.";
        }
        if (s.issueChanges + s.pullChanges > 0) {
            String lead = ChangeSet.homeLine(previous, current);
            if (lead != null && !lead.toLowerCase(java.util.Locale.ROOT).contains("sha")) return lead;
            return "Указатели направлений те же, но изменились записи задач или предложений. Это не доказательство готовности.";
        }
        String quiet = ChangeSet.homeLine(previous, current);
        if (quiet != null && !quiet.toLowerCase(java.util.Locale.ROOT).contains("sha")) return quiet;
        if (!branchesKnown)
            return "Полного сравнения направлений нет. Нельзя утверждать, что ничего не менялось.";
        return "С прошлой полной проверки указатели направлений не сдвинулись. Это не значит, что работа закончена.";
    }

    static String needsAttention(JSONObject current, Summary s) {
        if (current == null) return "Нет сведений, чтобы назвать конкретную проблему.";
        String issues = openIssueLine(current);
        if (issues != null) return issues;
        if (!current.optBoolean("issuesComplete", false))
            return "Сведения о задачах не получены. Нельзя сказать, что открытых задач нет.";
        return "Не удалось получить сведения о проверках. Их отсутствие не означает, что всё в порядке.";
    }

    static String openIssueLine(JSONObject current) {
        if (current == null) return null;
        JSONArray issues = current.optJSONArray("issues");
        if (issues == null) return null;
        String first = null, second = null;
        for (int i = 0; i < issues.length(); i++) {
            JSONObject x = issues.optJSONObject(i);
            if (x == null || !"open".equals(x.optString("state"))) continue;
            String title = x.optString("title").trim();
            if (title.isEmpty()) title = "без названия";
            if (title.length() > 80) title = title.substring(0, 77) + "…";
            if (first == null) first = title;
            else { second = title; break; }
        }
        if (first == null) return null;
        StringBuilder b = new StringBuilder();
        b.append("Открытая задача: «").append(first).append("».");
        if (second != null) b.append(" Ещё открыто: «").append(second).append("».");
        if (!current.optBoolean("issuesComplete", false))
            b.append(" Список задач неполный, это не все проблемы.");
        else b.append(" Это не полный список дефектов программы.");
        return b.toString();
    }

    static String nextStep(JSONObject current, JSONObject previous, Summary s) {
        return nextStep(current, previous, s, null);
    }

    static String nextStep(JSONObject current, JSONObject previous, Summary s, String lastError) {
        if (lastError != null && !lastError.isEmpty()) {
            String low = lastError.toLowerCase(Locale.ROOT);
            if (low.contains("401") || low.contains("истёк") || low.contains("истек")
                    || low.contains("404") || low.contains("нет доступа") || low.contains("ограничен"))
                return "Проверить доступ";
            if (low.contains("не ответил") || low.contains("лимит") || low.contains("соединения")
                    || low.contains("отменён"))
                return "Повторить загрузку";
        }
        if (current == null || current.optBoolean("restoredFromBackup", false)) return "Повторить загрузку";
        if (previous == null) return "Открыть направления";
        if (s.advanced + s.added + s.missing > 0 || s.issueChanges + s.pullChanges > 0) return "Открыть изменения";
        if (current.optBoolean("issuesComplete", false) && countState(current.optJSONArray("issues"), "open") > 0)
            return "Посмотреть задачу";
        if (!current.optBoolean("issuesComplete", false) && countState(current.optJSONArray("issues"), "open") > 0)
            return "Посмотреть задачу";
        if (!current.optBoolean("branchesComplete", false) || !current.optBoolean("issuesComplete", false))
            return "Повторить загрузку";
        return "Открыть направления";
    }

    static boolean researchMoved(JSONObject current, JSONObject previous) {
        if (current == null || previous == null) return false;
        java.util.Map<String,String> old = heads(previous.optJSONArray("branches"));
        org.json.JSONArray now = current.optJSONArray("branches");
        if (now == null) return false;
        for (int i = 0; i < now.length(); i++) {
            org.json.JSONObject b = now.optJSONObject(i);
            if (b == null) continue;
            String name = b.optString("name");
            if (!name.startsWith("research/")) continue;
            String prior = old.get(name);
            if (prior != null && !prior.equals(b.optString("sha"))) return true;
        }
        return false;
    }

    /** Title-only wording. Must not name files that were not loaded. */
    static String explainClaim(String message, org.json.JSONArray files) {
        String raw = message == null ? "" : message.trim();
        String low = raw.toLowerCase(java.util.Locale.ROOT);
        boolean filesKnown = files != null;
        String fileLine = filesKnown ? fileHint(files) : HumanReport.NOT_ENOUGH + ": список файлов не получен.";
        String skip = low.contains("[skip ci]") ? " Пометка [skip ci] в тексте — не успешная проверка." : "";
        if (low.startsWith("docs")) {
            if (!filesKnown) return "Автор написал, что менялись документы. " + fileLine + skip;
            return "Автор написал, что менялись документы. " + fileLine + " Это не доказательство готовности программы." + skip;
        }
        if (low.startsWith("fix"))
            return "Автор сообщил об исправлении. Это заявление, не проверка." + (filesKnown ? " " + fileLine : " " + fileLine) + skip;
        return commitSummary(raw) + ". " + fileLine + skip + " " + NO_CHECK + ".";
    }

    private static String fileHint(org.json.JSONArray files) {
        if (files.length() == 0) return "GitHub не вернул изменённые файлы.";
        StringBuilder names = new StringBuilder("GitHub показывает файлы: ");
        int n = Math.min(files.length(), 4);
        for (int i = 0; i < n; i++) {
            org.json.JSONObject row = files.optJSONObject(i);
            if (i > 0) names.append(", ");
            names.append(row == null ? "без имени" : row.optString("filename"));
        }
        return names.toString() + ".";
    }

    private static java.util.Map<String,String> heads(org.json.JSONArray branches) {
        java.util.Map<String,String> map = new java.util.HashMap<>();
        if (branches == null) return map;
        for (int i = 0; i < branches.length(); i++) {
            org.json.JSONObject b = branches.optJSONObject(i);
            if (b != null) map.put(b.optString("name"), b.optString("sha"));
        }
        return map;
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

    static String syncStatus(JSONObject current) {
        if (current == null) return "Синхронизации ещё не было.";
        String when = humanDate(current.optLong("capturedAt", 0));
        if (current.optBoolean("restoredFromBackup", false))
            return "Это импорт, не свежая проверка GitHub. Дата файла: " + when + ".";
        boolean partial = !current.optBoolean("branchesComplete", false)
                || !current.optBoolean("issuesComplete", false)
                || !current.optBoolean("pullsComplete", false)
                || current.optBoolean("branchesStale", false)
                || current.optBoolean("issuesStale", false)
                || current.optBoolean("pullsStale", false)
                || current.optBoolean("commitsStale", false);
        if (partial) return "Проверка частичная, не полная. Ответ GitHub: " + when + ".";
        if (current.optBoolean("branchesCached", false) || current.optBoolean("issuesCached", false)
                || current.optBoolean("pullsCached", false))
            return "GitHub подтвердил, что часть списков не изменилась. Ответ: " + when + ". История изучена лишь частично, это не полная приёмка.";
        return "Ответ GitHub: " + when + ". История изучена лишь частично, это не полная приёмка.";
    }

    static String coverageLine(JSONObject current) {
        if (current == null) return "";
        String branches = current.optBoolean("branchesComplete", false)
                ? "Указатели веток проверены полностью."
                : "Указатели веток проверены не полностью.";
        return branches + " История изучена лишь частично.";
    }

    static String branchAssumption(String name) {
        if (name == null || name.isEmpty()) return "Название направления не получено.";
        if ("main".equals(name) || "master".equals(name))
            return "Это имя основной ветки в GitHub. Оно не доказывает готовность проекта.";
        return "Предположение по названию: " + branchKind(name) + ". Это не установленный факт.";
    }

    static String pullListLabel(JSONObject pull) {
        if (pull == null) return "Состояние предложения неизвестно.";
        if (pull.optBoolean("merged", false))
            return "Внесено в проект по отметке GitHub. Это не проверка на устройстве.";
        if ("closed".equals(pull.optString("state")))
            return "Закрыто, но GitHub не отмечает внесение в основную ветку.";
        if ("open".equals(pull.optString("state")))
            return "Предложение ещё открыто. Результат не подтверждён.";
        return "Состояние предложения неизвестно.";
    }

    static String humanDate(String iso) {
        if (iso == null || iso.isEmpty() || "null".equals(iso)) return "Дата неизвестна";
        try {
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(iso);
            return humanDate(odt.toInstant().toEpochMilli());
        } catch (Exception ignored) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(iso);
                return humanDate(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            } catch (Exception again) {
                return iso;
            }
        }
    }

    static String humanDate(long millis) {
        if (millis <= 0) return "Дата неизвестна";
        return java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(new java.util.Date(millis));
    }
}
