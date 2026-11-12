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
    void biteLatchIgnoredUnlessLineOut() {
        FishingBiteLatch.clear();
        FishingBiteLatch.mark(10L, 12, "test");
        assertFalse(FishingBiteLatch.isActive(10L));

        FishingLineState.markCast(10L);
        FishingBiteLatch.mark(11L, 12, "title");
        assertTrue(FishingBiteLatch.isActive(11L));

        FishingLineState.markReel(20L);
        assertFalse(FishingLineState.isLineOut());
        assertFalse(FishingBiteLatch.isActive(21L));
    }

    @Test
    void rodUseCooldown() {
        FishingLineState.markCast(0L);
        assertFalse(FishingLineState.canUseRod(5L, 18));
        assertTrue(FishingLineState.canUseRod(18L, 18));
    }
}
