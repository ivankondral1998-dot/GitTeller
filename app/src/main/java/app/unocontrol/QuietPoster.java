package app.unocontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONObject;

/** One quiet shade line. No sound, no heads-up, no full-screen page. */
final class QuietPoster {
    static final String CHANNEL = "gitteller_quiet";
    private static final int ID = 41;
    private static final String MARK = "notice_mark";

    private QuietPoster() {}

    static void postIfNew(Context context, JSONObject current, JSONObject previous) {
        if (context == null || !QuietNotice.worthTelling(current, previous)) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!Watch.enabled(prefs)) return;
        String line = QuietNotice.oneLine(current, previous);
        String mark = current.optLong("capturedAt", 0) + "|" + previous.optLong("capturedAt", 0) + "|" + line;
        if (mark.equals(prefs.getString(MARK, ""))) return;
        if (!allowed(context)) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Тихие изменения", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Одна строка, без звука. Подробности только в раскрытой шторке.");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String repo = current.optString("repo", "").trim();
        if (!repo.isEmpty()) open.putExtra(Watch.EXTRA_REPO, repo);
        PendingIntent tap = PendingIntent.getActivity(context, 7, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("GitTeller")
                .setContentText(line)
                .setStyle(new Notification.BigTextStyle().bigText(QuietNotice.detail(current, previous)))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setSound(null)
                .setDefaults(0);
        manager.notify(ID, builder.build());
        prefs.edit().putString(MARK, mark).apply();
    }

    /** App toggle is not enough: the system can still drop the shade line. */
    static boolean allowed(Context context) {
        if (context == null) return false;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && !manager.areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            android.app.NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }
}
