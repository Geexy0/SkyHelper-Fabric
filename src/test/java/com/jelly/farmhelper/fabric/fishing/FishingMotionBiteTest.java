package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingMotionBiteTest {
    @Test
    void detectsSuddenBobberDip() {
        assertTrue(FishingBiteSignals.isMotionBiteSignal(64.0, 63.9, 0.0, -0.2));
        assertTrue(FishingBiteSignals.isMotionBiteSignal(10.0, 9.95, -0.01, -0.2));
    }

    @Test
    void ignoresCalmBobber() {
        assertFalse(FishingBiteSignals.isMotionBiteSignal(64.0, 64.01, 0.0, 0.01));
        assertFalse(FishingBiteSignals.isMotionBiteSignal(Double.NaN, 64.0, 0.0, -0.2));
    }
}
