package com.jelly.farmhelper.fabric.fishing;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure bite helpers. Hypixel SkyBlock bite cue matches PaulAddons FunnyFishing:
 * particle FLAME (lava) / WATER_WAKE|FISHING (water), count=6, speed=0.2, near bobber.
 */
public final class FishingBiteSignals {
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final float BITE_PARTICLE_SPEED = 0.2f;
    private static final float BITE_PARTICLE_SPEED_EPS = 0.001f;
    private static final int BITE_PARTICLE_COUNT = 6;
    /** Horizontal distance from bobber to particle (PaulAddons uses &lt; 0.1). */
    public static final double BITE_PARTICLE_MAX_DIST = 0.1;

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
        String stripped = normalized.replace(" ", "");
        if (stripped.contains("!!") || normalized.matches(".*!{2,}.*")) {
            return true;
        }
        return normalized.contains("fish bites")
                || normalized.contains("something bites")
                || normalized.contains("got a bite");
    }

    public static boolean isMotionBiteSignal(double prevY, double currY, double prevVelY, double currVelY) {
        // Not used for SB pull — kept for tests / future vanilla.
        if (Double.isNaN(prevY) || Double.isNaN(currY)) {
            return false;
        }
        return (currY - prevY) <= -0.08;
    }

    public static boolean isBiteSoundHint(String soundId) {
        return false;
    }

    /**
     * PaulAddons-compatible SkyBlock bite particle filter (type + count + speed only).
     * Caller must also check distance to the player's bobber.
     */
    public static boolean isSkyblockBiteParticle(String particleTypeId, int count, float speed) {
        if (count != BITE_PARTICLE_COUNT) {
            return false;
        }
        if (Math.abs(speed - BITE_PARTICLE_SPEED) > BITE_PARTICLE_SPEED_EPS) {
            return false;
        }
        String id = normalize(particleTypeId);
        if (id.isBlank()) {
            return false;
        }
        // Lava: flame. Water: wake / fishing (WATER_WAKE renamed in modern mappings).
        return id.contains("flame")
                || id.contains("wake")
                || id.equals("fishing")
                || id.endsWith(":fishing")
                || (id.contains("fishing") && !id.contains("bobber"));
    }

    @Deprecated
    public static boolean isBiteParticleHint(String particleId) {
        return isSkyblockBiteParticle(particleId, BITE_PARTICLE_COUNT, BITE_PARTICLE_SPEED);
    }

    public static boolean isNearBobberXZ(double particleX, double particleZ, double bobberX, double bobberY, double bobberZ) {
        double dx = particleX - bobberX;
        double dz = particleZ - bobberZ;
        // PaulAddons: getDistance(px, bobberY, pz) < 0.1
        double dy = 0.0;
        return dx * dx + dy * dy + dz * dz < BITE_PARTICLE_MAX_DIST * BITE_PARTICLE_MAX_DIST;
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
