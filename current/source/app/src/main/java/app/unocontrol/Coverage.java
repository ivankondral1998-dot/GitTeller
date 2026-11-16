package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Why a saved snapshot is or is not complete. Reads stored fields only. Never calls GitHub. */
final class Coverage {
    static final String COMPLETE_WITHIN_REQUEST = "COMPLETE_WITHIN_REQUEST";
    static final String SAMPLED_BY_DESIGN = "SAMPLED_BY_DESIGN";
    static final String TRUNCATED_BY_CAP = "TRUNCATED_BY_CAP";
    static final String PARTIAL_PAGE = "PARTIAL_PAGE";
    static final String STALE_AFTER_FAILURE = "STALE_AFTER_FAILURE";
    static final String CACHED_NOT_MODIFIED = "CACHED_NOT_MODIFIED";
    static final String ACCESS_OR_API_FAILURE = "ACCESS_OR_API_FAILURE";
    static final String NOT_REQUESTED = "NOT_REQUESTED";
    static final String NOT_VERIFIED = "NOT_VERIFIED";
    static final String IMPORTED = "IMPORTED";
    static final String UNKNOWN = "UNKNOWN";

    static final class Row {
        final String area;
        final String title;
        final String code;
        final int count;
        final long at;
        final String fact;
        final String meaning;
        final String action;
        final String novice;
        final String developer;

        Row(String area, String title, String code, int count, long at, String fact, String meaning,
            String action, String novice, String developer) {
            this.area = area;
            this.title = title;
            this.code = code;
            this.count = count;
            this.at = at;
            this.fact = fact;
            this.meaning = meaning;
            this.action = action;
            this.novice = novice;
            this.developer = developer;
        }

        String shown(boolean developerMode) { return developerMode ? developer : novice; }
    }

    private Coverage() {}

    static List<Row> explain(JSONObject snapshot) {
        List<Row> rows = new ArrayList<>();
        rows.add(snapshotRow(snapshot));
        rows.add(listRow(snapshot, "branches", "Ветки", "ветки", GithubClient.BRANCH_PAGE_CAP));
        rows.add(listRow(snapshot, "issues", "Задачи", "задачи", GithubClient.ISSUE_PAGE_CAP));
        rows.add(listRow(snapshot, "pulls", "Предложения", "предложения", GithubClient.PULL_PAGE_CAP));
        rows.add(commitsRow(snapshot));
        rows.add(checksRow());
        return rows;
    }

    static Row one(JSONObject snapshot, String field) {
        for (Row row : explain(snapshot)) if (row.area.equals(field)) return row;
        return row(field, field, UNKNOWN, -1, 0, "Раздел не классифицирован.",
                "Неизвестное не считается полной загрузкой.", "refresh",
                field + ": причина неизвестна.", field + " · UNKNOWN");
    }

    /** Short home line. Never claims the project is fine when checks were not read. */
    static String compact(JSONObject snapshot, boolean developer) {
        if (snapshot == null)
            return "Снимка нет. «Обновить» само не запускается. Это не пустой проект и не успешная проверка.";
        StringBuilder text = new StringBuilder();
        if (SnapshotCodec.isImported(snapshot))
            text.append("Снимок импортирован. Это не живые данные. ");
        text.append("История коммитов — выборка, не вся история. Проверки пока не подтверждены. ");
        Row failure = null;
        int unknown = 0;
        for (Row row : explain(snapshot)) {
            if ("snapshot".equals(row.area) || "checks".equals(row.area) || "commits".equals(row.area)) continue;
            if (ACCESS_OR_API_FAILURE.equals(row.code) || STALE_AFTER_FAILURE.equals(row.code)
                    || TRUNCATED_BY_CAP.equals(row.code) || PARTIAL_PAGE.equals(row.code)) {
                if (failure == null) failure = row;
            } else if (UNKNOWN.equals(row.code) || NOT_REQUESTED.equals(row.code)) unknown++;
        }
        if (failure != null) text.append(failure.novice).append(' ');
        else if (unknown > 0) text.append("Часть списков без доказанной полноты. ");
        else text.append("Списки в границах запроса есть. Это не тесты и не вся история. ");
        if (developer) {
            text.append("Источник: ").append(snapshot.optString("origin", "не определён"))
                    .append(". Запись: ").append(HumanReport.humanDate(snapshot.optLong("capturedAt", 0))).append('.');
        }
        return text.toString().trim();
    }

