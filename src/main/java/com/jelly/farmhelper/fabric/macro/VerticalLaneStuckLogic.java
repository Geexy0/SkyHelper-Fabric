package com.jelly.farmhelper.fabric.macro;

/**
 * Pure stuck / lane-exit decisions for vertical S-shape controllers (no Minecraft types).
 */
public final class VerticalLaneStuckLogic {
    public static final int ROW_END_STUCK_THRESHOLD = 8;
    public static final int SWITCH_STUCK_THRESHOLD = 6;
    public static final int HOP_STUCK_THRESHOLD = 4;

    public enum Decision {
        CONTINUE_SIDE,
        SWITCH_LANE,
        ANTI_STUCK_RESET,
        RECALCULATE,
        FORWARD_HOP
    }

    private VerticalLaneStuckLogic() {
    }

    public static Decision decideWhileStrafing(
            boolean sideWalkable,
            boolean frontWalkable,
            boolean backWalkable,
            boolean allowBack,
            int stuckNoExitTicks
    ) {
        return decideWhileStrafing(sideWalkable, frontWalkable, backWalkable, allowBack, false, stuckNoExitTicks);
    }

    public static Decision decideWhileStrafing(
            boolean sideWalkable,
            boolean frontWalkable,
            boolean backWalkable,
            boolean allowBack,
            boolean forwardHopAvailable,
            int stuckNoExitTicks
    ) {
        if (sideWalkable) {
            return Decision.CONTINUE_SIDE;
        }
        if (frontWalkable || (allowBack && backWalkable)) {
            return Decision.SWITCH_LANE;
        }
        // Row-end water/slab: prefer a short forward hop before full anti-stuck reset.
        if (forwardHopAvailable && stuckNoExitTicks >= HOP_STUCK_THRESHOLD
                && stuckNoExitTicks < ROW_END_STUCK_THRESHOLD) {
            return Decision.FORWARD_HOP;
        }
        if (stuckNoExitTicks >= ROW_END_STUCK_THRESHOLD) {
            return Decision.ANTI_STUCK_RESET;
        }
        return Decision.RECALCULATE;
    }

    public static Decision decideWhileSwitching(
            boolean resolvedSide,
            boolean frontWalkable,
            boolean backWalkable,
            int stuckNoExitTicks
    ) {
        return decideWhileSwitching(resolvedSide, frontWalkable, backWalkable, false, stuckNoExitTicks);
    }

    public static Decision decideWhileSwitching(
            boolean resolvedSide,
            boolean frontWalkable,
            boolean backWalkable,
            boolean forwardHopAvailable,
            int stuckNoExitTicks
    ) {
        if (resolvedSide) {
            return Decision.CONTINUE_SIDE;
        }
        if (!frontWalkable && !backWalkable) {
            if (forwardHopAvailable && stuckNoExitTicks >= HOP_STUCK_THRESHOLD
                    && stuckNoExitTicks < SWITCH_STUCK_THRESHOLD) {
                return Decision.FORWARD_HOP;
            }
            if (stuckNoExitTicks >= SWITCH_STUCK_THRESHOLD) {
                return Decision.ANTI_STUCK_RESET;
            }
        }
        return Decision.RECALCULATE;
    }

    /**
     * After a lane switch resolves, camera-locked presets should snap lane yaw to the nearest cardinal.
     */
    public static boolean shouldRelockCardinalYaw(boolean cameraLockedPreset, boolean resolvedSide) {
        return cameraLockedPreset && resolvedSide;
    }

    public enum StartSide {
        RIGHT,
        LEFT,
        DEFER
    }

    /**
     * Immediate open-side pick before wall-distance scan.
     * Only decides when exactly one side is walkable; otherwise defer so the nearer-wall
     * scan can send the farmer to the opposite side of that wall.
     */
    public static StartSide preferStartSide(
            boolean cameraLockedCrop,
            boolean rightWalkable,
            boolean leftWalkable
    ) {
        if (rightWalkable && !leftWalkable) {
            return StartSide.RIGHT;
        }
        if (leftWalkable && !rightWalkable) {
            return StartSide.LEFT;
        }
        return StartSide.DEFER;
    }

    /**
     * Nearer obstacle/wall on {@code wallOnRight} → start the other way.
     */
    public static StartSide oppositeOfNearerWall(boolean wallOnRight) {
        return wallOnRight ? StartSide.LEFT : StartSide.RIGHT;
    }
}
