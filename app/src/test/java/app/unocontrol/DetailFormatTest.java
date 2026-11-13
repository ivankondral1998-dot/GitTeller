package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DetailFormatTest {
    @Test public void issueTextIsNotAPassingTest() throws Exception {
        JSONObject issue = new JSONObject().put("body", "Нужно починить вход").put("labels",
                new JSONArray().put(new JSONObject().put("name", "bug")));
        String text = GithubClient.formatIssue(issue).text;
        assertTrue(text.contains("Нужно починить вход"));
        assertTrue(text.contains("bug"));
        assertTrue(text.contains("Проверка работоспособности отсутствует"));
        assertFalse(text.contains("PASS"));
    }

    @Test public void closedPullWithoutMergedFlagIsNotMerged() throws Exception {
        JSONObject pull = new JSONObject().put("merged", false).put("state", "closed").put("body", "");
        String text = GithubClient.formatPull(pull, new JSONArray(), false, null,
                "Проверок GitHub в ответе нет. Проверка работоспособности отсутствует.").text;
        assertTrue(text.contains("не сообщает"));
        assertFalse(text.toLowerCase().contains("слито ("));
        assertTrue(text.contains("нет изменённых файлов") || text.contains("Файлы"));
    }

    @Test public void failedCheckIsNotHidden() throws Exception {
        JSONObject runs = new JSONObject().put("total_count", 1).put("check_runs", new JSONArray()
                .put(new JSONObject().put("name", "build").put("conclusion", "failure").put("status", "completed")));
        String text = GithubClient.formatChecks(runs);
        assertTrue(text.contains("1 с ошибкой"));
        assertFalse(text.contains("PASS"));
    }
}