    private static Row snapshotRow(JSONObject snapshot) {
        if (snapshot == null)
            return row("snapshot", "Снимок", UNKNOWN, 0, 0, "Снимок не сохранён.",
                    "Показывать нечего.", "refresh",
                    "Снимка проекта ещё нет.", "Снимок · UNKNOWN");
        long at = snapshot.optLong("capturedAt", 0);
        if (SnapshotCodec.isImported(snapshot))
            return row("snapshot", "Снимок", IMPORTED, 0, at, "Архив JSON, не ответ GitHub этого сеанса.",
                    "Импорт нельзя считать свежей проверкой.", "refresh",
                    "Это сохранённый файл, не свежее чтение GitHub.",
                    "Снимок · IMPORTED · " + HumanReport.humanDate(at));
        String origin = snapshot.optString("origin", "");
        if (origin.isEmpty())
            return row("snapshot", "Снимок", UNKNOWN, 0, at, "Поле origin отсутствует.",
                    "Без источника нельзя назвать снимок полным.", "refresh",
                    "Откуда снимок — неизвестно.", "Снимок · UNKNOWN · origin нет");
        return row("snapshot", "Снимок", origin.equals("LIVE") || origin.equals("PARTIAL") || origin.equals("CACHED")
                        ? origin : UNKNOWN,
                0, at, "Источник в снимке: " + origin + ".",
                "LIVE не означает, что каждый список полон и что тесты прошли.", "none",
                "Снимок сохранён. Его полнота разобрана по разделам ниже.",
                "Снимок · " + origin + " · " + HumanReport.humanDate(at));
    }

    private static Row checksRow() {
        return row("checks", "Проверки", NOT_VERIFIED, -1, 0,
                "Общий снимок не запрашивает checks, статусы и Actions.",
                "Отсутствие записи о CI не является успешным тестом.", "none",
                "Проверки пока не подтверждены. Пустое место — не успех.",
                "Проверки · NOT_VERIFIED · запрос в общем снимке не выполнялся. Это не PASS.");
    }

    private static Row commitsRow(JSONObject snapshot) {
        if (snapshot == null)
            return row("commits", "Коммиты", UNKNOWN, -1, 0, "Снимка нет.", "Историю не с чем сравнивать.", "refresh",
                    "История ещё не загружена.", "Коммиты · UNKNOWN");
        int count = countOf(snapshot, "commits");
        long at = snapshot.optLong("commitsFetchedAt", 0);
        if (SnapshotCodec.isImported(snapshot))
            return row("commits", "Коммиты", IMPORTED, count, at, "История пришла из файла импорта.",
                    "Это не живая выборка и не вся история.", "refresh",
                    "История в файле. Она не заменяет чтение GitHub.",
                    "Коммиты · IMPORTED · записей " + shown(count));
        if (snapshot.optBoolean("commitsStale", false))
            return failureRow("commits", "Коммиты", STALE_AFTER_FAILURE, count, at, notes(snapshot, "история"),
                    "Показан прошлый список коммитов: новое чтение не удалось.",
                    "Старая выборка не описывает проект сейчас.", "refresh",
                    "История устарела: свежий ответ не получен.");
        if (!snapshot.has("commitsFetched"))
            return row("commits", "Коммиты", UNKNOWN, count, at,
                    "В снимке нет признака commitsFetched.",
                    "Без этого признака нельзя назвать историю ни полной, ни штатной выборкой.", "refresh",
                    "Неизвестно, читалась ли история.",
                    "Коммиты · UNKNOWN · нет commitsFetched");
        if (!snapshot.optBoolean("commitsFetched", false)) {
            String joined = join(notes(snapshot, "история"));
            if (joined.contains("запрос не отправлялся"))
                return row("commits", "Коммиты", NOT_REQUESTED, count, at, joined.isEmpty() ? "Запрос истории не отправлялся." : joined,
                        "Раздел не читался. Пустота не означает отсутствие коммитов.", "refresh",
                        "История в этот раз не запрашивалась.",
                        "Коммиты · NOT_REQUESTED · " + clip(joined));
            if (http(joined) != null)
                return failureRow("commits", "Коммиты", ACCESS_OR_API_FAILURE, count, at, notes(snapshot, "история"),
                        "Запрос истории не удался.", "Ошибка сети или GitHub. Это не пустая история.", actionFor(joined),
                        "Историю прочитать не удалось.");
            return row("commits", "Коммиты", UNKNOWN, count, at,
                    joined.isEmpty() ? "commitsFetched=false, причины в снимке нет." : joined,
                    "Причина не записана. Нельзя назвать это выборкой или успехом.", "refresh",
                    "История не получена, причина неизвестна.",
                    "Коммиты · UNKNOWN · " + clip(joined));
        }
        return row("commits", "Коммиты", SAMPLED_BY_DESIGN, count, at,
                "Запрошены последние 12 коммитов основной ветки. commitsComplete намеренно не ставится.",
                "Не требуется, это выборка. Это не вся история и не проверка тестов.", "none",
                "История показана выборочно. Это не ошибка.",
                "Коммиты · SAMPLED_BY_DESIGN · записей " + shown(count) + " · последние 12 основной ветки · "
                        + HumanReport.humanDate(at));
    }

