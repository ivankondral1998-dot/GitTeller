package app.unocontrol;

/** Ten muted looks. Each has a day set and a night set. No neon. */
final class Palette {
    static final String PREF = "look";
    static final String DAY_NIGHT = "day_night";
    static final String SYSTEM = "system";
    static final String DAY = "day";
    static final String NIGHT = "night";

    static final class Look {
        final String id;
        final String title;
        final String alias;
        final int icon;
        final int iconInk;
        final int[] day;
        final int[] night;

        Look(String id, String title, String alias, int icon, int iconInk, int[] day, int[] night) {
            this.id = id;
            this.title = title;
            this.alias = alias;
            this.icon = icon;
            this.iconInk = iconInk;
            this.day = day;
            this.night = night;
        }
    }

    static final class Colors {
        final boolean dark;
        final int bg, panel, panel2, text, muted, accent, warn, stroke;

        Colors(boolean dark, int[] row) {
            this.dark = dark;
            bg = row[0];
            panel = row[1];
            panel2 = row[2];
            text = row[3];
            muted = row[4];
            accent = row[5];
            warn = row[6];
            stroke = row[7];
        }
    }

    private static final Look[] ALL = {
            look("sand", "Песок", "IconSand", 0xFFC4A574, 0xFF2A241C,
                    row(0xFFF6F3EC, 0xFFFFFFFF, 0xFFECE6DC, 0xFF1C2026, 0xFF5A626C, 0xFF8C6030, 0xFF8C5414, 0xFFD6CEC2),
                    row(0xFF0C1219, 0xFF17212C, 0xFF1E2B38, 0xFFEEF2F7, 0xFF9EACBC, 0xFFDAAE7F, 0xFFEFC680, 0xFF2F3F50)),
            look("sage", "Шалфей", "IconSage", 0xFF6E8F78, 0xFFF4F7F2,
                    row(0xFFF3F6F2, 0xFFFFFFFF, 0xFFE4EBE3, 0xFF1C2620, 0xFF5C6A60, 0xFF3F6B4E, 0xFF8A6A28, 0xFFD0D8D0),
                    row(0xFF101612, 0xFF1A2420, 0xFF24302A, 0xFFE8F0EA, 0xFF9AABA0, 0xFF8FBF9A, 0xFFE0C48A, 0xFF314038)),
            look("sea", "Море", "IconSea", 0xFF5E86A0, 0xFFF4F7F8,
                    row(0xFFF2F5F7, 0xFFFFFFFF, 0xFFE3EAEE, 0xFF1A242C, 0xFF5A6872, 0xFF2F5F78, 0xFF8A6230, 0xFFCDD6DC),
                    row(0xFF0C1218, 0xFF152028, 0xFF1E2C36, 0xFFE8F0F6, 0xFF96A8B6, 0xFF8EB8CE, 0xFFE4C48A, 0xFF2C3C48)),
            look("slate", "Грифель", "IconSlate", 0xFF7E8794, 0xFFF6F7F8,
                    row(0xFFF4F5F6, 0xFFFFFFFF, 0xFFE6E8EB, 0xFF1E2228, 0xFF5E6670, 0xFF4A5562, 0xFF8A5A30, 0xFFD2D6DB),
                    row(0xFF121416, 0xFF1C2026, 0xFF262C34, 0xFFECEFF3, 0xFFA0A8B2, 0xFFC5CDD6, 0xFFE6C48C, 0xFF343C46)),
            look("clay", "Глина", "IconClay", 0xFFC4896A, 0xFF2A201C,
                    row(0xFFF7F3EF, 0xFFFFFFFF, 0xFFF0E6DE, 0xFF2A201C, 0xFF6E6058, 0xFF8C4E34, 0xFF8A5A20, 0xFFDCCFC4),
                    row(0xFF16110E, 0xFF241C18, 0xFF2E241E, 0xFFF6EEE8, 0xFFB4A298, 0xFFE0A88C, 0xFFE8C48A, 0xFF40342C)),
            look("moss", "Мох", "IconMoss", 0xFF5E7848, 0xFFF4F6F0,
                    row(0xFFF4F6F1, 0xFFFFFFFF, 0xFFE6EBDF, 0xFF1C2418, 0xFF5C6854, 0xFF3E5C32, 0xFF8A6828, 0xFFD2D8C8),
                    row(0xFF10140E, 0xFF1A2216, 0xFF24301E, 0xFFEAF2E4, 0xFF9AAA94, 0xFFA4C48A, 0xFFE4C888, 0xFF34402C)),
            look("dusk", "Сумерки", "IconDusk", 0xFF7A6E96, 0xFFF7F5F8,
                    row(0xFFF5F3F7, 0xFFFFFFFF, 0xFFE8E4EE, 0xFF221E28, 0xFF686072, 0xFF5C4E78, 0xFF8A5A38, 0xFFD4D0DC),
                    row(0xFF121018, 0xFF1E1A26, 0xFF282234, 0xFFF0ECF6, 0xFFA8A0B4, 0xFFC4B4DC, 0xFFE6C490, 0xFF3A3448)),
            look("lagoon", "Лагуна", "IconLagoon", 0xFF3E7A76, 0xFFF3F7F6,
                    row(0xFFF2F6F5, 0xFFFFFFFF, 0xFFE2EEEC, 0xFF142422, 0xFF56706C, 0xFF1F6A64, 0xFF8A6830, 0xFFCDDAD6),
                    row(0xFF0E1616, 0xFF162222, 0xFF1E2E2C, 0xFFE6F4F2, 0xFF94B0AC, 0xFF8ECCC4, 0xFFE4C88C, 0xFF2C4040)),
            look("stone", "Камень", "IconStone", 0xFFA89880, 0xFF2A261E,
                    row(0xFFF6F4F0, 0xFFFFFFFF, 0xFFEBE6DE, 0xFF24221E, 0xFF6A645C, 0xFF6E5E48, 0xFF8A5C28, 0xFFD8D2C8),
                    row(0xFF141210, 0xFF201C18, 0xFF2A2620, 0xFFF2EEE8, 0xFFB0A89E, 0xFFD4C4A8, 0xFFE6C490, 0xFF3C3830)),
            look("berry", "Ягода", "IconBerry", 0xFF8E5A70, 0xFFF8F4F6,
                    row(0xFFF7F3F4, 0xFFFFFFFF, 0xFFF0E4E8, 0xFF281C22, 0xFF6E5A62, 0xFF7A3E56, 0xFF8A5A28, 0xFFDCCED2),
                    row(0xFF160E12, 0xFF241820, 0xFF2E2028, 0xFFF6ECF0, 0xFFB4A0A8, 0xFFE0A8BC, 0xFFE6C490, 0xFF403038))
    };

    private Palette() {}

    static Look[] all() { return ALL; }

    static Look find(String id) {
        if (id != null) for (Look look : ALL) if (look.id.equals(id)) return look;
        return ALL[0];
    }

    /** System night is dark only when the phone explicitly says so. Undefined is not forced dark. */
    static boolean dark(String mode, int nightBits) {
        if (DAY.equals(mode)) return false;
        if (NIGHT.equals(mode)) return true;
        return (nightBits & 0x30) == 0x20;
    }

    static String normalizeMode(String mode) {
        if (DAY.equals(mode) || NIGHT.equals(mode)) return mode;
        return SYSTEM;
    }

    static Colors colors(Look look, boolean dark) {
        return new Colors(dark, dark ? look.night : look.day);
    }

    private static Look look(String id, String title, String alias, int icon, int ink, int[] day, int[] night) {
        return new Look(id, title, alias, icon, ink, day, night);
    }

    private static int[] row(int bg, int panel, int panel2, int text, int muted, int accent, int warn, int stroke) {
        return new int[]{bg, panel, panel2, text, muted, accent, warn, stroke};
    }
}
