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
        if (!alive || activity == null || slot == null) return;
        try {
            if (banner != null) {
                BannerAdView old = banner;
                banner = null;
                try { old.destroy(); } catch (Throwable ignored) {}
            }
            BannerAdView view = new BannerAdView(activity);
            banner = view; // Also release a partially configured view if setup throws.
            // Small fixed banner (320x50), not sticky (sticky takes up to 15% of screen).
            view.setAdSize(BannerAdSize.fixedSize(activity, 320, 50));
            view.setAdUnitId(AdUnits.BANNER);
            view.setBannerAdEventListener(new BannerAdEventListener() {
                @Override public void onAdLoaded() { bannerResult(activity, view, slot, true, onVisibility); }
                @Override public void onAdFailedToLoad(AdRequestError error) { bannerResult(activity, view, slot, false, onVisibility); }
                @Override public void onAdClicked() {}
                @Override public void onImpression(ImpressionData data) {}
                @Override public void onLeftApplication() {}
                @Override public void onReturnedToApplication() {}
            });
            slot.removeAllViews();
            int h = px(activity, 50);
            slot.setClipChildren(true);
            slot.setClipToPadding(true);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h, Gravity.CENTER);
            slot.addView(view, lp);
            view.loadAd(new AdRequest.Builder().build());
        } catch (Throwable ignored) {
            BannerAdView failed = banner;
            banner = null;
            try { if (failed != null) failed.destroy(); } catch (Throwable ignoredAgain) {}
            slot.removeAllViews();
            showSlot(activity, slot, false, onVisibility);
        }
    }

    private void bannerResult(Activity activity, BannerAdView source, FrameLayout slot, boolean visible, Runnable onVisibility) {
        activity.runOnUiThread(() -> {
            if (!alive || banner != source || activity.isFinishing() || activity.isDestroyed()) return;
            showSlot(activity, slot, visible, onVisibility);
        });
    }

    private static void showSlot(Activity activity, FrameLayout slot, boolean visible, Runnable onVisibility) {
        int h = px(slot.getContext(), 50);
        android.view.ViewGroup.LayoutParams box = slot.getLayoutParams();
        if (box != null) {
            box.height = visible ? h : 0;
            slot.setLayoutParams(box);
        }
        if (slot.getChildCount() > 0) {
            android.view.View child = slot.getChildAt(0);
            android.view.ViewGroup.LayoutParams childBox = child.getLayoutParams();
            if (childBox != null) {
                childBox.height = h;
                child.setLayoutParams(childBox);
            }
        }
        slot.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        if (onVisibility == null || activity == null) return;
        activity.runOnUiThread(onVisibility);
    }

    private static int px(android.content.Context context, int dp) {
        if (context == null) return dp;
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    void preload(Activity activity) {
        if (!alive || activity == null || interstitial != null || loadingAd) return;
        try {
            if (loader == null) loader = new InterstitialAdLoader(activity);
            loadingAd = true;
            loader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    activity.runOnUiThread(() -> {
                        if (!alive) return;
                        loadingAd = false;
                        interstitial = ad;
                    });
                }
                @Override public void onAdFailedToLoad(AdRequestError error) {
                    activity.runOnUiThread(() -> {
                        if (!alive) return;
                        loadingAd = false;
                        interstitial = null;
                    });
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
        if (!alive || activity == null) return false;
        if (showing) return true; // Coalesce refresh requests until the first ad callback.
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
                @Override public void onAdFailedToShow(AdError error) { activity.runOnUiThread(once); }
                @Override public void onAdDismissed() { activity.runOnUiThread(once); }
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
