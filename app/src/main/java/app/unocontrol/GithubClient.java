package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only GitHub access. Never writes, and never persists auth credentials. */
final class GithubClient {
    static final int BRANCH_PAGE_CAP = 5;
    static final int ISSUE_PAGE_CAP = 2;
    static final int PULL_PAGE_CAP = 2;
    static final String CI_UNAVAILABLE = "Результаты автоматических проверок получить не удалось.";

    static final class Result {
        final JSONObject snapshot;
        Result(JSONObject snapshot) { this.snapshot = snapshot; }
    }

    static final class HttpFailure extends Exception {
        final int status;
        HttpFailure(int status, String message) { super(message); this.status = status; }
    }

    private final String token;
    private volatile HttpURLConnection inflight;
    private volatile boolean halted;
    private String boundRepo = "";
    private long boundId;

    GithubClient(String token) { this.token = token == null ? "" : token.trim(); }

    void cancel() {
        halted = true;
        HttpURLConnection c = inflight;
        if (c != null) c.disconnect();
    }

    Result load(String repo) throws Exception { return load(repo, null); }

    Result load(String repo, JSONObject previous) throws Exception {
        if (!repo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}") || repo.contains(".."))
            throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        boundRepo = repo;
        boundId = 0;
        String root = "https://api.github.com/repos/" + repo;
        JSONObject meta = jsonObject(get(root));
        String actual = meta.optString("full_name", "");
        if (!repo.equalsIgnoreCase(actual))
            throw new IllegalStateException("GitHub вернул другой репозиторий.");
        boundRepo = actual;
        boundId = meta.optLong("id", 0);
        String branch = meta.optString("default_branch", "main");

        List<String> warnings = new ArrayList<>();
        String firstError = null;
        boolean anyFresh = false;
        boolean branchesCached = false, issuesCached = false, pullsCached = false;
        String outEtagBranches = "", issuesEtag = "", pullsEtag = "";

        boolean branchesFetched = false;
        boolean branchComplete = false;
        JSONArray branches = new JSONArray();
        try {
            int before = warnings.size();
            boolean[] cached = new boolean[1];
            String[] tag = new String[1];
            String priorTag = etagIfSafe(previous, "branches", "branchesComplete", "branchesEtag");
            branches = pages(root + "/branches?per_page=100", BRANCH_PAGE_CAP, warnings, "ветки", priorTag, cached, tag);
            branchesFetched = true;
            anyFresh = true;
            if (cached[0]) {
                JSONArray kept = previous == null ? null : previous.optJSONArray("branches");
                if (kept != null) {
                    branches = kept;
                    branchComplete = previous.optBoolean("branchesComplete", false);
                    outEtagBranches = tag[0] == null || tag[0].isEmpty() ? previous.optString("branchesEtag", "") : tag[0];
                    branchesCached = true;
                } else {
                    branchComplete = false;
                    branches = new JSONArray();
                    warnings.add("ветки: ответ 304 без сохранённого списка");
                }
            } else {
                branchComplete = !coverageBroken(warnings.subList(before, warnings.size()), "ветки");
                outEtagBranches = tag[0] == null ? "" : tag[0];
            }
        } catch (Exception e) {
            firstError = sourceError("ветки", e);
            warnings.add(firstError);
            if (isHalt(e)) halted = true;
        }

        boolean commitsFetched = false;
        JSONArray commits = new JSONArray();
        if (!halted) {
            try {
                commits = slimCommits(jsonArray(get(root + "/commits?sha=" + encodePathPart(branch) + "&per_page=12")));
                commitsFetched = true;
                anyFresh = true;
            } catch (Exception e) {
                String message = sourceError("история", e);
                if (firstError == null) firstError = message;
                warnings.add(message);
                if (isHalt(e)) halted = true;
            }
        } else warnings.add("история: запрос не отправлялся, лимит GitHub ещё действует");

        JSONArray issues = new JSONArray();
        boolean issuesFetched = false;
        boolean issuesComplete = false;
        if (!halted) {
            try {
                int before = warnings.size();
                String priorTag = etagIfSafe(previous, "issues", "issuesComplete", "issuesEtag");
                boolean[] cached = new boolean[1];
                String[] tag = new String[1];
                issues = pages(root + "/issues?state=all&per_page=100", ISSUE_PAGE_CAP, warnings, "задачи", priorTag, cached, tag);
                issuesFetched = true;
                anyFresh = true;
                if (cached[0]) {
                    JSONArray kept = previous == null ? null : previous.optJSONArray("issues");
                    if (kept != null) {
                        issues = kept;
                        issuesComplete = previous.optBoolean("issuesComplete", false);
                        issuesEtag = tag[0] == null || tag[0].isEmpty() ? previous.optString("issuesEtag", "") : tag[0];
                        issuesCached = true;
                    } else {
                        issuesComplete = false;
                        issues = new JSONArray();
                        warnings.add("задачи: ответ 304 без сохранённого списка");
                    }
                } else {
                    issuesComplete = !coverageBroken(warnings.subList(before, warnings.size()), "задачи");
                    issuesEtag = tag[0] == null ? "" : tag[0];
                }
            } catch (Exception e) {
                String message = sourceError("задачи", e);
                if (firstError == null) firstError = message;
                warnings.add(message);
                if (isHalt(e)) halted = true;
            }
        } else warnings.add("задачи: запрос не отправлялся, лимит GitHub ещё действует");

        JSONArray pulls = new JSONArray();
        boolean pullsFetched = false;
        boolean pullsComplete = false;
        if (!halted) {
            try {
                int before = warnings.size();
                String priorTag = etagIfSafe(previous, "pulls", "pullsComplete", "pullsEtag");
                boolean[] cached = new boolean[1];
                String[] tag = new String[1];
                pulls = pages(root + "/pulls?state=all&per_page=100", PULL_PAGE_CAP, warnings, "предложения", priorTag, cached, tag);
                pullsFetched = true;
                anyFresh = true;
                if (cached[0]) {
                    JSONArray kept = previous == null ? null : previous.optJSONArray("pulls");
                    if (kept != null) {
                        pulls = kept;
                        pullsComplete = previous.optBoolean("pullsComplete", false);
                        pullsEtag = tag[0] == null || tag[0].isEmpty() ? previous.optString("pullsEtag", "") : tag[0];
                        pullsCached = true;
                    } else {
                        pullsComplete = false;
                        pulls = new JSONArray();
                        warnings.add("предложения: ответ 304 без сохранённого списка");
                    }
                } else {
                    pullsComplete = !coverageBroken(warnings.subList(before, warnings.size()), "предложения");
                    pullsEtag = tag[0] == null ? "" : tag[0];
                }
            } catch (Exception e) {
                String message = sourceError("предложения", e);
                if (firstError == null) firstError = message;
                warnings.add(message);
            }
        } else warnings.add("предложения: запрос не отправлялся, лимит GitHub ещё действует");

        if (!anyFresh) {
            throw new IllegalStateException(firstError == null
                    ? "GitHub не ответил. Старые данные сохранены." : firstError);
        }

        JSONObject out = new JSONObject();
        out.put("repo", actual).put("defaultBranch", branch)
                .put("capturedAt", System.currentTimeMillis())
                .put("branches", branchesCached ? branches : (branchesFetched ? slimBranches(branches) : new JSONArray()))
                .put("commits", commits)
                .put("issues", issuesCached ? issues : (issuesFetched ? slimIssues(issues) : new JSONArray()))
                .put("pulls", pullsCached ? pulls : (pullsFetched ? slimPulls(pulls) : new JSONArray()))
                .put("branchesComplete", branchComplete)
                .put("issuesComplete", issuesComplete)
                .put("pullsComplete", pullsComplete)
                .put("commitsComplete", false)
                .put("historyPartial", true)
                .put("branchesFetched", branchesFetched)
                .put("commitsFetched", commitsFetched)
                .put("issuesFetched", issuesFetched)
                .put("pullsFetched", pullsFetched)
                .put("branchesStale", false)
                .put("commitsStale", false)
                .put("issuesStale", false)
                .put("pullsStale", false);
        if (!outEtagBranches.isEmpty()) out.put("branchesEtag", outEtagBranches).put("branchesEtagSinglePage", true);
        if (!issuesEtag.isEmpty()) out.put("issuesEtag", issuesEtag).put("issuesEtagSinglePage", true);
        if (!pullsEtag.isEmpty()) out.put("pullsEtag", pullsEtag).put("pullsEtagSinglePage", true);
        long nowMs = out.optLong("capturedAt", System.currentTimeMillis());
        if (branchesFetched) out.put("branchesFetchedAt", nowMs);
        if (commitsFetched) out.put("commitsFetchedAt", nowMs);
        if (issuesFetched) out.put("issuesFetchedAt", nowMs);
        if (pullsFetched) out.put("pullsFetchedAt", nowMs);
        out.put("branchesCached", branchesCached);
        out.put("issuesCached", issuesCached);
        out.put("pullsCached", pullsCached);
        preserveSources(out, previous);
        out.put("origin", isPartialSnapshot(out) ? "PARTIAL" : "LIVE");
        JSONArray warn = new JSONArray();
        for (String w : warnings) warn.put(w);
        JSONArray extra = out.optJSONArray("warnings");
        if (extra != null) for (int i = 0; i < extra.length(); i++) warn.put(extra.optString(i));
        out.put("warnings", warn);
        return new Result(out);
    }

