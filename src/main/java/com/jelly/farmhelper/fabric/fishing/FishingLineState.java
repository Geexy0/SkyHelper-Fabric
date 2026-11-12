package com.jelly.farmhelper.fabric.fishing;

/**
 * Tracks whether a cast is outstanding and enforces rod-use cooldowns (stops lava spam).
 */
public final class FishingLineState {
    /** Ignore all bite signals until the bobber has settled (cast splash ≠ bite). */
    public static final int BITE_GRACE_TICKS = 50;

    private static volatile boolean lineOut;
    private static volatile long lastUseTick = -10_000L;
    private static volatile long castAtTick = -1L;

    private FishingLineState() {
    }

    public static void reset() {
        lineOut = false;
        lastUseTick = -10_000L;
        castAtTick = -1L;
        FishingBiteLatch.clear();
    }

    public static boolean isLineOut() {
        return lineOut;
    }

    public static long castAtTick() {
        return castAtTick;
    }

    public static void markCast(long nowTick) {
        lineOut = true;
        castAtTick = nowTick;
        lastUseTick = nowTick;
        FishingBiteLatch.clear();
    }

    public static void markReel(long nowTick) {
        lineOut = false;
        castAtTick = -1L;
        lastUseTick = nowTick;
        FishingBiteLatch.clear();
    }

    public static boolean canUseRod(long nowTick, int minGapTicks) {
        return nowTick - lastUseTick >= Math.max(1, minGapTicks);
    }

    /**
     * True only after post-cast grace — blocks cast-landing splash/motion false bites.
     */
    public static boolean canAcceptBite(long nowTick) {
        return lineOut && castAtTick >= 0L && nowTick >= castAtTick + BITE_GRACE_TICKS;
    }
}
