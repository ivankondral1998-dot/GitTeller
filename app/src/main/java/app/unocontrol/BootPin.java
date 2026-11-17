package app.unocontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Reschedules the quiet check after boot. Does not read the token. */
public final class BootPin extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) Watch.ensure(context);
    }
}
