package com.jelly.farmhelper.fabric.fishing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure anti-detect timing / break helpers for fishing (no Minecraft types).
 */
public final class FishingAntiDetect {
    public enum Strength {
        LOW,
        NORMAL,
        HIGH
    }

    private long lastDelayMs = -1L;
    private long nextBreakAtTick = -1L;
    private long breakUntilTick = -1L;
    private long sessionStartTick = -1L;
    private final long sessionSeed;

    public FishingAntiDetect() {
        this(ThreadLocalRandom.current().nextLong());
    }

    public FishingAntiDetect(long sessionSeed) {
        this.sessionSeed = sessionSeed;
    }

    public long sessionSeed() {
        return sessionSeed;
    }

    public void onSessionStart(long nowTick) {
        sessionStartTick = nowTick;
        lastDelayMs = -1L;
        breakUntilTick = -1L;
        nextBreakAtTick = -1L;
    }

    public void reset() {
        sessionStartTick = -1L;
        lastDelayMs = -1L;
        breakUntilTick = -1L;
        nextBreakAtTick = -1L;
    }

    public boolean isInBreak(long nowTick) {
        return breakUntilTick > 0 && nowTick < breakUntilTick;
    }

    public boolean shouldStartBreak(long nowTick, boolean breaksEnabled) {
        if (!breaksEnabled || isInBreak(nowTick)) {
            return false;
        }
        return nextBreakAtTick > 0 && nowTick >= nextBreakAtTick;
    }

    public void scheduleNextBreak(long nowTick, int intervalMinSec, int intervalMaxSec) {
        int min = Math.max(30, Math.min(intervalMinSec, intervalMaxSec));
        int max = Math.max(min, Math.max(intervalMinSec, intervalMaxSec));
        long delayTicks = sampleRangeMs(min * 1000L, max * 1000L, Strength.NORMAL) / 50L;
        nextBreakAtTick = nowTick + Math.max(20L * 30L, delayTicks);
    }

    public void beginBreak(long nowTick, int breakMinSec, int breakMaxSec) {
        int min = Math.max(2, Math.min(breakMinSec, breakMaxSec));
        int max = Math.max(min, Math.max(breakMinSec, breakMaxSec));
        long lenTicks = sampleRangeMs(min * 1000L, max * 1000L, Strength.NORMAL) / 50L;
        breakUntilTick = nowTick + Math.max(40L, lenTicks);
        nextBreakAtTick = -1L;
    }

    public void endBreak(long nowTick, int intervalMinSec, int intervalMaxSec) {
        breakUntilTick = -1L;
        scheduleNextBreak(nowTick, intervalMinSec, intervalMaxSec);
    }

    public boolean sessionExpired(long nowTick, int sessionLimitMinutes) {
        if (sessionLimitMinutes <= 0 || sessionStartTick < 0) {
            return false;
        }
        long limitTicks = (long) sessionLimitMinutes * 60L * 20L;
        return nowTick - sessionStartTick >= limitTicks;
    }

    /**
     * Sample a delay in milliseconds; never returns the exact previous delay when range allows.
     */
    public long sampleDelayMs(long minMs, long maxMs, Strength strength) {
        long sampled = sampleRangeMs(minMs, maxMs, strength == null ? Strength.NORMAL : strength);
        if (lastDelayMs >= 0 && sampled == lastDelayMs && maxMs > minMs) {
            sampled = sampled >= maxMs ? Math.max(minMs, sampled - 1L) : sampled + 1L;
        }
        lastDelayMs = sampled;
        return sampled;
    }

    public long sampleDelayTicks(long minMs, long maxMs, Strength strength) {
        return Math.max(1L, sampleDelayMs(minMs, maxMs, strength) / 50L);
    }

    public long sampleReactionTicks(Strength strength) {
        Strength s = strength == null ? Strength.NORMAL : strength;
        long min = switch (s) {
            case LOW -> 60L;
            case HIGH -> 100L;
            case NORMAL -> 80L;
        };
        long max = switch (s) {
            case LOW -> 220L;
            case HIGH -> 450L;
            case NORMAL -> 350L;
        };
        // Rare slow reaction outlier (~8%).
        if (ThreadLocalRandom.current().nextDouble() < 0.08) {
            max = Math.round(max * 1.6);
        }
        return sampleDelayTicks(min, max, s);
    }

    public float idleYawDelta(float amplitudeDegrees) {
        float amp = Math.max(0.1f, amplitudeDegrees);
        return (float) ((ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * amp);
    }

    public float idlePitchDelta(float amplitudeDegrees) {
        float amp = Math.max(0.05f, amplitudeDegrees * 0.55f);
        return (float) ((ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * amp);
    }

    public long rotationDurationMs(Strength strength) {
        Strength s = strength == null ? Strength.NORMAL : strength;
        long min = switch (s) {
            case LOW -> 90L;
            case HIGH -> 160L;
            case NORMAL -> 120L;
        };
        long max = switch (s) {
            case LOW -> 220L;
            case HIGH -> 420L;
            case NORMAL -> 320L;
        };
        return sampleDelayMs(min, max, s);
    }

    static long sampleRangeMs(long minMs, long maxMs, Strength strength) {
        long min = Math.max(1L, Math.min(minMs, maxMs));
        long max = Math.max(min, Math.max(minMs, maxMs));
        if (min == max) {
            return min;
        }
        Strength s = strength == null ? Strength.NORMAL : strength;
        double spread = switch (s) {
            case LOW -> 0.18;
            case HIGH -> 0.42;
            case NORMAL -> 0.28;
        };
        double mean = (min + max) / 2.0;
        double sigma = (max - min) * spread;
        double gaussian = mean + ThreadLocalRandom.current().nextGaussian() * sigma;
        long value = Math.round(gaussian);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        // Rare long outlier.
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            value = Math.min(max, Math.round(value + (max - min) * 0.35));
        }
        return value;
    }
}
