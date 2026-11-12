package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingLineStateTest {
    @AfterEach
    void cleanup() {
        FishingLineState.reset();
    }

    @Test
    void biteIgnoredDuringGraceAndWithoutLine() {
        FishingBiteLatch.markPopup(10L, "title");
        assertFalse(FishingBiteLatch.isActive(10L));

        FishingLineState.markCast(10L);
        FishingBiteLatch.markPopup(20L, "title");
        assertFalse(FishingLineState.canAcceptBite(20L));
        assertFalse(FishingBiteLatch.isActive(20L));

        long ready = 10L + FishingLineState.BITE_GRACE_TICKS;
        assertTrue(FishingLineState.canAcceptBite(ready));
        FishingBiteLatch.markPopup(ready, "title");
        assertTrue(FishingBiteLatch.isActive(ready));
    }

    @Test
    void rodUseCooldown() {
        FishingLineState.markCast(0L);
        assertFalse(FishingLineState.canUseRod(5L, 18));
        assertTrue(FishingLineState.canUseRod(18L, 18));
    }
}
