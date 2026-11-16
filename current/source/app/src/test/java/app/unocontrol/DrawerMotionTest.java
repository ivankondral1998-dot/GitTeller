package app.unocontrol;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawerMotionTest {
    @Test public void doubleOpenBackAndOutsideTapDoNotStackOrLeaveTheApp() {
        DrawerMotion drawer = new DrawerMotion();
        assertTrue(drawer.requestOpen());
        assertFalse(drawer.requestOpen());
        drawer.markOpen();
        assertTrue(drawer.open);
        assertTrue(drawer.consumeBack());
        assertFalse(drawer.consumeBack() && drawer.requestClose());
        assertTrue(drawer.closing);
        drawer.markClosed();
        assertFalse(drawer.open);
        assertFalse(drawer.consumeBack());
        drawer.requestOpen();
        drawer.markOpen();
        assertTrue(drawer.requestClose());
        assertFalse(drawer.requestClose());
        drawer.markClosed();
        assertFalse(drawer.busy);
    }
}
