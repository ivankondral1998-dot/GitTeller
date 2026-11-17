package app.unocontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WatchReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (!Watch.ACTION.equals(intent.getAction())) return;
        PendingResult pending = goAsync();
        new Thread(() -> {
            try { Watch.run(context.getApplicationContext()); }
            finally { pending.finish(); }
        }, "gitteller-watch").start();
    }
}
