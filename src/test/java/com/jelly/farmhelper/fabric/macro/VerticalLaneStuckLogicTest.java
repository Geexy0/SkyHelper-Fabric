package com.jelly.farmhelper.fabric.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerticalLaneStuckLogicTest {
    @Test
    void openSideContinuesStrafe() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.CONTINUE_SIDE,
                VerticalLaneStuckLogic.decideWhileStrafing(true, false, false, true, 20)
        );
    }

    @Test
    void frontExitSwitchesLane() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.SWITCH_LANE,
                VerticalLaneStuckLogic.decideWhileStrafing(false, true, false, true, 0)
        );
    }

    @Test
    void rowEndWithNoExitAntiStucksAfterThreshold() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.RECALCULATE,
                VerticalLaneStuckLogic.decideWhileStrafing(false, false, false, true, 3)
        );
        assertEquals(
                VerticalLaneStuckLogic.Decision.ANTI_STUCK_RESET,
                VerticalLaneStuckLogic.decideWhileStrafing(
                        false,
                        false,
                        false,
                        true,
                        VerticalLaneStuckLogic.ROW_END_STUCK_THRESHOLD
                )
        );
    }

    @Test
    void rowEndPrefersForwardHopBeforeAntiStuck() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.FORWARD_HOP,
                VerticalLaneStuckLogic.decideWhileStrafing(
                        false,
                        false,
                        false,
                        true,
                        true,
                        VerticalLaneStuckLogic.HOP_STUCK_THRESHOLD
                )
        );
        assertEquals(
                VerticalLaneStuckLogic.Decision.ANTI_STUCK_RESET,
                VerticalLaneStuckLogic.decideWhileStrafing(
                        false,
                        false,
                        false,
                        true,
                        true,
                        VerticalLaneStuckLogic.ROW_END_STUCK_THRESHOLD
                )
        );
    }

    @Test
    void switchingStuckAntiResets() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.ANTI_STUCK_RESET,
                VerticalLaneStuckLogic.decideWhileSwitching(
                        false,
                        false,
                        false,
                        VerticalLaneStuckLogic.SWITCH_STUCK_THRESHOLD
                )
        );
        assertEquals(
                VerticalLaneStuckLogic.Decision.CONTINUE_SIDE,
                VerticalLaneStuckLogic.decideWhileSwitching(true, false, false, 99)
        );
    }

    @Test
    void switchingPrefersHopBeforeAntiStuck() {
        assertEquals(
                VerticalLaneStuckLogic.Decision.FORWARD_HOP,
                VerticalLaneStuckLogic.decideWhileSwitching(
                        false,
                        false,
                        false,
                        true,
                        VerticalLaneStuckLogic.HOP_STUCK_THRESHOLD
                )
        );
    }

    @Test
    void yawRelockOnlyForCameraLockedPresets() {
        assertTrue(VerticalLaneStuckLogic.shouldRelockCardinalYaw(true, true));
        assertFalse(VerticalLaneStuckLogic.shouldRelockCardinalYaw(false, true));
        assertFalse(VerticalLaneStuckLogic.shouldRelockCardinalYaw(true, false));
    }
}