    /** A successful request for recent commits does not imply full history coverage. */
    static boolean isPartialSnapshot(JSONObject snapshot) {
        return "PARTIAL".equals(snapshot.optString("origin"))
                || !snapshot.optBoolean("branchesComplete", false)
                || !snapshot.optBoolean("issuesComplete", false)
                || !snapshot.optBoolean("pullsComplete", false)
                || !snapshot.optBoolean("commitsFetched", false)
                || snapshot.optBoolean("branchesStale", false)
                || snapshot.optBoolean("issuesStale", false)
                || snapshot.optBoolean("pullsStale", false)
                || snapshot.optBoolean("commitsStale", false);
    }

    /** Keeps the last good list when this refresh did not fetch that source. */
    static void preserveSources(JSONObject fresh, JSONObject previous) throws Exception {
        if (fresh == null || previous == null) return;
        if (!fresh.optString("repo").equalsIgnoreCase(previous.optString("repo"))) return;
        if (SnapshotCodec.isImported(previous)) return;
        keep(fresh, previous, "branches", "branchesFetched", "branchesComplete", "branchesStale", "ветки");
        keep(fresh, previous, "commits", "commitsFetched", "commitsComplete", "commitsStale", "история");
        keep(fresh, previous, "issues", "issuesFetched", "issuesComplete", "issuesStale", "задачи");
        keep(fresh, previous, "pulls", "pullsFetched", "pullsComplete", "pullsStale", "предложения");
    }

