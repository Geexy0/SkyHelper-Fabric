package com.jelly.farmhelper.fabric.feature;

/**
 * Shared GUI retry budget helper for visitors / sell / pests menu flows.
 */
public final class GuiRetryHelper {
    private GuiRetryHelper() {
    }

    public static int clampLimit(int configured, int minimum) {
        return Math.max(minimum, configured);
    }

    /**
     * @return true if another retry is allowed (and caller should bump attempts).
     */
    public static boolean canRetry(int attemptsSoFar, int limit) {
        return attemptsSoFar < Math.max(1, limit);
    }

    public static long backoffTicks(int attemptsSoFar, long baseTicks) {
        int attempt = Math.max(0, attemptsSoFar);
        long base = Math.max(5L, baseTicks);
        long scaled = base + (long) attempt * 10L;
        return Math.min(120L, scaled);
    }
}
