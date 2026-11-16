package app.unocontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/**
 * Banner and interstitial for Mobile Ads 7.18.1.
 * Banner: setAdUnitId + BannerAdSize.fixedSize(320, 50) + AdRequest.Builder().
 * Interstitial: InterstitialAdLoader.setAdLoadListener + AdRequestConfiguration.
 * Every call fails open: a missing ad never blocks GitHub refresh.
 * Frequency lives in gitteller_v1, never in the token vault.
 */
final class Ads {
    private static final String KEY = "ads_interstitial_at";

    private BannerAdView banner;
    private InterstitialAdLoader loader;
    private InterstitialAd interstitial;
    private boolean showing;
    private boolean loadingAd;
    private boolean alive = true;

    void attachBanner(Activity activity, FrameLayout slot, Runnable onVisibility) {
        if (activity == null || slot == null) return;
        try {
            if (banner != null) {
                try { banner.destroy(); } catch (Throwable ignored) {}
                banner = null;
            }
            BannerAdView view = new BannerAdView(activity);
            // Small fixed banner (320x50), not sticky (sticky takes up to 15% of screen).
            view.setAdSize(BannerAdSize.fixedSize(activity, 320, 50));
            view.setAdUnitId(AdUnits.BANNER);
            view.setBannerAdEventListener(new BannerAdEventListener() {
                @Override public void onAdLoaded() { showSlot(activity, slot, true, onVisibility); }
                @Override public void onAdFailedToLoad(AdRequestError error) { showSlot(activity, slot, false, onVisibility); }
                @Override public void onAdClicked() {}
                @Override public void onImpression(ImpressionData data) {}
                @Override public void onLeftApplication() {}
                @Override public void onReturnedToApplication() {}
            });
            slot.removeAllViews();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            slot.addView(view, lp);
            view.loadAd(new AdRequest.Builder().build());
            banner = view;
        } catch (Throwable ignored) {
            showSlot(activity, slot, false, onVisibility);
        }
    }

    private static void showSlot(Activity activity, FrameLayout slot, boolean visible, Runnable onVisibility) {
        slot.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        if (onVisibility == null || activity == null) return;
        activity.runOnUiThread(onVisibility);
    }

    void preload(Activity activity) {
        if (!alive || activity == null || interstitial != null || loadingAd) return;
        try {
            if (loader == null) loader = new InterstitialAdLoader(activity);
            loadingAd = true;
            loader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    loadingAd = false;
                    if (!alive) return;
                    interstitial = ad;
                }
                @Override public void onAdFailedToLoad(AdRequestError error) {
                    loadingAd = false;
                    interstitial = null;
                }
            });
            loader.loadAd(new AdRequestConfiguration.Builder(AdUnits.INTERSTITIAL).build());
        } catch (Throwable ignored) {
            loadingAd = false;
            interstitial = null;
        }
    }

    /** Show only if due and loaded. onFinished runs once, whether or not an ad appeared. */
    boolean showInterstitialIfDue(Activity activity, SharedPreferences prefs, Runnable onFinished) {
        if (!alive || activity == null || showing) return false;
        long now = System.currentTimeMillis();
        long last = prefs == null ? 0L : prefs.getLong(KEY, 0L);
        if (!AdPolicy.due(now, last) || interstitial == null) {
            if (interstitial == null) preload(activity);
            return false;
        }
        try {
            showing = true;
            final boolean[] done = {false};
            Runnable once = () -> {
                if (done[0]) return;
                done[0] = true;
                showing = false;
                interstitial = null;
                if (!alive) return;
                preload(activity);
                if (onFinished != null) onFinished.run();
            };
            interstitial.setAdEventListener(new InterstitialAdEventListener() {
                @Override public void onAdShown() { markShown(prefs, System.currentTimeMillis()); }
                @Override public void onAdFailedToShow(AdError error) { once.run(); }
                @Override public void onAdDismissed() { once.run(); }
                @Override public void onAdClicked() {}
                @Override public void onAdImpression(ImpressionData data) {}
            });
            interstitial.show(activity);
            return true;
        } catch (Throwable ignored) {
            showing = false;
            interstitial = null;
            return false;
        }
    }

    static void markShown(SharedPreferences prefs, long now) {
        if (prefs == null) return;
        try { prefs.edit().putLong(KEY, now).apply(); } catch (Throwable ignored) {}
    }

    void destroy() {
        alive = false;
        try { if (interstitial != null) interstitial.setAdEventListener(null); } catch (Throwable ignored) {}
        try { if (banner != null) banner.destroy(); } catch (Throwable ignored) {}
        try { if (loader != null) loader.cancelLoading(); } catch (Throwable ignored) {}
        banner = null;
        interstitial = null;
        loader = null;
        loadingAd = false;
        showing = false;
    }
}
