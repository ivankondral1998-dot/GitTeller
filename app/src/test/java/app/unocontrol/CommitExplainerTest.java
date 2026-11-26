package app.unocontrol;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommitExplainerTest {
    @Test public void bugFixIsClaimNotProof() {
        String explanation=CommitExplainer.explain("fix: checkpoint restore");
        assertTrue(explanation.contains("сообщил об исправлении"));
        assertTrue(explanation.contains("не проверена"));
    }
    @Test public void unknownCommitDoesNotAssertCompletion() {
        assertTrue(CommitExplainer.explain("we shipped it").contains("не проверена"));
    }
    @Test public void blankMessageIsExplicit() {
        assertTrue(CommitExplainer.explain("").contains("без описания"));
    }
    @Test public void testAndDocSubjectsAreNotProofOfChangedFiles() {
        assertTrue(CommitExplainer.explain("test: checks").contains("сообщил"));
        assertTrue(CommitExplainer.explain("docs: update").contains("сообщил"));
    }
}
