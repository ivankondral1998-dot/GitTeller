package app.unocontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

/** Infrequent quiet check. Failures stay silent and do not replace a saved snapshot. */
final class Watch {
    static final String PREF = "quiet_watch";
    static final long INTERVAL_MS = 6L * 60L * 60L * 1000L;
    static final long FIRST_MS = 15L * 60L * 1000L;
    static final String ACTION = "app.unocontrol.QUIET_WATCH";
    static final String EXTRA_REPO = "quiet_repo";
    static final String NEXT = "quiet_next_at";
    /** Snapshot writers share this so a slow check cannot replace a newer save. */
    static final Object WRITE = new Object();

    private Watch() {}

    /** False once a manual refresh or import has replaced the snapshot this check started from. */
    static boolean baselineStillSaved(String started, String savedNow) {
        if (started == null) started = "";
        if (savedNow == null) savedNow = "";
        return started.equals(savedNow);
    }

    static boolean enabled(SharedPreferences prefs) {
        return prefs == null || prefs.getBoolean(PREF, true);
    }

    /**
     * Negative means leave the existing alarm alone.
     * A future deadline is kept. A due alarm is left to the receiver.
     * Only a missing deadline, or a boot that wiped alarms, schedules a new one.
     */
    static long scheduleDelay(long now, long next, boolean force) {
        if (!force && next > now) return -1L;
        if (!force && next != 0L) return -1L;
        if (force && next > now) return Math.max(60_000L, next - now);
        if (force) return INTERVAL_MS;
        return FIRST_MS;
    }

    static void ensure(Context context) {
        hold(context, false);
    }

    /** Boot clears alarms. Put the next check back without shrinking it to the first delay. */
    static void restoreAfterBoot(Context context) {
        hold(context, true);
    }

    private static void hold(Context context, boolean force) {
        if (context == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pi = pending(context);
        SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!enabled(prefs)) {
            alarms.cancel(pi);
            prefs.edit().remove(NEXT).apply();
            return;
        }
        long now = System.currentTimeMillis();
        long next = prefs.getLong(NEXT, 0L);
        long delay = scheduleDelay(now, next, force);
        if (delay < 0L) return;
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delay, INTERVAL_MS, pi);
        prefs.edit().putLong(NEXT, now + delay).apply();
    }

    static void run(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!enabled(prefs)) return;
        String repo = prefs.getString("repo", "octocat/Hello-World");
        String key = repo.toLowerCase(java.util.Locale.ROOT);
        String baseline = prefs.getString(key + ":current", "");
        if (baseline == null) baseline = "";
        JSONObject current = parse(baseline);
        if (current == null || SnapshotCodec.isImported(current)) return;
        String token = "";
        try {
            String loaded = new TokenVault(context).load(repo);
            if (loaded != null) token = loaded;
        } catch (Exception ignored) {
            return;
        }
        try {
            JSONObject fresh = new GithubClient(token).load(repo, current).snapshot;
            synchronized (WRITE) {
                String saved = prefs.getString(key + ":current", "");
                if (saved == null) saved = "";
                if (!baselineStillSaved(baseline, saved)) return;
                if (QuietNotice.worthTelling(fresh, current)) QuietPoster.postIfNew(context, fresh, current);
                SharedPreferences.Editor edit = prefs.edit()
                        .putString(key + ":previous", current.toString())
                        .putString(key + ":current", fresh.toString())
                        .remove(key + ":lastError");
                try {
                    JSONArray prior = parseArray(prefs.getString(key + ":feed", ""));
                    edit.putString(key + ":feed", EventFeed.remember(prior, current, fresh).toString());
                } catch (Exception ignored) {
                    // A broken local feed must not discard the fresh snapshot.
                }
                edit.commit();
            }
        } catch (Exception ignored) {
            // A missed check is not a reason to buzz or to drop the last good snapshot.
        }
    }

    private static PendingIntent pending(Context context) {
        Intent intent = new Intent(context, WatchReceiver.class);
        intent.setAction(ACTION);
        return PendingIntent.getBroadcast(context, 9, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static JSONArray parseArray(String raw) {
        try {
            return raw == null || raw.isEmpty() ? new JSONArray() : new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static JSONObject parse(String raw) {
        try {
            return raw == null || raw.isEmpty() ? null : new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
