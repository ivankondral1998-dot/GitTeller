package app.unocontrol;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshCancellationTest {
    @Test public void cancelledClientDoesNotStartARequestOrReportRateLimit() throws Exception {
        GithubClient client = new GithubClient("");
        client.cancel();
        try {
            client.load("octocat/Hello-World");
            fail("Cancelled request must not return a snapshot");
        } catch (InterruptedException expected) {
            assertTrue(GithubClient.safeMessage(expected).contains("отменён"));
        }
    }

    @Test public void stoppedQuietCheckCannotAttachNewClient() {
        Watch.RunToken run = new Watch.RunToken();
        run.cancel();
        assertTrue(run.cancelled());
        assertFalse(run.attach(new GithubClient("")));
    }

    @Test public void staleManualRefreshCannotReuseTheOldBaseline() {
        assertFalse(Watch.baselineStillSaved("old", "new"));
        assertTrue(Watch.baselineStillSaved("", null));
    }
}
