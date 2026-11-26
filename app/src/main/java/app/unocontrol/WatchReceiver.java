package app.unocontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WatchReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (!Watch.ACTION.equals(intent.getAction())) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("gitteller_v1", Context.MODE_PRIVATE);
        if (!Watch.enabled(prefs)) return;
        prefs.edit().putLong(Watch.NEXT, System.currentTimeMillis() + Watch.INTERVAL_MS).apply();
        Watch.enqueue(context);
    }
}
