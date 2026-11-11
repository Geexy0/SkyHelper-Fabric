package com.jelly.farmhelper.fabric.fishing;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure bite / fishing chat-title signal helpers.
 */
public final class FishingBiteSignals {
    private static final Pattern FORMATTING = Pattern.compile("§.");

    private FishingBiteSignals() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return FORMATTING.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isBiteHint(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        return normalized.contains("!!!")
                || normalized.contains("fish bites")
                || normalized.contains("something bites")
                || normalized.contains("got a bite")
                || normalized.equals("!");
    }

    public static boolean isStaffOrPauseHint(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        return normalized.contains("sending to server")
                || normalized.contains("warping")
                || normalized.contains("evacuation")
                || normalized.contains("limbo")
                || normalized.contains("[staff]")
                || normalized.contains("a player has been removed");
    }
}
