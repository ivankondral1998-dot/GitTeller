package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Read-only GitHub access. Never writes, and never persists auth credentials. */
final class GithubClient {
    static final class Result {
        final JSONObject snapshot;
        Result(JSONObject snapshot) { this.snapshot = snapshot; }
    }
    private final String token;
    private volatile HttpURLConnection inflight;
    GithubClient(String token) { this.token = token == null ? "" : token.trim(); }

    void cancel() {
        HttpURLConnection c = inflight;
        if (c != null) c.disconnect();
    }

    Result load(String repo) throws Exception {
        if (!repo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}") || repo.contains(".."))
            throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        String root = "https://api.github.com/repos/" + repo;
        JSONObject meta = jsonObject(get(root));
        String actual = meta.optString("full_name", "");
        if (!repo.equalsIgnoreCase(actual))
            throw new IllegalStateException("GitHub вернул другой репозиторий.");
        String branch = meta.optString("default_branch", "main");

        List<String> warnings = new ArrayList<>();
        JSONArray branches = pages(root + "/branches?per_page=100", 20, warnings, "ветки");
        boolean branchComplete = true;
        for (String w : warnings) if (w.startsWith("ветки:")) branchComplete = false;
        if (branches.length() == 0 && !branchComplete)
            throw new IllegalStateException("Список веток не получен; старый снимок сохранён.");

        JSONArray commits;
        try { commits = jsonArray(get(root + "/commits?sha=" + encodePathPart(branch) + "&per_page=12")); }
        catch (Exception e) { commits = new JSONArray(); warnings.add("История коммитов не загружена: " + safeMessage(e)); }
        JSONArray issues;
        boolean issuesComplete;
        try {
            int warningsBefore = warnings.size();
            issues = pages(root + "/issues?state=all&per_page=100", 4, warnings, "задачи");
            issuesComplete = warnings.size() == warningsBefore;
        } catch (Exception e) {
            issues = new JSONArray();
            issuesComplete = false;
            warnings.add("Задачи недоступны: " + safeMessage(e));
        }
        JSONArray pulls;
        boolean pullsComplete;
        try {
            int warningsBefore = warnings.size();
            pulls = pages(root + "/pulls?state=all&per_page=100", 4, warnings, "запросы изменений");
            pullsComplete = warnings.size() == warningsBefore;
        } catch (Exception e) {
            pulls = new JSONArray();
            pullsComplete = false;
            warnings.add("Запросы изменений недоступны: " + safeMessage(e));
        }

        JSONObject out = new JSONObject();
        out.put("repo", actual).put("defaultBranch", branch)
                .put("capturedAt", System.currentTimeMillis())
                .put("branches", slimBranches(branches))
                .put("commits", slimCommits(commits))
                .put("issues", slimIssues(issues))
                .put("pulls", slimIssues(pulls))
                .put("branchesComplete", branchComplete)
                .put("issuesComplete", issuesComplete)
                .put("pullsComplete", pullsComplete);
        JSONArray warn = new JSONArray();
        for (String w : warnings) warn.put(w);
        out.put("warnings", warn);
        return new Result(out);
    }

    private JSONArray pages(String start, int max, List<String> warnings, String label) throws Exception {
        JSONArray all = new JSONArray();
        String url = start;
        for (int page = 0; page < max; page++) {
            Response response;
            try { response = get(url); }
            catch (Exception e) {
                if (page == 0) throw e;
                warnings.add(label + ": загрузка прервана");
                return all;
            }
            JSONArray current = jsonArray(response);
            for (int j = 0; j < current.length(); j++) all.put(current.get(j));
            String next = nextLink(response.link);
            if (next == null) return all;
            if (!next.startsWith("https://api.github.com/repos/")) {
                warnings.add(label + ": небезопасная ссылка продолжения отклонена");
                return all;
            }
            url = next;
        }
        warnings.add(label + ": предел " + max + " страниц");
        return all;
    }

    private static String nextLink(String link) {
        if (link == null) return null;
        for (String part : link.split(",")) {
            if (part.contains("rel=\"next\"")) {
                int a = part.indexOf('<'), b = part.indexOf('>');
                if (a >= 0 && b > a) return part.substring(a + 1, b);
            }
        }
        return null;
    }

