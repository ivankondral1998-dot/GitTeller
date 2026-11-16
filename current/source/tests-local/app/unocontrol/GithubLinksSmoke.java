package app.unocontrol;

public final class GithubLinksSmoke {
    private static int count;
    private static void eq(String expected, String actual){
        if(expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        count++;
    }
    public static void main(String[] args){
        String repo="ivankondral1998-dot/UNO-android-AI-Agent";
        String issue= "https://github.com/" + repo + "/issues/6";
        eq(issue, GithubLinks.recordUrl(repo,"issues",6));
        eq("https://github.com/"+repo+"/pull/7",GithubLinks.recordUrl(repo,"pull",7));
        eq(null,GithubLinks.recordUrl(repo,"issues",0));
        eq(null,GithubLinks.recordUrl(repo,"issue",6));
        eq(null,GithubLinks.recordUrl("bad/../repo","issues",6));
        eq(issue,GithubLinks.validatedRepoUrl(repo,issue));
        eq(null,GithubLinks.validatedRepoUrl(repo,issue+"?q=author%3Awrong"));
        eq(null,GithubLinks.validatedRepoUrl(repo,issue+"#fake"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com.evil.test/"+repo+"/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"http://github.com/"+repo+"/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com/other/repo/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com/"+repo+"/issues?q=author%3Abad"));
        eq("https://github.com/"+repo+"/tree/research/uno-train-001",
            GithubLinks.validatedRepoUrl(repo,"https://github.com/"+repo+"/tree/research/uno-train-001"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com/"+repo+"/tree/../settings"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"javascript:alert(1)"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com/"+repo+"/tree/%2e%2e/%2e%2e/other/repo"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://user:pass@github.com/"+repo+"/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl(repo,"https://github.com:443/"+repo+"/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl(repo,issue+"/"));
        eq(issue,GithubLinks.validatedRepoUrl(repo,"HTTPS://GitHub.com/"+repo+"/issues/6"));
        eq(null,GithubLinks.validatedRepoUrl("other/repo",issue));
        System.out.println("GithubLinksSmoke PASS="+count);
    }
}
