package com.jelly.farmhelper.fabric.macro;

/**
 * Pure stuck / lane-exit decisions for vertical S-shape controllers (no Minecraft types).
 */
public final class VerticalLaneStuckLogic {
    public static final int ROW_END_STUCK_THRESHOLD = 8;
    public static final int SWITCH_STUCK_THRESHOLD = 6;

    public enum Decision {
        CONTINUE_SIDE,
        SWITCH_LANE,
        ANTI_STUCK_RESET,
        RECALCULATE
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
        if (sideWalkable) {
            return Decision.CONTINUE_SIDE;
        }
        if (frontWalkable || (allowBack && backWalkable)) {
            return Decision.SWITCH_LANE;
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
        if (resolvedSide) {
            return Decision.CONTINUE_SIDE;
        }
        if (!frontWalkable && !backWalkable && stuckNoExitTicks >= SWITCH_STUCK_THRESHOLD) {
            return Decision.ANTI_STUCK_RESET;
        }
        return Decision.RECALCULATE;
    }
}
