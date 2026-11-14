package app.unocontrol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiModeTest {
    @Test public void unknownModeStaysNoviceAndReasonsDoNotInventProof() {
        assertEquals(UiMode.NOVICE, UiMode.normalize(null));
        assertEquals(UiMode.NOVICE, UiMode.normalize("invalid"));
        assertEquals(UiMode.DEVELOPER, UiMode.normalize(UiMode.DEVELOPER));
        assertTrue(UiMode.reason("changed", true, false, true).contains("два достоверных"));
        assertTrue(UiMode.reason("changed", true, true, false).contains("неполных"));
        assertTrue(UiMode.reason("changed", true, true, true).contains("не проверка"));
        assertTrue(UiMode.reason("who", true, true, true).contains("не определяют исполнителя"));
        assertTrue(UiMode.reason("problems", true, true, true).contains("не обязательно"));
        assertTrue(UiMode.reason("where", true, true, true).contains("не того, что агент"));
    }
}
