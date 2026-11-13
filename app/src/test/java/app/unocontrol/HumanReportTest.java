package app.unocontrol;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HumanReportTest {
    @Test public void unknownBranchIsNotAProjectGuess() {
        assertEquals("Тема по названию не определена", HumanReport.branchTopic("research/uno-train"));
        assertFalse(HumanReport.branchTopic("feat/memory-core").contains("Память"));
        assertEquals("Основная ветка проекта", HumanReport.branchKind("main"));
    }

    @Test public void closedIsNotMergedAndChecksAreAbsent() {
        assertTrue(HumanReport.state("closed").contains("не проверен"));
        assertFalse(HumanReport.state("closed").toLowerCase().contains("merge"));
        assertEquals("Недостаточно данных", HumanReport.NOT_ENOUGH);
        assertEquals("Проверка работоспособности отсутствует", HumanReport.NO_CHECK);
    }

    @Test public void commitsAreScopedToTheDefaultBranch() throws Exception {
        JSONObject snap = new JSONObject();
        snap.put("defaultBranch", "release");
        assertEquals("ветки «release»", HumanReport.commitScope(snap));
    }
}
