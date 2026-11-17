package app.unocontrol;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Initializes the Yandex Mobile Ads SDK once. Failure is ignored:
 * a missing or broken SDK must not stop GitTeller from reading GitHub.
 */
public final class GitTellerApp extends Application {
    private int started;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pinIcon = () -> {
        if (started > 0) return;
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("gitteller_v1", MODE_PRIVATE);
            IconPins.apply(this, prefs.getString(Palette.PREF, "sand"));
        } catch (Throwable ignored) {
            // A broken icon must not stop the next launch.
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        try {
            com.yandex.mobile.ads.common.MobileAds.initialize(this, () -> {});
        } catch (Throwable ignored) {
            // Ads are optional. The app works without them.
        }
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("gitteller_v1", MODE_PRIVATE);
            IconPins.enableChosen(this, prefs.getString(Palette.PREF, "sand"));
            Watch.ensure(this);
        } catch (Throwable ignored) {
            // A broken icon or alarm must not stop the app from opening.
        }
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) {
                started++;
                handler.removeCallbacks(pinIcon);
            }
            @Override public void onActivityStopped(Activity activity) {
                if (started > 0) started--;
                if (started == 0) handler.postDelayed(pinIcon, 800);
            }
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
