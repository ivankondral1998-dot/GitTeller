package app.unocontrol;

import java.net.URI;

/** Links open only for the selected repository. Arbitrary or imported URLs stay closed. */
final class GithubLinks {
    private GithubLinks() {}

    static String recordUrl(String repo, String type, int number) {
        if (!validRepo(repo) || number <= 0) return null;
        if (!"issues".equals(type) && !"pull".equals(type)) return null;
        return "https://github.com/" + repo + "/" + type + "/" + number;
    }

    /**
     * Accept only https://github.com/{selectedRepo}/issues|pull/{n} or /tree/{branch}.
     * Returns a canonical URL, never the raw candidate. Query, fragment, user-info,
     * port, percent-encoding and ".." segments are rejected so a browser cannot be
     * sent to a GitHub search filter or another repository.
     */
    static String validatedRepoUrl(String repo, String candidate) {
        if (!validRepo(repo) || candidate == null) return null;
        if (hasForbiddenChars(candidate)) return null;
        try {
            URI u = new URI(candidate);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo() != null || u.getPort() != -1
                    || u.getRawQuery() != null || u.getRawFragment() != null) return null;
            String host = u.getHost();
            if (host == null || !"github.com".equalsIgnoreCase(host)) return null;
            String raw = u.getRawPath();
            if (raw == null || raw.indexOf('%') >= 0 || raw.indexOf('\\') >= 0) return null;
            String prefix = "/" + repo + "/";
            if (!raw.startsWith(prefix)) return null;
            String tail = raw.substring(prefix.length());
            if (tail.matches("(?:issues|pull)/[1-9][0-9]*"))
                return "https://github.com/" + repo + "/" + tail;
            if (tail.startsWith("tree/")) {
                String branch = tail.substring("tree/".length());
                if (!safeBranch(branch)) return null;
                return "https://github.com/" + repo + "/tree/" + branch;
            }
        } catch (Exception ignored) { /* Never open malformed or untrusted URLs. */ }
        return null;
    }

    private static boolean hasForbiddenChars(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x20 || c == 0x7f || c == '\\') return true;
        }
        return false;
    }

    private static boolean safeBranch(String branch) {
        if (branch.isEmpty() || branch.startsWith("/") || branch.endsWith("/")
                || branch.contains("//") || branch.contains("..")
                || branch.indexOf(' ') >= 0 || branch.indexOf('\\') >= 0
                || branch.indexOf('?') >= 0 || branch.indexOf('#') >= 0
                || branch.indexOf('%') >= 0)
            return false;
        int start = 0;
        for (int i = 0; i <= branch.length(); i++) {
            if (i == branch.length() || branch.charAt(i) == '/') {
                String segment = branch.substring(start, i);
                if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return false;
                start = i + 1;
            }
        }
        return true;
    }

    private static boolean validRepo(String repo) {
        return repo != null && repo.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")
                && !repo.contains("..");
    }
}
