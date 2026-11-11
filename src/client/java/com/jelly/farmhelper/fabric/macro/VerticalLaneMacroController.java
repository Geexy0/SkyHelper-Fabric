package com.jelly.farmhelper.fabric.macro;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.util.AngleUtils;
import com.jelly.farmhelper.fabric.util.PlayerUtils;
import net.minecraft.client.Minecraft;

final class VerticalLaneMacroController implements LegacyMovementController {
    private final MovementMacroExecutor executor;
    private ChangeLaneDirection changeLaneDirection;
    private int stuckNoExitTicks;
    private int forwardHopTicks;

    VerticalLaneMacroController(MovementMacroExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void updateState(Minecraft client, FarmHelperConfig config, int forwardTicks, int sideTicks, boolean holdAttack) {
        MovementMacroExecutor.LegacyRouteState state = executor.getRouteState();
        if (state == null) {
            executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
            state = MovementMacroExecutor.LegacyRouteState.NONE;
        }

        float routingYaw = executor.getLaneRoutingYaw();
        MovementMacroExecutor.Walkability walkability = executor.computeWalkability(client, routingYaw);
        boolean hopAvailable = executor.hasForwardWaterOrSlabHop(client, routingYaw);
        switch (state) {
            case LEFT, RIGHT -> {
                boolean wantsRight = state == MovementMacroExecutor.LegacyRouteState.RIGHT;
                executor.setMoveRight(wantsRight);
                boolean sideWalkable = wantsRight ? walkability.right() : walkability.left();
                boolean allowBack = executor.getActiveType() != LegacyMacroType.S_CACTUS
                        && executor.getActiveType() != LegacyMacroType.S_CACTUS_SUNTZU
                        && !config.alwaysHoldW;
                boolean frontOk = walkability.front() && !config.alwaysHoldW;
                boolean backOk = walkability.back() && allowBack;
                VerticalLaneStuckLogic.Decision decision = VerticalLaneStuckLogic.decideWhileStrafing(
                        sideWalkable,
                        frontOk,
                        backOk,
                        true,
                        hopAvailable,
                        stuckNoExitTicks
                );
                if (decision == VerticalLaneStuckLogic.Decision.CONTINUE_SIDE) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.startLegacyAntiStuckPulse(config);
                    return;
                }
                if (decision == VerticalLaneStuckLogic.Decision.SWITCH_LANE) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    if (executor.isStuckInMelonsOrPumpkins(client, routingYaw)) {
                        executor.startLegacyAntiStuckPulse(config);
                        return;
                    }
                    changeLaneDirection = null;
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.SWITCHING_LANE);
                    executor.setLegacyWalkingDirection(client, routingYaw);
                    return;
                }
                if (decision == VerticalLaneStuckLogic.Decision.FORWARD_HOP) {
                    stuckNoExitTicks++;
                    forwardHopTicks = Math.max(forwardHopTicks, 4);
                    executor.setDirectionLabel("ROW_END_HOP");
                    return;
                }
                if (walkability.left()) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.LEFT);
                } else if (walkability.right()) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.RIGHT);
                } else if (decision == VerticalLaneStuckLogic.Decision.ANTI_STUCK_RESET) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    changeLaneDirection = null;
                    executor.startLegacyAntiStuckPulse(config);
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                } else {
                    stuckNoExitTicks++;
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                }
            }
            case SWITCHING_LANE -> {
                if (executor.legacyDetectLagBack(client)) {
                    return;
                }
                MovementMacroExecutor.LegacyRouteState resolved = executor.resolveRouteAfterLaneSwitch(client, walkability, false);
                boolean resolvedSide = resolved == MovementMacroExecutor.LegacyRouteState.LEFT
                        || resolved == MovementMacroExecutor.LegacyRouteState.RIGHT;
                VerticalLaneStuckLogic.Decision decision = VerticalLaneStuckLogic.decideWhileSwitching(
                        resolvedSide,
                        walkability.front(),
                        walkability.back(),
                        hopAvailable,
                        stuckNoExitTicks
                );
                if (decision == VerticalLaneStuckLogic.Decision.CONTINUE_SIDE) {
                    changeLaneDirection = null;
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.setRouteState(resolved);
                    relockCardinalYawIfNeeded(config);
                } else if (decision == VerticalLaneStuckLogic.Decision.FORWARD_HOP) {
                    stuckNoExitTicks++;
                    forwardHopTicks = Math.max(forwardHopTicks, 4);
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                    executor.setDirectionLabel("SWITCH_HOP");
                } else if (decision == VerticalLaneStuckLogic.Decision.ANTI_STUCK_RESET) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    changeLaneDirection = null;
                    executor.startLegacyAntiStuckPulse(config);
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                } else if (!walkability.front() && !walkability.back()) {
                    stuckNoExitTicks++;
                } else {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                }
            }
            case DROPPING -> {
                if (client.player == null) {
                    return;
                }
                int currentY = client.player.blockPosition().getY();
                if (client.player.onGround() && Math.abs(executor.getLegacyLayerY() - currentY) > 1.5) {
                    changeLaneDirection = null;
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    if (config.rotateAfterDrop) {
                        float rotatedYaw = AngleUtils.closestCardinal(executor.getTargetYaw() + 180f);
                        executor.setTargetYaw(rotatedYaw);
                        executor.setLaneReferenceYaw(AngleUtils.closestCardinal(rotatedYaw));
                    }
                    executor.stopAllMovement(client);
                    executor.setLegacyLayerY(currentY);
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                } else if (client.player.onGround()) {
                    stuckNoExitTicks = 0;
                    forwardHopTicks = 0;
                    executor.setLegacyLayerY(currentY);
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                } else {
                    executor.legacyScheduleNotMoving(config);
                }
            }
            case NONE -> {
                changeLaneDirection = null;
                stuckNoExitTicks = 0;
                forwardHopTicks = 0;
                executor.setRouteState(executor.calculateLegacyVerticalDirection(client));
            }
            default -> executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
        }
    }

    @Override
    public void invokeState(Minecraft client, FarmHelperConfig config, int forwardTicks, int sideTicks, boolean holdAttack) {
        MovementMacroExecutor.LegacyRouteState state = executor.getRouteState();
        if (state == null) {
            return;
        }

        float routingYaw = executor.getLaneRoutingYaw();
        MovementMacroExecutor.Walkability walkability = executor.computeWalkability(client, routingYaw);
        switch (state) {
            case LEFT -> {
                // Do NOT hold W just because front is walkable — that diagonal-drifts wheat/potato rows.
                // Forward only via alwaysHoldW, classic edge nudge, or an explicit stuck hop pulse.
                boolean forward = config.alwaysHoldW
                        || PlayerUtils.shouldWalkForwards(client)
                        || forwardHopTicks > 0;
                if (forwardHopTicks > 0) {
                    forwardHopTicks--;
                }
                executor.holdMovement(client, config, forward, false, true, false, false, holdAttack);
                executor.setDirectionLabel(forwardHopTicks > 0 ? "LEFT_HOP" : "LEFT");
            }
            case RIGHT -> {
                boolean forward = config.alwaysHoldW
                        || PlayerUtils.shouldWalkForwards(client)
                        || forwardHopTicks > 0;
                if (forwardHopTicks > 0) {
                    forwardHopTicks--;
                }
                executor.holdMovement(client, config, forward, false, false, true, false, holdAttack);
                executor.setDirectionLabel(forwardHopTicks > 0 ? "RIGHT_HOP" : "RIGHT");
            }
            case SWITCHING_LANE -> {
                if (forwardHopTicks > 0) {
                    forwardHopTicks--;
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                    executor.setLaneShiftDirection(MovementMacroExecutor.LaneShiftDirection.FORWARD);
                    executor.holdMovement(client, config, true, false, false, false, true, holdAttack);
                    executor.setDirectionLabel("SWITCH_HOP");
                    return;
                }
                if (changeLaneDirection == null) {
                    if (walkability.front() || executor.hasForwardWaterOrSlabHop(client, routingYaw)) {
                        changeLaneDirection = ChangeLaneDirection.FORWARD;
                    } else if (walkability.back()) {
                        changeLaneDirection = ChangeLaneDirection.BACKWARD;
                    } else {
                        executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                        return;
                    }
                }
                if (changeLaneDirection == ChangeLaneDirection.FORWARD) {
                    executor.setLaneShiftDirection(MovementMacroExecutor.LaneShiftDirection.FORWARD);
                    executor.holdMovement(client, config, true, false, false, false, true, holdAttack);
                    executor.setDirectionLabel("SWITCH_FWD");
                } else {
                    executor.setLaneShiftDirection(MovementMacroExecutor.LaneShiftDirection.BACKWARD);
                    executor.holdMovement(client, config, false, true, false, false, false, holdAttack);
                    executor.setDirectionLabel("SWITCH_BACK");
                }
            }
            case DROPPING -> {
                if (client.player == null) {
                    return;
                }
                int currentY = client.player.blockPosition().getY();
                if (client.player.onGround() && Math.abs(executor.getLegacyLayerY() - currentY) <= 1.5) {
                    executor.setLegacyLayerY(currentY);
                    executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
                }
            }
            case NONE -> executor.setRouteState(executor.calculateLegacyVerticalDirection(client));
            default -> executor.setRouteState(MovementMacroExecutor.LegacyRouteState.NONE);
        }
    }

    private void relockCardinalYawIfNeeded(FarmHelperConfig config) {
        LegacyMacroType type = executor.getActiveType();
        boolean cameraLocked = type != null && type.locksCameraLikeWheat();
        if (!VerticalLaneStuckLogic.shouldRelockCardinalYaw(cameraLocked, true)) {
            return;
        }
        float locked = AngleUtils.closestCardinal(
                config != null && config.customYaw ? config.customYawLevel : executor.getTargetYaw()
        );
        executor.setTargetYaw(locked);
        executor.setLaneReferenceYaw(locked);
    }

    private enum ChangeLaneDirection {
        FORWARD,
        BACKWARD
    }
}