    private static void keep(JSONObject fresh, JSONObject previous, String arrayKey, String fetchedKey,
                             String completeKey, String staleKey, String label) throws Exception {
        if (fresh.optBoolean(fetchedKey, true)) return;
        JSONArray old = previous.optJSONArray(arrayKey);
        if (old == null) return;
        fresh.put(arrayKey, old);
        fresh.put(staleKey, true);
        fresh.put(completeKey, false);
        JSONArray warnings = fresh.optJSONArray("warnings");
        if (warnings == null) {
            warnings = new JSONArray();
            fresh.put("warnings", warnings);
        }
        warnings.put(label + ": показан прошлый список. Свежие данные этого раздела не получены.");
    }

    static boolean coverageBroken(List<String> warnings, String label) {
        if (warnings == null) return false;
        String prefix = label + ":";
        for (String w : warnings) if (w != null && w.startsWith(prefix)) return true;
        return false;
    }

    private static boolean isHalt(Exception e) {
        if (e instanceof HttpFailure && ((HttpFailure) e).status == 429) return true;
        String m = e.getMessage() == null ? "" : e.getMessage();
        return m.contains("Лимит запросов");
    }

    private static String sourceError(String label, Exception e) {
        return label + ": " + safeMessage(e);
    }

    /** ETag is trustworthy only for one complete page. A later page can change while page 1 stays the same. */
    private static String etagIfSafe(JSONObject previous, String array, String completeKey, String etagKey) {
        if (previous == null || SnapshotCodec.isImported(previous)) return null;
        if (!previous.optBoolean(completeKey, false)) return null;
        // Older snapshots may have cached the ETag of page one of a multi-page list.
        // The explicit single-page marker is mandatory before conditional requests.
        if (!previous.optBoolean(array + "EtagSinglePage", false)) return null;
        JSONArray list = previous.optJSONArray(array);
        if (list == null || list.length() >= 100) return null;
        String tag = previous.optString(etagKey, "");
        return tag.isEmpty() ? null : tag;
    }

    /** Page-1 ETag is the whole list only when GitHub sent no next page. A short list after dropping pull requests is not enough. */
    static String etagIfCompletePage(String etag, String link) {
        if (etag == null || etag.isEmpty() || nextLink(link) != null) return "";
        return etag;
    }

    private JSONArray pages(String start, int max, List<String> warnings, String label) throws Exception {
        return pages(start, max, warnings, label, null, null, null);
    }

    private JSONArray pages(String start, int max, List<String> warnings, String label,
                            String etag, boolean[] cached, String[] tag) throws Exception {
        JSONArray all = new JSONArray();
        String url = start;
        for (int page = 0; page < max; page++) {
            Response response;
            try { response = get(url, page == 0 ? etag : null); }
            catch (Exception e) {
                if (page == 0) throw e;
                warnings.add(label + ": загрузка прервана");
                return all;
            }
            if (page == 0 && tag != null) tag[0] = etagIfCompletePage(response.etag, response.link);
            if (page == 0 && response.notModified) {
                if (cached != null) cached[0] = true;
                return all;
            }
            JSONArray current = jsonArray(response);
            for (int j = 0; j < current.length(); j++) all.put(current.get(j));
            String next = nextLink(response.link);
            if (next == null) return all;
            adoptRepositoryId(next);
            if (!safeContinuation(next, boundRepo, boundId)) {
                warnings.add(label + ": небезопасная ссылка продолжения отклонена");
                return all;
            }
            url = next;
        }
        warnings.add(label + ": предел " + max + " страниц");
        return all;
    }

