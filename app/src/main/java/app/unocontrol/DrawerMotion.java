package app.unocontrol;

/** Open/close rules for the home drawer. Views animate; this only stops double opens and a Back exit. */
final class DrawerMotion {
    boolean open;
    boolean busy;
    boolean closing;

    boolean requestOpen() {
        if (open || busy || closing) return false;
        busy = true;
        return true;
    }

    void markOpen() {
        open = true;
        busy = false;
        closing = false;
    }

    boolean requestClose() {
        if (closing) return false;
        if (!open && !busy) return false;
        closing = true;
        busy = true;
        return true;
    }

    void markClosed() {
        open = false;
        busy = false;
        closing = false;
    }

    /** True when Back must close the panel instead of leaving the app. */
    boolean consumeBack() {
        if (!open && !busy) return false;
        requestClose();
        return true;
    }
}
