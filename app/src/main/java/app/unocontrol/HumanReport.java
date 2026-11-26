package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Conservative Russian UI labels. Source metadata is not proof of successful tests. */
final class HumanReport {
    private HumanReport() {}
    static String homeTitle(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Подключите проект";
        if (SnapshotCodec.isImported(current)) return "Сведения из сохранённого файла";
        if (previous == null || SnapshotCodec.isImported(previous)) return "Первая проверка выполнена";
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
        if (SnapshotCodec.isImported(current)) return "Это сохранённая копия. Для проверки текущего состояния обновите данные с GitHub.";
        if (previous == null || SnapshotCodec.isImported(previous))
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
        if (SnapshotCodec.isImported(current))
            return "Показан сохранённый файл, а не свежая проверка.";
        if (previous == null || SnapshotCodec.isImported(previous)
                || !current.optString("repo").equalsIgnoreCase(previous.optString("repo")))
            return "Это первая проверка. Базы для сравнения нет, поэтому нельзя сказать, что проект стоит на месте или что в нём нет нового.";
        boolean branchesKnown = current.optBoolean("branchesComplete", false) && previous.optBoolean("branchesComplete", false);
        if (!branchesKnown && s.advanced + s.added + s.missing == 0 && s.issueChanges + s.pullChanges == 0)
            return "Список направлений неполный. Считать его полной картиной изменений нельзя.";
        if (s.advanced + s.added + s.missing > 0) {
            if (s.advanced == 0 && s.added > 0 && s.missing == 0)
                return "В текущей выборке впервые обнаружено направление, которого не было в прошлом полном списке. Дата создания не установлена.";
            if (s.advanced == 0 && s.missing > 0 && s.added == 0)
                return "Направление отсутствует в новом полном списке, хотя было в предыдущем. Удаление ветки не подтверждено.";
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
        if (current == null || SnapshotCodec.isImported(current)) return "Повторить загрузку";
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
        int newline = t.indexOf('\n');
        if (newline >= 0) t = t.substring(0, newline).trim();
        if (t.matches("^(docs|doc)(\\([^)]*\\))?!?:.*")) return "Автор сообщил об обновлении документов или инструкций";
        if (t.matches("^(fix|hotfix|bugfix)(\\([^)]*\\))?!?:.*")) return "Разработчик сообщил об исправлении";
        if (t.matches("^(feat|feature)(\\([^)]*\\))?!?:.*")) return "Разработчик сообщил о новой функции";
        if (t.matches("^(test|tests)(\\([^)]*\\))?!?:.*")) return "Автор сообщил об изменении тестов или проверок";
        if (t.matches("^(refactor)(\\([^)]*\\))?!?:.*")) return "Автор сообщил об изменении внутренней организации кода";
        if (t.matches("^merge(?:\\s|:).*")) return "Автор сообщил о слиянии истории разработки";
        return "В истории проекта появилась запись";
    }
    static String state(String s) {
        if ("open".equals(s)) return "Открыто в GitHub: выполнение не подтверждено";
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
        String branch = snapshot.optString("commitsBranch");
        if (branch.isEmpty() && !snapshot.optBoolean("commitsStale", false))
            branch = snapshot.optString("defaultBranch");
        if (branch.isEmpty() || "null".equals(branch)) return "ветки, имя которой неизвестно";
        return "ветки «"+branch+"»";
    }

    static String syncStatus(JSONObject current) {
        if (current == null) return "Синхронизации ещё не было.";
        String when = humanDate(current.optLong("capturedAt", 0));
        if (SnapshotCodec.isImported(current))
            return "Это импорт, не свежая проверка GitHub. Дата файла: " + when + ".";
        boolean partial = GithubClient.isPartialSnapshot(current);
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
            return "Так часто называют основную ветку. Одно имя не доказывает, что она основная или что проект готов.";
        return "Предположение по названию: " + branchKind(name) + ". Это не установленный факт.";
    }

    static String pullListLabel(JSONObject pull) {
        if (pull == null) return "Состояние предложения неизвестно.";
        if (ChangeSet.merged(pull))
            return "Внесено в целевую ветку по отметке GitHub. Это не проверка на устройстве.";
        if ("closed".equals(pull.optString("state")))
            return "Закрыто, но GitHub не отмечает слияние в доступных полях.";
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

    /** One status line for the owner: loaded, partial, comparable, or not. */
    static String ownerStatus(JSONObject current, JSONObject previous, Summary s) {
        if (current == null) return "Данные ещё не загружены.";
        if (SnapshotCodec.isImported(current))
            return "Показан сохранённый файл, не свежая загрузка. Дата файла: " + humanDate(current.optLong("capturedAt", 0)) + ".";
        boolean partial = GithubClient.isPartialSnapshot(current);
        boolean noBase = !comparableSnapshots(current, previous);
        String when = " Загрузка: " + humanDate(current.optLong("capturedAt", 0)) + ".";
        String base = noBase ? " Сравнивать не с чем."
                : " База сравнения: " + humanDate(previous.optLong("capturedAt", 0)) + ".";
        String kind;
        if (noBase) kind = "Нет достоверного сравнения.";
        else if (s != null && s.advanced + s.added + s.missing + s.issueChanges + s.pullChanges > 0)
            kind = "Есть изменения в загруженных данных.";
        else if (partial) kind = "Часть данных недоступна.";
        else kind = "В загруженных списках явного сдвига нет. Это не полная приёмка.";
        if (partial && !kind.contains("Часть")) kind += " Часть данных недоступна.";
        return kind + when + base;
    }

    /** Concrete facts. Does not turn a pointer move into progress or a file list. */
    static String ownerChanged(JSONObject current, JSONObject previous, Summary s) {
        String fallback = whatChanged(current, previous, s);
        if (current == null || previous == null) return fallback;
        if (SnapshotCodec.isImported(current) || SnapshotCodec.isImported(previous))
            return fallback;
        try {
            JSONArray events = ChangeSet.between(previous, current);
            if (events.length() == 0) return fallback;
            StringBuilder b = new StringBuilder();
            int n = 0;
            for (int i = 0; i < events.length() && n < 3; i++) {
                JSONObject e = events.optJSONObject(i);
                if (e == null) continue;
                String line = e.optString("event");
                if (line.isEmpty() || line.toLowerCase(java.util.Locale.ROOT).contains("sha")) continue;
                if (n > 0) b.append(' ');
                b.append(line);
                n++;
            }
            if (n == 0) return fallback;
            if (events.length() > n) b.append(" Остальное — в разделе «Изменения».");
            return b.toString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static String ownerWhere(JSONObject current, JSONObject previous) {
        if (current == null) return "Проект ещё не выбран.";
        if (SnapshotCodec.isImported(current))
            return "Показан импортированный файл. Чтобы определить, где изменилась работа, обновите данные с GitHub.";
        if (!comparableSnapshots(current, previous))
            return "Нет достоверной предыдущей проверки для сравнения направлений." + latestCommitLine(current);
        StringBuilder names = new StringBuilder();
        JSONArray now = current.optJSONArray("branches");
        JSONArray old = previous.optJSONArray("branches");
        java.util.Map<String, String> prior = new java.util.HashMap<>();
        if (old != null) for (int i = 0; i < old.length(); i++) {
            JSONObject b = old.optJSONObject(i);
            if (b != null && !b.optString("name").isEmpty()) prior.put(b.optString("name"), b.optString("sha"));
        }
        if (now != null) for (int i = 0; i < now.length() && names.length() < 180; i++) {
            JSONObject b = now.optJSONObject(i);
            if (b == null) continue;
            String name = b.optString("name");
            String sha = b.optString("sha");
            String was = prior.get(name);
            if (was != null && !was.isEmpty() && !sha.isEmpty() && !was.equals(sha)) {
                if (names.length() > 0) names.append(", ");
                names.append(name);
            }
        }
        String latest = latestCommitLine(current);
        if (names.length() == 0)
            return "В загруженных направлениях сдвига точки работы не видно." + latest;
        return "Сдвинулись направления: " + names + "." + latest;
    }

    static String ownerProblems(JSONObject current) {
        if (current == null) return "Проблемы ещё не читались.";
        StringBuilder b = new StringBuilder();
        b.append("Автоматические проверки не входят в общий снимок: их запрашивают у конкретной записи. Состояние CI всего проекта неизвестно. ");
        JSONArray issues = current.optJSONArray("issues");
        boolean complete = current.optBoolean("issuesComplete", false);
        if (!complete) b.append("Список задач неполный. ");
        int open = 0;
        String ordinary = null;
        String defect = null;
        String blocker = null;
        if (issues != null) for (int i = 0; i < issues.length(); i++) {
            JSONObject x = issues.optJSONObject(i);
            if (x == null || !"open".equals(x.optString("state"))) continue;
            open++;
            String title = clipTitle(x.optString("title"));
            String kind = problemKind(title, x.optString("labels"));
            if ("blocker".equals(kind)) {
                if (blocker == null) blocker = title;
            } else if ("defect".equals(kind)) {
                if (defect == null) defect = title;
            } else if (ordinary == null) ordinary = title;
        }
        if (blocker != null)
            b.append("По названию похоже на блокировку, это не проверка: «").append(blocker).append("». ");
        if (defect != null)
            b.append("По названию похоже на дефект, это не проверка: «").append(defect).append("». ");
        if (ordinary != null)
            b.append("Открытая задача, не подтверждённая ошибка: «").append(ordinary).append("». ");
        if (open == 0 && complete && issues != null)
            b.append("В загруженном списке нет открытых задач. Это не значит, что программа без дефектов. ");
        else if (open > 0) b.append("Открытых задач в выборке: ").append(open).append(". ");
        return b.toString().trim();
    }

    /** defect, blocker, or ordinary task. A title is never a confirmed failure. */
    static String problemKind(String title, String labels) {
        String text = ((title == null ? "" : title) + " " + (labels == null ? "" : labels)).toLowerCase(java.util.Locale.ROOT);
        if (text.contains("blocker") || text.contains("blocked") || text.contains("blocking") || text.contains("блок"))
            return "blocker";
        if (looksNamedDefect(title, labels)) return "defect";
        return "task";
    }

    private static boolean comparableSnapshots(JSONObject current, JSONObject previous) {
        return current != null && previous != null
                && !SnapshotCodec.isImported(current) && !SnapshotCodec.isImported(previous)
                && current.optString("repo").equalsIgnoreCase(previous.optString("repo"));
    }

    static String previousBranchSha(String name, JSONObject current, JSONObject previous) {
        return comparableSnapshots(current, previous) ? shaOf(previous, name) : null;
    }

    /** What the owner can say about one branch pointer before compare is fetched. */
    static String branchPointerLine(String name, JSONObject current, JSONObject previous) {
        String guess = branchAssumption(name);
        if (!comparableSnapshots(current, previous))
            return "Сравнивать указатель не с чем. " + guess;
        String sha = shaOf(current, name);
        String prior = shaOf(previous, name);
        if ((sha != null && sha.isEmpty()) || (prior != null && prior.isEmpty()))
            return "Для сравнения нужны оба указателя ветки. " + guess;
        if (prior == null) {
            if (previous.optJSONArray("branches") == null || !previous.optBoolean("branchesComplete", false))
                return "В неполном прошлом списке этого имени не было. Новым его называть нельзя. " + guess;
            return "В прошлой полной проверке этого имени не было. Создание ветки не доказано. " + guess;
        }
        if (sha == null)
            return "В текущем списке этого имени нет. Удаление не подтверждено. " + guess;
        if (!prior.equals(sha))
            return "Точка работы сдвинулась. Характер изменения ещё неизвестен: сравнение файлов не открыто. " + guess;
        return "Точка работы с прошлой проверки не сдвинулась. " + guess;
    }

    private static String shaOf(JSONObject snap, String name) {
        JSONArray list = snap.optJSONArray("branches");
        if (list == null || name == null) return null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject b = list.optJSONObject(i);
            if (b != null && name.equals(b.optString("name"))) return b.optString("sha");
        }
        return null;
    }

    static String ownerWho(JSONObject current) {
        if (current == null) return "Исполнитель не установлен.";
        JSONArray commits = current.optJSONArray("commits");
        JSONObject first = commits == null ? null : commits.optJSONObject(0);
        String author = first == null ? "" : first.optString("author").trim();
        if (author.isEmpty())
            return "Исполнитель не установлен. В снимке нет автора последней записи основной ветки. Это не значит, что кто-то работает прямо сейчас.";
        String source = SnapshotCodec.isImported(current) ? "В импортированном файле указан автор: "
                : current.optBoolean("commitsStale", false) ? "Автор в устаревшем снимке истории: "
                : "Автор последней записи основной ветки в полученной выборке: ";
        return source + author
                + ". Это записанное имя, не доказательство, что человек или программа работает сейчас. Исполнитель задачи не установлен.";
    }

    static String unknownOnce(String raw) {
        String text = raw == null ? "" : raw.trim();
        while (text.regionMatches(true, 0, "Не подтверждено", 0, "Не подтверждено".length())) {
            text = text.substring("Не подтверждено".length()).replaceFirst("^[:\\s]+", "");
        }
        if (text.isEmpty()) text = "тесты и готовность на телефоне.";
        return "Что не проверено: " + text;
    }

    static String eventMeaning(String kind) {
        if (kind == null) return "Факт записан. Готовность проекта из него не следует.";
        if ("branch_moved".equals(kind))
            return "Точка работы сдвинулась. Пока нет сравнения, нельзя сказать, были это новые записи, откат или переписывание.";
        if ("branch_seen".equals(kind))
            return "Ветка обнаружена в текущем снимке. Её имени не было в предыдущем полном списке. Дата создания и автор неизвестны.";
        if ("branch_absent".equals(kind))
            return "Имени нет в новом полном списке. Удаление ветки не подтверждено.";
        if ("issue_touch".equals(kind) || "pull_touch".equals(kind))
            return "GitHub сообщает об обновлении записи. Содержание изменения не определено.";
        if ("issue_new".equals(kind))
            return "Появилась задача. Это не доказанная ошибка программы.";
        if ("issue_state".equals(kind) || "pull_state".equals(kind))
            return "Изменилось состояние записи. Закрытие не означает, что работу проверили.";
        if ("issue_title".equals(kind) || "pull_title".equals(kind))
            return "Изменилось название. Это не завершение работы.";
        if ("pull_merged".equals(kind))
            return "GitHub отмечает слияние. Это не проверка на телефоне.";
        if ("pull_new".equals(kind))
            return "Появилось предложение изменений. Результат не подтверждён.";
        return "Факт записан. Готовность проекта из него не следует.";
    }

    static String eventWhere(JSONObject event) {
        if (event == null) return "Место не указано.";
        String id = event.optString("id");
        String kind = event.optString("kind");
        if (id.startsWith("branch:")) return "Направление «" + id.substring("branch:".length()) + "».";
        if (id.startsWith("issue#")) return "Задача " + id.substring("issue".length()) + ".";
        if (id.startsWith("pull#")) return "Предложение " + id.substring("pull".length()) + ".";
        if (kind.startsWith("issue")) return "Задача.";
        if (kind.startsWith("pull")) return "Предложение изменений.";
        return "Место в снимке: " + (id.isEmpty() ? "не указано" : id) + ".";
    }

    static boolean looksNamedDefect(String title, String labels) {
        String text = ((title == null ? "" : title) + " " + (labels == null ? "" : labels)).toLowerCase(java.util.Locale.ROOT);
        return text.contains("bug") || text.contains("defect") || text.contains("ошиб")
                || text.contains("дефект") || text.contains("crash") || text.contains("баг");
    }

    private static String latestCommitLine(JSONObject current) {
        JSONArray commits = current.optJSONArray("commits");
        if (commits == null || commits.length() == 0) return " Последней записи основной ветки в снимке нет.";
        JSONObject c = commits.optJSONObject(0);
        if (c == null) return "";
        String scope = current.optBoolean("commitsStale", false)
                ? "из устаревшей выборки истории" : commitScope(current);
        return " Последняя запись " + scope + ": " + commitSummary(c.optString("message"))
                + ". Дата: " + humanDate(c.optString("date"))
                + ". Это заявление автора, не проверка, и не последняя запись всего проекта.";
    }

    private static String clipTitle(String title) {
        if (title == null || title.trim().isEmpty()) return "без названия";
        String clean = title.replace('\n', ' ').trim();
        return clean.length() <= 80 ? clean : clean.substring(0, 77) + "…";
    }
}