    static boolean safeContinuation(String next, String repo, long repoId) {
        if (next == null) return false;
        int q = next.indexOf('?');
        String path = q >= 0 ? next.substring(0, q) : next;
        String lower = path.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://api.github.com/")) return false;
        String repoLower = repo == null ? "" : repo.toLowerCase(Locale.ROOT);
        if (!repoLower.isEmpty()) {
            String base = "https://api.github.com/repos/" + repoLower;
            if (lower.equals(base) || lower.startsWith(base + "/")) return true;
        }
        if (repoId > 0) {
            String base = "https://api.github.com/repositories/" + repoId;
            if (lower.equals(base) || lower.startsWith(base + "/")) return true;
        }
        return false;
    }

    private void adoptRepositoryId(String next) {
        if (boundId != 0 || next == null) return;
        String prefix = "https://api.github.com/repositories/";
        String lower = next.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(prefix)) return;
        int start = prefix.length();
        int end = start;
        while (end < lower.length() && Character.isDigit(lower.charAt(end))) end++;
        if (end == start) return;
        try { boundId = Long.parseLong(lower.substring(start, end)); }
        catch (NumberFormatException ignored) { boundId = 0; }
    }

    static String nextLink(String link) {
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
            String author = "";
            if (commit != null && commit.optJSONObject("committer") != null)
                date = commit.optJSONObject("committer").optString("date", "");
            JSONObject account = x.optJSONObject("author");
            if (account != null) author = account.optString("login", "");
            if (author.isEmpty() && commit != null && commit.optJSONObject("author") != null)
                author = commit.optJSONObject("author").optString("name", "");
            if (author.length() > 80) author = author.substring(0, 80);
            out.put(new JSONObject().put("sha", x.optString("sha")).put("message", message)
                    .put("date", date).put("author", author));
        }
        return out;
    }

    private static String labelNames(JSONArray labels) {
        if (labels == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < labels.length() && b.length() < 120; i++) {
            JSONObject row = labels.optJSONObject(i);
            String name = row == null ? labels.optString(i) : row.optString("name");
            if (name == null) continue;
            name = name.replace('\n', ' ').trim();
            if (name.isEmpty()) continue;
            if (name.length() > 40) name = name.substring(0, 40);
            if (b.length() > 0) b.append(", ");
            b.append(name);
        }
        return b.toString();
    }

    private static JSONArray slimIssues(JSONArray raw) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject x = raw.optJSONObject(i);
            if (x == null || x.has("pull_request")) continue;
            JSONObject row = new JSONObject().put("number", x.optInt("number"))
                    .put("title", x.optString("title"))
                    .put("state", x.optString("state"))
                    .put("updated_at", cleanStamp(x.optString("updated_at")));
            String labels = labelNames(x.optJSONArray("labels"));
            if (!labels.isEmpty()) row.put("labels", labels);
            out.put(row);
        }
        return out;
    }

    private static JSONArray slimPulls(JSONArray raw) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject x = raw.optJSONObject(i);
            if (x == null) continue;
            String mergedAt = x.optString("merged_at", "");
            boolean merged = x.optBoolean("merged", false)
                    || (mergedAt != null && !mergedAt.isEmpty() && !"null".equals(mergedAt));
            out.put(new JSONObject().put("number", x.optInt("number"))
                    .put("title", x.optString("title"))
                    .put("state", x.optString("state"))
                    .put("updated_at", cleanStamp(x.optString("updated_at")))
                    .put("merged", merged)
                    .put("merged_at", cleanStamp(mergedAt)));
        }
        return out;
    }

    static boolean mayRetry(int attempt, int status, Throwable error) {
        if (attempt > 0) return false;
        if (status == 401 || status == 403 || status == 404 || status == 429) return false;
        if (status == 408 || status == 502 || status == 503 || status == 504) return true;
        return status == 0 && retryableFailure(error);
    }

    static boolean retryableFailure(Throwable e) {
        String s = e == null || e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        return e instanceof java.net.SocketTimeoutException || s.contains("timed out") || s.contains("timeout");
    }

    private Response get(String address) throws Exception { return get(address, null); }

    private Response get(String address, String etag) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) {
                try { Thread.sleep(600L); }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
            try {
                return getOnce(address, etag);
            } catch (HttpFailure e) {
                last = e;
                if (e.status == 429 || (e.getMessage() != null && e.getMessage().contains("Лимит запросов"))) halted = true;
                if (mayRetry(attempt, e.status, e)) continue;
                throw e;
            } catch (Exception e) {
                last = e;
                if (mayRetry(attempt, 0, e)) continue;
                throw e;
            }
        }
        throw last == null ? new IllegalStateException("GitHub не ответил. Старые данные сохранены.") : last;
    }

    private Response getOnce(String address, String etag) throws Exception {
        if (halted) throw new HttpFailure(429, "Лимит запросов GitHub исчерпан. Повторите позже. Старые данные сохранены.");
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Запрос отменён.");
        if (!safeContinuation(address, boundRepo, boundId))
            throw new SecurityException("Недопустимый адрес GitHub API.");
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(14000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setRequestProperty("User-Agent", "GitTeller/1.0.21-dev");
        if (etag != null && !etag.isEmpty()) c.setRequestProperty("If-None-Match", etag);
        if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        inflight = c;
        try {
            int status = c.getResponseCode();
            if (status == 304) return new Response("", null, c.getHeaderField("ETag"), true);
            if (status != 200) {
                String apiMessage = apiMessage(c);
                throw new HttpFailure(status, errorForStatus(status,
                        c.getHeaderField("X-RateLimit-Remaining"),
                        c.getHeaderField("Retry-After"),
                        c.getHeaderField("X-RateLimit-Reset"),
                        apiMessage));
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
            return new Response(data.toString(StandardCharsets.UTF_8.name()), link, c.getHeaderField("ETag"), false);
        } finally {
            if (inflight == c) inflight = null;
            c.disconnect();
        }
    }

    private static String apiMessage(HttpURLConnection c) {
        try {
            InputStream err = c.getErrorStream();
            if (err == null) return "";
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            byte[] buf = new byte[512]; int n; int total = 0;
            while ((n = err.read(buf)) != -1 && total < 2000) {
                data.write(buf, 0, n);
                total += n;
            }
            String body = data.toString(StandardCharsets.UTF_8.name());
            try {
                return new JSONObject(body).optString("message", "");
            } catch (Exception ignored) {
                return body.length() > 180 ? body.substring(0, 180) : body;
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    static final class Extra {
        final String text;
        Extra(String text) { this.text = text; }
    }

    Extra readIssue(String repo, int number) throws Exception {
        bind(repo);
        JSONObject issue = jsonObject(get(root(repo) + "/issues/" + number));
        return formatIssue(issue);
    }

    Extra readPull(String repo, int number) throws Exception {
        bind(repo);
        String base = root(repo) + "/pulls/" + number;
        JSONObject pull = jsonObject(get(base));
        JSONArray files = new JSONArray();
        boolean filesCut = false;
        String filesNote = null;
        try {
            Response page = get(base + "/files?per_page=100");
            files = jsonArray(page);
            String next = nextLink(page.link);
            adoptRepositoryId(next);
            if (next != null && safeContinuation(next, boundRepo, boundId)) {
                Response more = get(next);
                JSONArray extra = jsonArray(more);
                for (int i = 0; i < extra.length(); i++) files.put(extra.get(i));
                filesCut = nextLink(more.link) != null;
            }
        } catch (Exception e) {
            filesNote = HumanReport.NOT_ENOUGH + ": список файлов не прочитан (" + safeMessage(e) + ").";
        }
        String sha = pull.optJSONObject("head") == null ? "" : pull.optJSONObject("head").optString("sha");
        String checks = loadChecks(repo, sha);
        return formatPull(pull, files, filesCut, filesNote, checks);
    }

    Extra readCommit(String repo, String sha) throws Exception {
        bind(repo);
        if (sha == null || !sha.matches("[0-9a-fA-F]{40}"))
            throw new IllegalArgumentException("Нет полного SHA для запроса файлов.");
        Response commitResponse = get(root(repo) + "/commits/" + sha + "?per_page=100");
        JSONObject commit = jsonObject(commitResponse);
        boolean filesCut = nextLink(commitResponse.link) != null;
        String next = nextLink(commitResponse.link);
        adoptRepositoryId(next);
        if (next != null && safeContinuation(next, boundRepo, boundId)) {
            try {
                Response more = get(next);
                JSONObject extra = jsonObject(more);
                JSONArray files = commit.optJSONArray("files");
                if (files == null) {
                    files = new JSONArray();
                    commit.put("files", files);
                }
                JSONArray moreFiles = extra.optJSONArray("files");
                if (moreFiles != null) for (int i = 0; i < moreFiles.length(); i++) files.put(moreFiles.get(i));
                filesCut = nextLink(more.link) != null;
            } catch (Exception ignored) {
                filesCut = true;
            }
        }
        return formatCommit(commit, loadChecks(repo, sha), filesCut);
    }

    JSONArray readBranchCommits(String repo, String branch) throws Exception {
        bind(repo);
        if (branch == null || branch.isEmpty() || branch.contains("..") || branch.indexOf(' ') >= 0 || branch.indexOf('\\') >= 0)
            throw new IllegalArgumentException("Некорректное направление.");
        if (branch.length() > 200) throw new IllegalArgumentException("Некорректное направление.");
        return slimCommits(jsonArray(get(root(repo) + "/commits?sha=" + encodePathPart(branch) + "&per_page=8")));
    }

    private String loadChecks(String repo, String sha) {
        if (sha == null || !sha.matches("[0-9a-fA-F]{40}")) return CI_UNAVAILABLE;
        String checks = null;
        String statuses = null;
        String actions = null;
        try {
            checks = formatChecks(jsonObject(get(root(repo) + "/commits/" + sha + "/check-runs?per_page=20")));
        } catch (Exception ignored) {
            checks = null;
        }
        boolean checksUseful = checks != null && !checks.contains("Проверок GitHub в ответе нет");
        if (!checksUseful && !halted) {
            try {
                statuses = formatStatuses(jsonObject(get(root(repo) + "/commits/" + sha + "/status")));
            } catch (Exception ignored) {
                statuses = null;
            }
        }
        boolean statusUseful = statuses != null && !statuses.contains("получить не удалось");
        if (!checksUseful && !statusUseful && !halted) {
            try {
                actions = formatActions(jsonObject(get(root(repo) + "/actions/runs?head_sha=" + sha + "&per_page=5")));
            } catch (Exception ignored) {
                actions = null;
            }
        }
        return describeCi(checks, statuses, actions);
    }

    static String describeCi(String checks, String statuses, String actions) {
        boolean checksUseful = checks != null && !checks.contains("Проверок GitHub в ответе нет");
        boolean statusUseful = statuses != null && !statuses.contains("получить не удалось");
        boolean actionUseful = actions != null && !actions.contains("в ответе нет");
        if (!checksUseful && !statusUseful && !actionUseful) {
            if (checks == null && statuses == null && actions == null) return CI_UNAVAILABLE;
            return "Автоматических проверок для этого коммита в доступных источниках нет. Это не успех. " + CI_UNAVAILABLE;
        }
        StringBuilder b = new StringBuilder();
        if (checksUseful) b.append(checks).append("\n");
        else if (checks != null && (statusUseful || actionUseful)) b.append(checks).append("\n");
        if (statusUseful) b.append(statuses).append("\n");
        if (actionUseful) b.append(actions).append("\n");
        b.append("Пропущенные, нейтральные и отсутствующие проверки не означают успех.");
        return b.toString().trim();
    }

    private void bind(String repo) {
        boundRepo = repo == null ? "" : repo;
        boundId = 0;
    }

    private static String root(String repo) {
        if (repo == null || !repo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}") || repo.contains(".."))
            throw new IllegalArgumentException("Репозиторий укажите в формате владелец/проект.");
        return "https://api.github.com/repos/" + repo;
    }

    static Extra formatIssue(JSONObject issue) {
        StringBuilder b = new StringBuilder();
        String body = issue == null ? "" : issue.optString("body").trim();
        b.append("Что заявил автор. ").append(body.isEmpty() ? "Описания в GitHub нет." : clip(body)).append("\n");
        b.append("Что подтверждает GitHub. ").append(labels(issue)).append("\n");
        b.append("Чего установить нельзя. ").append(HumanReport.NO_CHECK).append(". Текст записи не доказывает, что работа выполнена.");
        return new Extra(b.toString());
    }

    static Extra formatPull(JSONObject pull, JSONArray files, boolean filesCut, String filesNote, String checks) {
        StringBuilder b = new StringBuilder();
        boolean merged = pull != null && (pull.optBoolean("merged", false)
                || (!pull.optString("merged_at").isEmpty() && !"null".equals(pull.optString("merged_at"))));
        String mergedAt = pull == null ? "" : pull.optString("merged_at");
        b.append("Что подтверждает GitHub. ");
        if (merged) b.append("GitHub отмечает, что предложение слито");
        else b.append("GitHub не сообщает, что предложение слито");
        if (merged && mergedAt != null && !mergedAt.isEmpty() && !"null".equals(mergedAt))
            b.append(" (").append(HumanReport.humanDate(mergedAt)).append(")");
        b.append(". Закрытие без этого признака не означает внесение в основную ветку.\n");
        String body = pull == null ? "" : pull.optString("body").trim();
        b.append("Что заявил автор. ").append(body.isEmpty() ? "Описания нет." : clip(body)).append("\n");
        if (filesNote != null) b.append(filesNote).append("\n");
        else b.append(fileNames(files, filesCut)).append("\n");
        b.append(checks == null ? HumanReport.NOT_ENOUGH + ": проверки не загружены." : checks).append("\n");
        b.append("Чего установить нельзя. ").append(HumanReport.NO_CHECK).append(". Даже успешная отметка GitHub не заменяет проверку на устройстве.");
        return new Extra(b.toString());
    }

    static Extra formatCommit(JSONObject commit, String checks, boolean filesCut) {
        StringBuilder b = new StringBuilder();
        JSONArray files = commit == null ? null : commit.optJSONArray("files");
        String message = "";
        if (commit != null) {
            JSONObject inner = commit.optJSONObject("commit");
            if (inner != null) message = inner.optString("message", "");
            if (message.isEmpty()) message = commit.optString("message", "");
        }
        b.append("Что заявил автор. ").append(HumanReport.explainClaim(message, files)).append("\n");
        b.append("Что подтверждает GitHub. ").append(fileNames(files, filesCut)).append("\n");
        b.append(checks == null ? HumanReport.NOT_ENOUGH + ": проверки не загружены." : checks).append("\n");
        b.append("Чего установить нельзя. ").append(HumanReport.NO_CHECK).append(". Список файлов не объясняет, работает ли программа.");
        return new Extra(b.toString());
    }

    static String formatStatuses(JSONObject status) {
        JSONArray list = status == null ? null : status.optJSONArray("statuses");
        if (list == null || list.length() == 0) return CI_UNAVAILABLE;
        int ok = 0, bad = 0, pending = 0, other = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            String state = row == null ? "" : row.optString("state");
            if ("success".equals(state)) ok++;
            else if ("failure".equals(state) || "error".equals(state)) bad++;
            else if ("pending".equals(state)) pending++;
            else other++;
        }
        return "Статусы коммита: " + ok + " успешных, " + bad + " с ошибкой, " + pending
                + " ещё идут, " + other + " прочих. Пропущенное и нейтральное не считаются успехом. Это не проверка на телефоне.";
    }

    static String formatActions(JSONObject payload) {
        JSONArray list = payload == null ? null : payload.optJSONArray("workflow_runs");
        if (list == null || list.length() == 0)
            return "Запусков GitHub Actions для этого коммита в ответе нет. Это не успех.";
        int success = 0, bad = 0, pending = 0, skipped = 0, other = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            String conclusion = row == null ? "" : row.optString("conclusion");
            String status = row == null ? "" : row.optString("status");
            if ("success".equals(conclusion)) success++;
            else if ("failure".equals(conclusion) || "timed_out".equals(conclusion)
                    || "cancelled".equals(conclusion) || "startup_failure".equals(conclusion)) bad++;
            else if ("skipped".equals(conclusion) || "neutral".equals(conclusion)) skipped++;
            else if (!"completed".equals(status)) pending++;
            else other++;
        }
        return "GitHub Actions для этого коммита: " + success + " с результатом success, " + bad
                + " с ошибкой, " + pending + " ещё идут, " + skipped
                + " пропущенных или нейтральных, " + other
                + " прочих. Пропуск и нейтральный итог не считаются успехом. Это не проверка на телефоне.";
    }

    static String formatChecks(JSONObject runs) {
        JSONArray list = runs == null ? null : runs.optJSONArray("check_runs");
        int total = runs == null ? 0 : runs.optInt("total_count", list == null ? 0 : list.length());
        if (list == null || list.length() == 0) return "Проверок GitHub в ответе нет. " + HumanReport.NO_CHECK + ".";
        int success = 0, skippedOrNeutral = 0, bad = 0, other = 0, cancelled = 0, timedOut = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            String conclusion = row == null ? "" : row.optString("conclusion");
            if ("success".equals(conclusion)) success++;
            else if ("neutral".equals(conclusion) || "skipped".equals(conclusion)) skippedOrNeutral++;
            else if ("cancelled".equals(conclusion)) { bad++; cancelled++; }
            else if ("timed_out".equals(conclusion)) { bad++; timedOut++; }
            else if ("failure".equals(conclusion) || "action_required".equals(conclusion)) bad++;
            else other++;
        }
        String text = "В загруженной части GitHub-проверок: " + success + " с результатом success, "
                + skippedOrNeutral + " пропущенных или нейтральных, " + bad
                + " с ошибкой, " + other + " без итога.";
        if (total > list.length()) text += " Список обрезан.";
        if (cancelled > 0) text += " Есть отменённые проверки. Это не успех.";
        if (timedOut > 0) text += " Есть проверки, прерванные по времени. Это не успех.";
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
        return errorForStatus(status, rateRemaining, null, null, null);
    }

    static String errorForStatus(int status, String rateRemaining, String retryAfter) {
        return errorForStatus(status, rateRemaining, retryAfter, null, null);
    }

    static String errorForStatus(int status, String rateRemaining, String retryAfter, String resetEpoch, String apiMessage) {
        if (status == 401) return "Токен недействителен или истёк (401). Старые данные сохранены. Токен на устройстве не удалён.";
        boolean rate = status == 429
                || (status == 403 && "0".equals(rateRemaining))
                || (status == 403 && retryAfter != null && retryAfter.matches("\\d+"))
                || (status == 403 && looksLikeRate(apiMessage));
        if (rate) {
            String wait;
            if (retryAfter != null && retryAfter.matches("\\d+"))
                wait = " Не повторяйте запрос раньше чем через " + retryAfter + " с.";
            else if (resetEpoch != null && resetEpoch.matches("\\d{9,12}"))
                wait = " Повторите после " + HumanReport.humanDate(Long.parseLong(resetEpoch) * 1000L) + ".";
            else wait = " Повторите позже.";
            String code = status == 429 ? "429" : "403";
            return "Лимит запросов GitHub исчерпан (" + code + ")." + wait + " Старые данные сохранены.";
        }
        if (status == 403) return "Доступ ограничен (403): не хватает прав или так закрыта эта точка API. Это не автоматически неверный токен. Старые данные сохранены.";
        if (status == 404) return "Проект не найден или к нему нет доступа (404). Старые данные сохранены.";
        String extra = apiMessage == null || apiMessage.isEmpty() ? "" : " " + clip(safeMessage(new IllegalStateException(apiMessage)), 140);
        return "GitHub вернул ошибку HTTP " + status + "." + extra + " Старые данные сохранены.";
    }

    private static boolean looksLikeRate(String apiMessage) {
        if (apiMessage == null) return false;
        String m = apiMessage.toLowerCase(Locale.ROOT);
        return m.contains("rate limit") || m.contains("secondary rate") || m.contains("abuse detection");
    }

    static String encodePathPart(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    static String safeMessage(Throwable e) {
        String s = e == null ? null : e.getMessage();
        if (s == null) return "неизвестная ошибка. Старые данные сохранены.";
        String low = s.toLowerCase(Locale.ROOT);
        if (low.contains("timed out") || low.contains("timeout"))
            return "GitHub не ответил вовремя. Старые данные сохранены. Можно повторить загрузку.";
        if (low.contains("unable to resolve host") || low.contains("network is unreachable")
                || low.contains("failed to connect") || low.contains("no address associated"))
            return "Нет соединения с GitHub. Старые данные сохранены. Можно повторить загрузку.";
        if (low.contains("socket closed") || low.contains("connection reset") || low.contains("connection abort")
                || low.contains("request canceled") || low.contains("запрос отменён"))
            return "Запрос отменён или соединение прервалось. Старые данные сохранены.";
        s = s.replaceAll("(?i)bearer\\s+\\S+", "bearer [скрыто]");
        s = s.replaceAll("ghp_[A-Za-z0-9]+", "[скрыто]");
        s = s.replaceAll("github_pat_[A-Za-z0-9_]+", "[скрыто]");
        return s.length() > 240 ? s.substring(0, 240) : s;
    }

    private static JSONArray jsonArray(Response r) throws Exception { return new JSONArray(r.body); }
    private static JSONObject jsonObject(Response r) throws Exception { return new JSONObject(r.body); }
    private static final class Response {
        final String body; final String link; final String etag; final boolean notModified;
        Response(String body, String link) { this(body, link, null, false); }
        Response(String body, String link, String etag, boolean notModified) {
            this.body = body; this.link = link; this.etag = etag; this.notModified = notModified;
        }
    }

    private static String cleanStamp(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) return "";
        return value.length() > 40 ? value.substring(0, 40) : value;
    }

    static String explainCompareStatus(String status, int ahead, int behind, boolean cut) {
        String text;
        if ("ahead".equals(status))
            text = "Появились новые записи (" + ahead + "). Это не прогресс продукта и не проверка.";
        else if ("behind".equals(status))
            text = "История откатилась (" + behind + " позади). Это не продвижение вперёд.";
        else if ("diverged".equals(status))
            text = "История разошлась: впереди " + ahead + ", позади " + behind + ". Нельзя считать это обычным продвижением.";
        else if ("identical".equals(status))
            text = "Указатели совпадают. Новых записей между ними нет.";
        else text = HumanReport.NOT_ENOUGH + ": направление сравнения неизвестно.";
        if (cut) text += " Список сравнения неполный.";
        if (!"identical".equals(status)) text += " " + HumanReport.NO_CHECK + ".";
        return text;
    }

    String readCompare(String repo, String base, String head) {
        if (base == null || head == null || !base.matches("[0-9a-fA-F]{40}") || !head.matches("[0-9a-fA-F]{40}"))
            return HumanReport.NOT_ENOUGH + ": нет двух полных указателей для сравнения.";
        if (base.equalsIgnoreCase(head)) return explainCompareStatus("identical", 0, 0, false);
        try {
            bind(repo);
            String address = root(repo) + "/compare/" + base + "..." + head;
            Response response = get(address);
            boolean cut = nextLink(response.link) != null;
            JSONObject body = jsonObject(response);
            int ahead = body.optInt("ahead_by", 0);
            int behind = body.optInt("behind_by", 0);
            String status = body.optString("status");
            JSONArray commits = body.optJSONArray("commits");
            JSONArray files = body.optJSONArray("files");
            if (commits != null && commits.length() >= 250) cut = true;
            if (files != null && files.length() >= 300) cut = true;
            String text = explainCompareStatus(status, ahead, behind, cut);
            if (files != null && files.length() > 0) {
                StringBuilder names = new StringBuilder(" GitHub показывает файлы: ");
                int n = Math.min(4, files.length());
                for (int i = 0; i < n; i++) {
                    JSONObject row = files.optJSONObject(i);
                    if (i > 0) names.append(", ");
                    names.append(row == null ? "без имени" : row.optString("filename"));
                }
                names.append(".");
                text += names.toString();
            }
            return text;
        } catch (Exception e) {
            return compareFailure(e);
        }
    }

    static String compareFailure(Exception e) {
        return HumanReport.NOT_ENOUGH + ". Сравнение веток не получено (" + safeMessage(e) + ").";
    }
}
