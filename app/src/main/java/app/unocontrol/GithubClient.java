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
    GithubClient(String token) { this.token = token == null ? "" : token.trim(); }

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
        try { issues = pages(root + "/issues?state=all&per_page=100", 4, warnings, "задачи"); }
        catch (Exception e) { issues = new JSONArray(); warnings.add("Задачи недоступны: " + safeMessage(e)); }
        JSONArray pulls;
        try { pulls = pages(root + "/pulls?state=all&per_page=100", 4, warnings, "запросы изменений"); }
        catch (Exception e) { pulls = new JSONArray(); warnings.add("Запросы изменений недоступны: " + safeMessage(e)); }

        JSONObject out = new JSONObject();
        out.put("repo", actual).put("defaultBranch", branch)
                .put("capturedAt", System.currentTimeMillis())
                .put("branches", slimBranches(branches))
                .put("commits", slimCommits(commits))
                .put("issues", slimIssues(issues))
                .put("pulls", slimIssues(pulls))
                .put("branchesComplete", branchComplete);
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
        c.setRequestProperty("User-Agent", "GitTeller/1.0.2");
        if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
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
        } finally { c.disconnect(); }
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
        // Never include a URL query or Authorization header in UI error messages.
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
    private static JSONArray jsonArray(Response r) throws Exception { return new JSONArray(r.body); }
    private static JSONObject jsonObject(Response r) throws Exception { return new JSONObject(r.body); }
    private static final class Response {
        final String body; final String link;
        Response(String body, String link) { this.body = body; this.link = link; }
    }
}
