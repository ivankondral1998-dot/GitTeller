package app.unocontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONObject;

/** Infrequent quiet check. Failures stay silent and do not replace a saved snapshot. */
final class Watch {
    static final String PREF = "quiet_watch";
    static final long INTERVAL_MS = 6L * 60L * 60L * 1000L;
    static final String ACTION = "app.unocontrol.QUIET_WATCH";

    private Watch() {}

    static boolean enabled(SharedPreferences prefs) {
        return prefs == null || prefs.getBoolean(PREF, true);
    }

    static void ensure(Context context) {
        if (context == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pi = pending(context);
        SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!enabled(prefs)) {
            alarms.cancel(pi);
            return;
        }
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_MS, INTERVAL_MS, pi);
    }

    static void run(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!enabled(prefs)) return;
        String repo = prefs.getString("repo", "octocat/Hello-World");
        String key = repo.toLowerCase(java.util.Locale.ROOT);
        JSONObject current = parse(prefs.getString(key + ":current", ""));
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
            if (QuietNotice.worthTelling(fresh, current)) QuietPoster.postIfNew(context, fresh, current);
            prefs.edit()
                    .putString(key + ":previous", current.toString())
                    .putString(key + ":current", fresh.toString())
                    .remove(key + ":lastError")
                    .commit();
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

    private static JSONObject parse(String raw) {
        try {
            return raw == null || raw.isEmpty() ? null : new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
