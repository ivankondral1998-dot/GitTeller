package app.unocontrol;

import android.app.Application;

/**
 * Initializes the Yandex Mobile Ads SDK once. Failure is ignored:
 * a missing or broken SDK must not stop GitTeller from reading GitHub.
 */
public final class GitTellerApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        try {
            com.yandex.mobile.ads.common.MobileAds.initialize(this, () -> {});
        } catch (Throwable ignored) {
            // Ads are optional. The app works without them.
        }
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("gitteller_v1", MODE_PRIVATE);
            IconPins.apply(this, prefs.getString(Palette.PREF, "sand"));
            Watch.ensure(this);
        } catch (Throwable ignored) {
            // A broken icon or alarm must not stop the app from opening.
        }
    }
}
