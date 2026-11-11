package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingAntiDetectTest {
    @Test
    void consecutiveDelaysDifferWhenRangeAllows() {
        FishingAntiDetect anti = new FishingAntiDetect(42L);
        long previous = -1L;
        for (int i = 0; i < 40; i++) {
            long delay = anti.sampleDelayMs(100L, 400L, FishingAntiDetect.Strength.NORMAL);
            assertTrue(delay >= 100L && delay <= 400L);
            if (previous >= 0) {
                assertNotEquals(previous, delay);
            }
            previous = delay;
        }
    }

    @Test
    void breakScheduleStartsAndEnds() {
        FishingAntiDetect anti = new FishingAntiDetect(7L);
        anti.onSessionStart(0L);
        anti.scheduleNextBreak(0L, 60, 60);
        assertFalse(anti.shouldStartBreak(10L, true));
        assertTrue(anti.shouldStartBreak(60L * 20L + 5L, true));

        anti.beginBreak(1000L, 5, 5);
        assertTrue(anti.isInBreak(1001L));
        assertFalse(anti.isInBreak(1000L + 5L * 20L + 40L));

        anti.endBreak(1200L, 60, 90);
        assertFalse(anti.isInBreak(1200L));
    }

    @Test
    void sessionLimitHonored() {
        FishingAntiDetect anti = new FishingAntiDetect(1L);
        anti.onSessionStart(0L);
        assertFalse(anti.sessionExpired(100L, 0));
        assertFalse(anti.sessionExpired(100L, 10));
        assertTrue(anti.sessionExpired(10L * 60L * 20L, 10));
    }

    @Test
    void reactionTicksStayPositive() {
        FishingAntiDetect anti = new FishingAntiDetect(99L);
        for (FishingAntiDetect.Strength strength : FishingAntiDetect.Strength.values()) {
            long ticks = anti.sampleReactionTicks(strength);
            assertTrue(ticks >= 1L);
        }
    }
}
