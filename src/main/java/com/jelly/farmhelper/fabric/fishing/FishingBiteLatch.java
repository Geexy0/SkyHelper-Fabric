package com.jelly.farmhelper.fabric.fishing;

/**
 * Shared bite latch so packet listeners and the bobber sensor can all signal a pull.
 */
public final class FishingBiteLatch {
    private static volatile long untilTick = -1L;
    private static volatile String lastReason = "";

    private FishingBiteLatch() {
    }

    public static void mark(long nowTick, int holdTicks, String reason) {
        long until = nowTick + Math.max(2, holdTicks);
        if (until > untilTick) {
            untilTick = until;
        }
        if (reason != null && !reason.isBlank()) {
            lastReason = reason;
        }
    }

    public static boolean isActive(long nowTick) {
        return untilTick >= 0L && nowTick <= untilTick;
    }

    public static String lastReason() {
        return lastReason == null ? "" : lastReason;
    }

    public static void clear() {
        untilTick = -1L;
        lastReason = "";
    }
}
