package app.unocontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** One launcher icon at a time. Disabling the icon that opened the app closes it, so that step waits. */
final class IconPins {
    private IconPins() {}

    /** Turn the chosen icon on and the others off. Call only when no screen is open. */
    static void apply(Context context, String lookId) {
        if (context == null) return;
        Palette.Look chosen = Palette.find(lookId);
        if (!enable(context, chosen)) {
            enable(context, Palette.find("sand"));
            return;
        }
        for (Palette.Look look : Palette.all()) {
            if (!look.id.equals(chosen.id)) disable(context, look);
        }
    }

    /** Add the chosen icon without turning the current one off. Safe while a screen is open. */
    static void enableChosen(Context context, String lookId) {
        if (context == null) return;
        if (!enable(context, Palette.find(lookId))) enable(context, Palette.find("sand"));
    }

    private static boolean enable(Context context, Palette.Look look) {
        return set(context, look, true);
    }

    private static void disable(Context context, Palette.Look look) {
        set(context, look, false);
    }

    private static boolean set(Context context, Palette.Look look, boolean should) {
        PackageManager pm = context.getPackageManager();
        ComponentName name = new ComponentName(context, "app.unocontrol." + look.alias);
        try {
            int state = pm.getComponentEnabledSetting(name);
            boolean on = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && "sand".equals(look.id));
            if (on == should) return true;
            pm.setComponentEnabledSetting(name,
                    should ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
