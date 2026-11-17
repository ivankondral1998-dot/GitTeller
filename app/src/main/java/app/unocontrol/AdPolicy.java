package app.unocontrol;

/** Pure clock for the once-per-hour interstitial. No Android, no token. */
final class AdPolicy {
    static final long HOUR_MS = 60L * 60L * 1000L;

    private AdPolicy() {}

    /** lastShown <= 0 means never shown, so the next update is due. */
    static boolean due(long nowMs, long lastShownMs) {
        if (lastShownMs <= 0L) return true;
        return nowMs - lastShownMs >= HOUR_MS;
    }
}
