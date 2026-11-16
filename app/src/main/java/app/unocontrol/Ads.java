package app.unocontrol;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.AdSize;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/**
 * Banner and interstitial. Every call fails open: a missing ad never blocks
 * GitHub refresh. Frequency lives in gitteller_v1, never in the token vault.
 */
final class Ads {
    private static final String KEY = "ads_interstitial_at";

    private BannerAdView banner;
    private InterstitialAdLoader loader;
    private InterstitialAd interstitial;
    private boolean showing;

    void attachBanner(Activity activity, FrameLayout slot) {
        if (activity == null || slot == null) return;
        try {
            if (banner != null) {
                try { banner.destroy(); } catch (Throwable ignored) {}
                banner = null;
            }
            BannerAdView view = new BannerAdView(activity);
            int width = Math.max(320, activity.getResources().getDisplayMetrics().widthPixels
                    / Math.max(1, activity.getResources().getDisplayMetrics().densityDpi / 160));
            view.setAdSize(AdSize.stickySize(activity, Math.min(width, 400)));
            view.setAdUnitId(AdUnits.BANNER);
            view.setBannerAdEventListener(new BannerAdEventListener() {
                @Override public void onAdLoaded() { slot.setVisibility(android.view.View.VISIBLE); }
                @Override public void onAdFailedToLoad(AdRequestError error) { slot.setVisibility(android.view.View.GONE); }
                @Override public void onAdClicked() {}
                @Override public void onImpression(ImpressionData data) {}
                @Override public void onLeftApplication() {}
                @Override public void onReturnedToApplication() {}
            });
            slot.removeAllViews();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            slot.addView(view, lp);
            view.loadAd(new AdRequest.Builder(AdUnits.BANNER).build());
            banner = view;
        } catch (Throwable ignored) {
            slot.setVisibility(android.view.View.GONE);
        }
    }

    void preload(Activity activity) {
        if (activity == null || interstitial != null) return;
        try {
            if (loader == null) loader = new InterstitialAdLoader(activity);
            loader.loadAd(new AdRequest.Builder(AdUnits.INTERSTITIAL).build(), new InterstitialAdLoadListener() {
                @Override public void onAdLoaded(InterstitialAd ad) { interstitial = ad; }
                @Override public void onAdFailedToLoad(AdRequestError error) { interstitial = null; }
            });
        } catch (Throwable ignored) {
            interstitial = null;
        }
    }

    /** Show only if due and loaded. onFinished runs once, whether or not an ad appeared. */
    boolean showInterstitialIfDue(Activity activity, SharedPreferences prefs, Runnable onFinished) {
        if (activity == null || showing) return false;
        long now = System.currentTimeMillis();
        long last = prefs == null ? 0L : prefs.getLong(KEY, 0L);
        if (!AdPolicy.due(now, last) || interstitial == null) return false;
        try {
            showing = true;
            final boolean[] done = {false};
            Runnable once = () -> {
                if (done[0]) return;
                done[0] = true;
                showing = false;
                interstitial = null;
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
        try { if (banner != null) banner.destroy(); } catch (Throwable ignored) {}
        banner = null;
        interstitial = null;
        loader = null;
    }
}
