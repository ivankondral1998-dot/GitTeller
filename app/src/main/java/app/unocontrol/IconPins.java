package app.unocontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** One launcher icon at a time. The picture carries the chosen color and the name. */
final class IconPins {
    private IconPins() {}

    static void apply(Context context, String lookId) {
        if (context == null) return;
        PackageManager pm = context.getPackageManager();
        String chosen = Palette.find(lookId).id;
        boolean pinned = false;
        for (Palette.Look look : Palette.all()) {
            ComponentName name = new ComponentName(context, "app.unocontrol." + look.alias);
            boolean should = look.id.equals(chosen);
            try {
                int state = pm.getComponentEnabledSetting(name);
                boolean on = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && "sand".equals(look.id));
                if (on != should) {
                    pm.setComponentEnabledSetting(name,
                            should ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
                if (should) pinned = true;
            } catch (Exception ignored) {
                // One failed alias must not turn off every icon.
            }
        }
        if (!pinned) {
            try {
                pm.setComponentEnabledSetting(new ComponentName(context, "app.unocontrol.IconSand"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception ignored) {}
        }
    }
}
