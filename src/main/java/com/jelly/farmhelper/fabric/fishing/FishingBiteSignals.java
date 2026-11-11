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
        // Hypixel often flashes "!!!" (or denser) as a title/subtitle on bite.
        if (normalized.contains("!!!") || normalized.matches("!{2,}")) {
            return true;
        }
        return normalized.contains("fish bites")
                || normalized.contains("something bites")
                || normalized.contains("got a bite")
                || normalized.contains("bite!")
                || normalized.equals("!");
    }

    /**
     * Bobber suddenly yanked down — works when vanilla DATA_BITING is never synced (Hypixel).
     */
    public static boolean isMotionBiteSignal(double prevY, double currY, double prevVelY, double currVelY) {
        if (Double.isNaN(prevY) || Double.isNaN(currY)) {
            return false;
        }
        double dy = currY - prevY;
        if (dy <= -0.04) {
            return true;
        }
        return !Double.isNaN(prevVelY)
                && !Double.isNaN(currVelY)
                && prevVelY > -0.08
                && currVelY <= -0.12;
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