    private static JSONArray slimBranches(JSONArray raw) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject x = raw.optJSONObject(i);
            if (x == null) continue;
            String sha = x.optJSONObject("commit") == null ? "" : x.optJSONObject("commit").optString("sha");
            if (!sha.matches("[0-9a-fA-F]{40}")) continue;
            out.put(new JSONObject().put("name", x.optString("name")).put("sha", sha));
        }
        return out;
    }

    private static JSONArray slimCommits(JSONArray raw) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject x = raw.optJSONObject(i);
            if (x == null) continue;
            JSONObject commit = x.optJSONObject("commit");
            String message = commit == null ? "" : commit.optString("message", "");
            if (message.contains("\n")) message = message.substring(0, message.indexOf('\n'));
            String date = "";
            if (commit != null && commit.optJSONObject("committer") != null)
                date = commit.optJSONObject("committer").optString("date", "");
            out.put(new JSONObject().put("sha", x.optString("sha")).put("message", message)
                    .put("date", date));
        }
        return out;
    }

    private static JSONArray slimIssues(JSONArray raw) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject x = raw.optJSONObject(i);
            if (x == null || x.has("pull_request")) continue;
            out.put(new JSONObject().put("number", x.optInt("number"))
                    .put("title", x.optString("title"))
                    .put("state", x.optString("state")));
        }
        return out;
    }

    private Response get(String address) throws Exception {
        // Whitelist prevents redirecting a PAT to an arbitrary host.
        if (!address.startsWith("https://api.github.com/repos/"))
            throw new SecurityException("Недопустимый адрес GitHub API.");
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(14000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setRequestProperty("User-Agent", "GitTeller/1.0.6");
        if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        inflight = c;
        try {
            int status = c.getResponseCode();
            if (status != 200) {
                throw new IllegalStateException(errorForStatus(status, c.getHeaderField("X-RateLimit-Remaining")));
            }
            String link = c.getHeaderField("Link");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) {
                    if (data.size() + n > 8_000_000) throw new IllegalStateException("Ответ GitHub слишком большой.");
                    data.write(buf, 0, n);
                }
            }
            return new Response(data.toString(StandardCharsets.UTF_8.name()), link);
        } finally {
            if (inflight == c) inflight = null;
            c.disconnect();
        }
    }

    static final class Extra {
        final String text;
        Extra(String text) { this.text = text; }
    }

    Extra readIssue(String repo, int number) throws Exception {
        JSONObject issue = jsonObject(get(root(repo) + "/issues/" + number));
        return formatIssue(issue);
    }

    Extra readPull(String repo, int number) throws Exception {
        String base = root(repo) + "/pulls/" + number;
        JSONObject pull = jsonObject(get(base));
        JSONArray files = new JSONArray();
        boolean filesCut = false;
        String filesNote = null;
        try {
            Response page = get(base + "/files?per_page=30");
            files = jsonArray(page);
            filesCut = nextLink(page.link) != null;
        } catch (Exception e) {
            filesNote = HumanReport.NOT_ENOUGH + ": список файлов не прочитан (" + safeMessage(e) + ").";
        }
        String checks = HumanReport.NOT_ENOUGH + ": проверки не загружены.";
        String sha = pull.optJSONObject("head") == null ? "" : pull.optJSONObject("head").optString("sha");
        if (sha.matches("[0-9a-fA-F]{40}")) {
            try { checks = formatChecks(jsonObject(get(root(repo) + "/commits/" + sha + "/check-runs?per_page=20"))); }
            catch (Exception e) { checks = HumanReport.NOT_ENOUGH + ": проверки не прочитаны (" + safeMessage(e) + ")."; }
        }
        return formatPull(pull, files, filesCut, filesNote, checks);
    }

    Extra readCommit(String repo, String sha) throws Exception {
        if (sha == null || !sha.matches("[0-9a-fA-F]{40}"))
            throw new IllegalArgumentException("Нет полного SHA для запроса файлов.");
        JSONObject commit = jsonObject(get(root(repo) + "/commits/" + sha));
        String checks;
        try { checks = formatChecks(jsonObject(get(root(repo) + "/commits/" + sha + "/check-runs?per_page=20"))); }
        catch (Exception e) { checks = HumanReport.NOT_ENOUGH + ": проверки не прочитаны (" + safeMessage(e) + ")."; }
        return formatCommit(commit, checks);
    }

    private static String root(String repo) {
        if (repo == null || !repo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}") || repo.contains(".."))
            throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        return "https://api.github.com/repos/" + repo;
    }

    static Extra formatIssue(JSONObject issue) {
        StringBuilder b = new StringBuilder();
        String body = issue == null ? "" : issue.optString("body").trim();
        b.append(body.isEmpty() ? "Описания в GitHub нет." : clip(body)).append("\n");
        b.append(labels(issue)).append("\n");
        b.append(HumanReport.NO_CHECK).append(". Текст записи не доказывает, что работа выполнена.");
        return new Extra(b.toString());
    }

    static Extra formatPull(JSONObject pull, JSONArray files, boolean filesCut, String filesNote, String checks) {
        StringBuilder b = new StringBuilder();
        boolean merged = pull != null && pull.optBoolean("merged", false);
        String mergedAt = pull == null ? "" : pull.optString("merged_at");
        if (merged) b.append("GitHub отмечает, что предложение слито");
        else b.append("GitHub не сообщает, что предложение слито");
        if (mergedAt != null && !mergedAt.isEmpty() && !"null".equals(mergedAt)) b.append(" (").append(mergedAt).append(")");
        b.append(". Закрытие без этого признака не означает merge.\n");
        String body = pull == null ? "" : pull.optString("body").trim();
        b.append(body.isEmpty() ? "Описания нет." : clip(body)).append("\n");
        if (filesNote != null) b.append(filesNote).append("\n");
        else b.append(fileNames(files, filesCut)).append("\n");
        b.append(checks == null ? HumanReport.NOT_ENOUGH + ": проверки не загружены." : checks).append("\n");
        b.append(HumanReport.NO_CHECK).append(". Даже успешная отметка GitHub не заменяет проверку на устройстве.");
        return new Extra(b.toString());
    }

    static Extra formatCommit(JSONObject commit, String checks) {
        StringBuilder b = new StringBuilder();
        JSONArray files = commit == null ? null : commit.optJSONArray("files");
        b.append(fileNames(files, false)).append("\n");
        b.append(checks == null ? HumanReport.NOT_ENOUGH + ": проверки не загружены." : checks).append("\n");
        b.append(HumanReport.NO_CHECK).append(". Список файлов не объясняет, работает ли программа.");
        return new Extra(b.toString());
    }

    static String formatChecks(JSONObject runs) {
        JSONArray list = runs == null ? null : runs.optJSONArray("check_runs");
        int total = runs == null ? 0 : runs.optInt("total_count", list == null ? 0 : list.length());
        if (list == null || list.length() == 0) return "Проверок GitHub в ответе нет. " + HumanReport.NO_CHECK + ".";
        int ok = 0, bad = 0, other = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            String conclusion = row == null ? "" : row.optString("conclusion");
            if ("success".equals(conclusion) || "neutral".equals(conclusion) || "skipped".equals(conclusion)) ok++;
            else if ("failure".equals(conclusion) || "cancelled".equals(conclusion) || "timed_out".equals(conclusion) || "action_required".equals(conclusion)) bad++;
            else other++;
        }
        String text = "В загруженной части проверок: " + ok + " без замечаний, " + bad + " с ошибкой, " + other + " без итога.";
        if (total > list.length()) text += " Список обрезан.";
        text += " Это отметки GitHub, не проверка на телефоне.";
        return text;
    }

    private static String labels(JSONObject issue) {
        JSONArray labels = issue == null ? null : issue.optJSONArray("labels");
        if (labels == null || labels.length() == 0) return "Меток нет.";
        StringBuilder b = new StringBuilder("Метки: ");
        for (int i = 0; i < Math.min(labels.length(), 8); i++) {
            JSONObject row = labels.optJSONObject(i);
            String name = row == null ? labels.optString(i) : row.optString("name");
            if (i > 0) b.append(", ");
            b.append(name.isEmpty() ? "без имени" : clip(name, 40));
        }
        return b.toString();
    }

    private static String fileNames(JSONArray files, boolean cut) {
        if (files == null) return HumanReport.NOT_ENOUGH + ": файлы не загружены.";
        if (files.length() == 0) return "В ответе GitHub нет изменённых файлов.";
        StringBuilder b = new StringBuilder("Файлы в ответе: ");
        int n = Math.min(files.length(), 12);
        for (int i = 0; i < n; i++) {
            JSONObject row = files.optJSONObject(i);
            String name = row == null ? "" : row.optString("filename");
            String status = row == null ? "" : row.optString("status");
            if (i > 0) b.append("; ");
            b.append(name.isEmpty() ? "без имени" : clip(name, 80));
            if (!status.isEmpty()) b.append(" (").append(status).append(")");
        }
        if (files.length() > n || cut) b.append("; список неполный");
        return b.toString();
    }

    private static String clip(String value) { return clip(value, 900); }
    private static String clip(String value, int max) {
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= max ? clean : clean.substring(0, max) + "…";
    }

    static String errorForStatus(int status, String rateRemaining) {
        if (status == 401) return "Токен недействителен или истёк (401). Старые данные сохранены.";
        if (status == 429 || (status == 403 && "0".equals(rateRemaining)))
            return "Лимит запросов GitHub исчерпан (429/403). Повторите позже. Старые данные сохранены.";
        if (status == 403) return "Доступ запрещён (403). Проверьте права токена. Старые данные сохранены.";
        if (status == 404) return "Репозиторий не найден или закрыт (404). Старые данные сохранены.";
        return "GitHub вернул ошибку HTTP " + status + ". Старые данные сохранены.";
    }

    static String encodePathPart(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    static String safeMessage(Throwable e) {
        String s = e.getMessage();
        if (s == null) return "неизвестная ошибка";
        s = s.replaceAll("(?i)bearer\\s+\\S+", "bearer [скрыто]");
        s = s.replaceAll("ghp_[A-Za-z0-9]+", "[скрыто]");
        s = s.replaceAll("github_pat_[A-Za-z0-9_]+", "[скрыто]");
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
    private static JSONArray jsonArray(Response r) throws Exception { return new JSONArray(r.body); }
    private static JSONObject jsonObject(Response r) throws Exception { return new JSONObject(r.body); }
    private static final class Response {
        final String body; final String link;
        Response(String body, String link) { this.body = body; this.link = link; }
    }
}
