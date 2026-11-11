package com.jelly.farmhelper.fabric.feature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiRetryHelperTest {
    @Test
    void clampsAndGatesRetries() {
        assertEquals(3, GuiRetryHelper.clampLimit(1, 3));
        assertEquals(5, GuiRetryHelper.clampLimit(5, 2));
        assertTrue(GuiRetryHelper.canRetry(0, 2));
        assertTrue(GuiRetryHelper.canRetry(1, 2));
        assertFalse(GuiRetryHelper.canRetry(2, 2));
    }

    @Test
    void backoffScalesAndCaps() {
        assertEquals(10L, GuiRetryHelper.backoffTicks(0, 10L));
        assertEquals(30L, GuiRetryHelper.backoffTicks(2, 10L));
        assertEquals(120L, GuiRetryHelper.backoffTicks(50, 40L));
    }
}