    private static Row listRow(JSONObject snapshot, String field, String title, String label, int cap) {
        if (snapshot == null)
            return row(field, title, UNKNOWN, -1, 0, "Снимка нет.", "Список неизвестен.", "refresh",
                    title + ": данных нет.", title + " · UNKNOWN");
        int count = countOf(snapshot, field);
        long at = snapshot.optLong(field + "FetchedAt", 0);
        List<String> notes = notes(snapshot, label);
        String joined = join(notes);
        if (SnapshotCodec.isImported(snapshot))
            return row(field, title, IMPORTED, count, at, "Список из файла импорта.",
                    "Импорт не является живым полным чтением.", "refresh",
                    title + ": импортированный снимок, не живые данные.",
                    title + " · IMPORTED · записей " + shown(count));
        if (snapshot.optBoolean(field + "Stale", false))
            return failureRow(field, title, STALE_AFTER_FAILURE, count, at, notes,
                    "Показан прошлый список: свежее чтение не удалось.",
                    "Эти записи могли устареть.", "refresh",
                    title + ": показаны старые данные, новое чтение не удалось.");
        if (snapshot.optBoolean(field + "Cached", false)) {
            boolean complete = snapshot.optBoolean(field + "Complete", false);
            return row(field, title, CACHED_NOT_MODIFIED, count, at,
                    "Ответ 304 сохранил прошлый список. Полнота того списка: " + (complete ? "в границах прошлого запроса" : "не подтверждена") + ".",
                    "Это не новое чтение. 304 не означает, что тесты прошли.",
                    complete ? "none" : "refresh",
                    complete ? title + ": GitHub подтвердил прежний список. Это не новая проверка."
                            : title + ": прежний список подтверждён не полностью.",
                    title + " · CACHED_NOT_MODIFIED · записей " + shown(count)
                            + " · complete=" + complete + " · " + HumanReport.humanDate(at));
        }
        if (joined.contains("предел ") && joined.contains("страниц"))
            return truncated(field, title, count, at, cap, joined);
        if (joined.contains("загрузка прервана") || joined.contains("небезопасная ссылка"))
            return row(field, title, PARTIAL_PAGE, count, at, clip(joined),
                    "Страницы оборвались. Видимый кусок не описывает весь список.", "refresh",
                    title + ": список оборвался на следующей странице. Это не весь проект.",
                    title + " · PARTIAL_PAGE · записей " + shown(count) + " · " + clip(joined));
        String http = http(joined);
        if (!snapshot.has(field + "Fetched")) {
            if (http != null)
                return access(field, title, count, at, joined, http);
            return row(field, title, UNKNOWN, count, at,
                    joined.isEmpty() ? "Нет признака " + field + "Fetched. Старая схема не считается полной." : clip(joined),
                    "Неизвестное не называется полной загрузкой.", "refresh",
                    title + ": неизвестно, полным ли был список.",
                    title + " · UNKNOWN · нет " + field + "Fetched");
        }
        if (!snapshot.optBoolean(field + "Fetched", false)) {
            if (joined.contains("запрос не отправлялся"))
                return row(field, title, NOT_REQUESTED, count, at,
                        joined.isEmpty() ? "Запрос не отправлялся." : clip(joined),
                        "Раздел не читался. Пустой экран не означает ноль записей.", "refresh",
                        title + ": в этот раз не запрашивались.",
                        title + " · NOT_REQUESTED · " + clip(joined));
            if (http != null) return access(field, title, count, at, joined, http);
            return row(field, title, UNKNOWN, count, at,
                    joined.isEmpty() ? field + "Fetched=false, текста ошибки нет." : clip(joined),
                    "Причина не записана.", "refresh",
                    title + ": не получены, причина неизвестна.",
                    title + " · UNKNOWN · " + clip(joined));
        }
        if (snapshot.optBoolean(field + "Complete", false) && notes.isEmpty())
            return row(field, title, COMPLETE_WITHIN_REQUEST, count, at,
                    "Список получен в границах запроса (не больше " + cap + " страниц по 100). Это не весь репозиторий и не вся история.",
                    "Полный ответ в этих границах не доказывает успешные тесты.", "none",
                    title + " получены в границах запроса. Это не вся история проекта.",
                    title + " · COMPLETE_WITHIN_REQUEST · записей " + shown(count) + " · страниц до " + cap
                            + " · не весь репозиторий · " + HumanReport.humanDate(at));
        if (http != null) return access(field, title, count, at, joined, http);
        if (!notes.isEmpty())
            return row(field, title, UNKNOWN, count, at, clip(joined),
                    "Замечание есть, но оно не доказывает ни предел страниц, ни код HTTP.", "refresh",
                    title + ": список неполный, точная причина не разобрана.",
                    title + " · UNKNOWN · " + clip(joined));
        return row(field, title, UNKNOWN, count, at, "Признак полноты ложный или причина не записана.",
                "Неполный список без причины не называется успешным.", "refresh",
                title + ": полнота не доказана.",
                title + " · UNKNOWN · complete=" + snapshot.optBoolean(field + "Complete", false));
    }

