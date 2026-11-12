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
        // Hypixel SkyBlock bite popup — require at least two '!'. Single '!' is too noisy.
        String stripped = normalized.replace(" ", "");
        if (stripped.contains("!!") || normalized.matches(".*!{2,}.*")) {
            return true;
        }
        return normalized.contains("fish bites")
                || normalized.contains("something bites")
                || normalized.contains("got a bite");
    }

    /**
     * Bobber suddenly yanked down. Keep thresholds modest to avoid lava jitter false positives.
     */
    public static boolean isMotionBiteSignal(double prevY, double currY, double prevVelY, double currVelY) {
        if (Double.isNaN(prevY) || Double.isNaN(currY)) {
            return false;
        }
        double dy = currY - prevY;
        if (dy <= -0.08) {
            return true;
        }
        return !Double.isNaN(prevVelY)
                && !Double.isNaN(currVelY)
                && prevVelY > -0.02
                && currVelY <= -0.2;
    }

    /**
     * Sound-based bites disabled for SkyBlock — cast-landing splash looks identical to a bite.
     */
    public static boolean isBiteSoundHint(String soundId) {
        return false;
    }

    /**
     * Particles are not used for bite detection — lava spams splash/bubble constantly.
     */
    public static boolean isBiteParticleHint(String particleId) {
        return false;
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
