package app.unocontrol;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdPolicyTest {
    @Test public void neverShownIsDue() {
        assertTrue(AdPolicy.due(1_000L, 0L));
        assertTrue(AdPolicy.due(1_000L, -1L));
    }

    @Test public void withinHourIsNotDue() {
        long now = 10_000_000L;
        assertFalse(AdPolicy.due(now, now - AdPolicy.HOUR_MS + 1L));
        assertFalse(AdPolicy.due(now, now));
    }

    @Test public void afterHourIsDue() {
        long now = 10_000_000L;
        assertTrue(AdPolicy.due(now, now - AdPolicy.HOUR_MS));
        assertTrue(AdPolicy.due(now, now - AdPolicy.HOUR_MS - 1L));
    }
}