    private static Row truncated(String field, String title, int count, long at, int cap, String joined) {
        String pages = "предел страниц";
        int start = joined.indexOf("предел ");
        if (start >= 0) {
            int end = joined.indexOf(" страниц", start);
            if (end > start) pages = joined.substring(start, end + " страниц".length());
        }
        return row(field, title, TRUNCATED_BY_CAP, count, at,
                "Пагинация остановлена: " + pages + ". Рабочий предел этого раздела — " + cap + " страниц.",
                "Дальше списка в снимке нет. Это не ошибка телефона и не весь репозиторий.", "refresh",
                title + ": показаны не все, запрос дошёл до своего предела. Это не сбой телефона.",
                title + " · TRUNCATED_BY_CAP · записей " + shown(count) + " · " + pages + " · cap " + cap);
    }

    private static Row access(String field, String title, int count, long at, String joined, String httpFact) {
        return row(field, title, ACCESS_OR_API_FAILURE, count, at, httpFact,
                "Ошибка чтения не означает, что записей нет и что проверки прошли.", actionFor(joined),
                title + ": " + httpFact,
                title + " · ACCESS_OR_API_FAILURE · записей " + shown(count) + " · " + httpFact);
    }

    private static Row failureRow(String field, String title, String code, int count, long at, List<String> notes,
                                  String base, String meaning, String action, String novice) {
        String http = http(join(notes));
        String fact = http == null ? base : base + " " + http;
        return row(field, title, code, count, at, fact, meaning, action, novice,
                title + " · " + code + " · записей " + shown(count) + " · " + fact);
    }

    static String http(String text) {
        if (text == null || text.isEmpty()) return null;
        boolean limit = text.contains("Лимит запросов");
        if (limit && (text.contains("429") || text.contains("403")))
            return "Лимит запросов подтверждён ответом GitHub.";
        if (text.contains("(401)") || text.contains("HTTP 401"))
            return "Ответ 401: токен не принят. Токен приложением не удалён.";
        if (text.contains("(404)") || text.contains("HTTP 404"))
            return "Ответ 404: проект не найден или к нему нет доступа.";
        if (text.contains("(429)") || text.contains("HTTP 429"))
            return "Ответ 429: лимит запросов.";
        if (text.contains("не ответил вовремя"))
            return "Таймаут: GitHub не ответил вовремя. Это не пустой список.";
        if (text.contains("Нет соединения"))
            return "Нет соединения с GitHub. Это не пустой список.";
        if (text.contains("(403)") || text.contains("HTTP 403"))
            return "Причина не установлена (403), возможно права или лимит.";
        if (text.contains("HTTP "))
            return clip(text);
        return null;
    }

    private static String actionFor(String text) {
        if (text == null) return "refresh";
        if (text.contains("(401)") || text.contains("HTTP 401") || text.contains("(404)") || text.contains("HTTP 404")
                || text.contains("(403)") || text.contains("HTTP 403"))
            return "access";
        return "refresh";
    }

    private static List<String> notes(JSONObject snapshot, String label) {
        List<String> out = new ArrayList<>();
        JSONArray warnings = snapshot.optJSONArray("warnings");
        if (warnings == null) return out;
        String prefix = label + ":";
        for (int i = 0; i < warnings.length(); i++) {
            String line = warnings.optString(i);
            if (line != null && line.startsWith(prefix)) out.add(line);
        }
        return out;
    }

    private static String join(List<String> lines) {
        StringBuilder b = new StringBuilder();
        for (String line : lines) {
            if (b.length() > 0) b.append(' ');
            b.append(line);
        }
        return b.toString();
    }

    private static int countOf(JSONObject snapshot, String field) {
        JSONArray list = snapshot.optJSONArray(field);
        return list == null ? -1 : list.length();
    }

    private static String shown(int count) { return count < 0 ? "нет массива" : Integer.toString(count); }

    private static String clip(String value) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= 220 ? clean : clean.substring(0, 220) + "…";
    }

    private static Row row(String area, String title, String code, int count, long at, String fact, String meaning,
                           String action, String novice, String developer) {
        return new Row(area, title, code, count, at, fact, meaning, action, novice, developer);
    }
}
