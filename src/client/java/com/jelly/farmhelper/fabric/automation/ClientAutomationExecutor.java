package com.jelly.farmhelper.fabric.automation;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.FarmHelperFabricClient;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.macro.MacroState;
import com.jelly.farmhelper.fabric.runtime.ClientActionQueue;
import com.jelly.farmhelper.fabric.state.ClientModeController;
import com.jelly.farmhelper.fabric.util.InventoryUtils;
import com.jelly.farmhelper.fabric.util.KeyBindUtils;
import com.jelly.farmhelper.fabric.util.PlotUtils;
import com.jelly.farmhelper.fabric.util.RenderUtils;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ClientAutomationExecutor {
    private static final double DEFAULT_STOP_DISTANCE = 2.0;
    private static final long DEFAULT_TIMEOUT_TICKS = 200L;
    private static final int DEFAULT_RETRIES = 3;
    private static final float YAW_STEP = 12f;
    private static final float PITCH_STEP = 8f;
    private static final float PEST_YAW_STEP_MIN = 1.8f;
    private static final float PEST_YAW_STEP_MAX = 4.6f;
    private static final float PEST_PITCH_STEP_MIN = 1.0f;
    private static final float PEST_PITCH_STEP_MAX = 3.0f;
    private static final float PEST_AIM_LAG_ALPHA = 0.22f;
    private static final double PEST_AIM_JITTER = 0.04;
    private static final float PEST_DEADZONE_YAW_FALLBACK = 4.5f;
    private static final float PEST_DEADZONE_PITCH_FALLBACK = 3.0f;
    private static final long RETRY_INTERVAL_TICKS = 12L;
    private static final long DEBUG_THINK_INTERVAL_TICKS = 8L;
    private static final long FLY_PATH_PROGRESS_SAMPLE_TICKS = 12L;
    private static final int FLY_PATH_STALL_SAMPLES = 2;
    /** Altitude to cruise at when flying between barn and plots to clear all buildings. */
    private static final double PLOT_FLIGHT_CRUISE_Y = 160.0;
    /** Vertical tolerance for phase transitions during plot flight. */
    private static final double PLOT_FLIGHT_Y_TOLERANCE = 3.0;
    private static final Set<String> PEST_QUERY_TOKENS = Set.of(
            "beetle", "cricket", "earthworm", "fly", "locust", "mite",
            "mosquito", "moth", "rat", "slug", "praying mantis", "firefly", "dragonfly", "pest", "pests"
    );
    private static final Set<String> NON_PEST_TARGET_KEYWORDS = Set.of(
            "pesthunter", "philip", "visitor", "npc", "jacob"
    );
    private static final int PEST_TRACER_COLOR = 0xFF3FC4FF;
    private static final int PEST_BOX_COLOR = 0xFF56D8FF;
    private static final int PEST_TEXT_COLOR = 0xFFE8F9FF;
    private static final Vec3[] PEST_SEARCH_OFFSETS = new Vec3[] {
            new Vec3(0, 0, 0),
            new Vec3(18, 0, 0),
            new Vec3(-18, 0, 0),
            new Vec3(0, 0, 18),
            new Vec3(0, 0, -18),
            new Vec3(24, 0, 24),
            new Vec3(-24, 0, 24),
            new Vec3(24, 0, -24),
            new Vec3(-24, 0, -24)
    };

    private final PathfinderService pathfinderService = new PathfinderService();
    private final GuiDecisionEngine guiDecisionEngine = new GuiDecisionEngine();
    private final PestEntityHeuristics pestHeuristics = new PestEntityHeuristics();
    private final ClientModeController modeController = new ClientModeController();
    private final MovementRecordingPlayer movementRecordingPlayer = new MovementRecordingPlayer();

    private ActiveAction activeAction;
    private NavigationState navigationState;
    private boolean wasControllingMovement;

    public boolean isBusy() {
        return activeAction != null;
    }

    public boolean isMovementRecordingPlaying() {
        return movementRecordingPlayer.isPlaying();
    }

    public void cancelAll(Minecraft client, String reason) {
        if (activeAction != null) {
            FarmHelperFabric.getWebhookService().debugTrace(
                    "automation",
                    "cancel active action type=" + activeAction.action.type() + " reason=" + reason
            );
        }
        movementRecordingPlayer.stop(client);
        clearActive(client);
        wasControllingMovement = false;
    }

    public void tick(Minecraft client, long nowTick) {
        movementRecordingPlayer.tick(client);
        if (activeAction == null) {
            if (!movementRecordingPlayer.isPlaying() && wasControllingMovement && !isMacroDrivingMovement()) {
                stopMovement(client);
            }
            wasControllingMovement = movementRecordingPlayer.isPlaying();
            navigationState = null;
            return;
        }
        wasControllingMovement = true;
        if (client.player == null || client.level == null || client.gameMode == null) {
            clearActive(client);
            return;
        }
        if (nowTick >= activeAction.timeoutTick) {
            clearActive(client);
            return;
        }
        if (shouldSuppressRotationForAction(activeAction.action.type())) {
            FarmHelperFabric.getFailsafeManager().suppressPacketChecks(0L, 4L, 0L, "automation active");
        }

        boolean completed = switch (activeAction.action.type()) {
            case MOVE_TO_POS -> tickMoveToPos(client, activeAction, nowTick);
            case MOVE_TO_ENTITY -> tickMoveToEntity(client, activeAction, nowTick);
            case FLY_TO_POS -> tickFlyToPos(client, activeAction, nowTick);
            case FLY_TO_PLOT_CENTER -> tickFlyToPlotCenter(client, activeAction, nowTick);
            case FLY_TO_ENTITY -> tickFlyToEntity(client, activeAction, nowTick);
            case INTERACT_NEAREST_ENTITY -> tickEntityInteraction(client, activeAction, nowTick, false);
            case ATTACK_NEAREST_ENTITY -> tickEntityInteraction(client, activeAction, nowTick, true);
            case WAIT_FOR_SCREEN -> tickWaitForScreen(client, activeAction, nowTick);
            case CLICK_SLOT_MATCHING -> tickClickSlotMatching(client, activeAction, nowTick);
            case MINE_NEAREST_BLOCK -> tickMineNearestBlock(client, activeAction, nowTick);
            case ROTATE_TO -> tickRotateTo(client, activeAction, nowTick);
            default -> true;
        };

        if (completed) {
            FarmHelperFabric.getWebhookService().debugTrace(
                    "automation",
                    "completed action type=" + activeAction.action.type() + " retries=" + activeAction.retries
            );
            clearActive(client);
        }
    }

    public boolean submit(ClientActionQueue.Action action, Minecraft client, long nowTick) {
        if (requiresPlayerContext(action.type()) && (client.player == null || client.gameMode == null)) {
            return false;
        }
        if (isBusy() && requiresActiveProcessing(action.type())) {
            return false;
        }

        FarmHelperFabric.getWebhookService().debugTrace(
                "action-submit",
                "type=" + action.type() + " payload=" + action.payload()
        );

        switch (action.type()) {
            case CHAT_COMMAND -> executeChatCommand(client, action.payload());
            case CHAT_MESSAGE -> executeChatMessage(client, action.payload());
            case CLOSE_CURRENT_SCREEN -> {
                if (client.gui.screen() != null) {
                    client.player.closeContainer();
                }
            }
            case USE_HELD_ITEM -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            case SELECT_HOTBAR_ITEM -> executeSelectHotbarItem(client, action.payload());
            case SET_PERFORMANCE_MODE -> applyPerformanceMode(client, action.payload());
            case SET_PIP_MODE -> applyPipMode(client, action.payload());
            case SET_MOUSE_UNGRAB -> applyMouseUngrab(client, action.payload());
            case SET_FREELOOK_MODE -> applyFreelookMode(client, action.payload());
            case SET_USE_KEY -> applyUseKeyHold(client, Boolean.parseBoolean(action.payload()));
            case TAP_ATTACK_KEY -> KeyBindUtils.leftClick(client);
            case REQUEST_WINDOW_ATTENTION -> requestWindowAttention(action.payload());
            case ROTATE_TO -> {
                long timeout = parseTimeoutTicks(action.type(), action.payload(), 30L);
                activeAction = new ActiveAction(action, nowTick, nowTick + timeout, 0);
                navigationState = null;
            }
            case PLAY_MOVEMENT_RECORDING -> playMovementRecording(client, action.payload());
            case STOP_MOVEMENT_RECORDING -> movementRecordingPlayer.stop(client);
            case CLICK_SLOT_MATCHING, MOVE_TO_POS, MOVE_TO_ENTITY, FLY_TO_POS, FLY_TO_PLOT_CENTER, FLY_TO_ENTITY, INTERACT_NEAREST_ENTITY, ATTACK_NEAREST_ENTITY, WAIT_FOR_SCREEN, MINE_NEAREST_BLOCK -> {
                long timeout = parseTimeoutTicks(action.type(), action.payload(), DEFAULT_TIMEOUT_TICKS);
                int maxRetries = parseRetries(action.type(), action.payload(), DEFAULT_RETRIES);
                activeAction = new ActiveAction(action, nowTick, nowTick + timeout, maxRetries);
                navigationState = null;
            }
        }
        return true;
    }

    private void executeChatCommand(Minecraft client, String payload) {
        String command = payload == null ? "" : payload.trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (!command.isEmpty()) {
            client.getConnection().sendCommand(command);
            if (isTeleportLikeCommand(command)) {
                FarmHelperFabric.getFailsafeManager().suppressPacketChecks(160L, 120L, 60L, "intentional teleport command");
            } else if (command.startsWith("setmaxspeed")) {
                FarmHelperFabric.getFailsafeManager().suppressPacketChecks(0L, 40L, 20L, "setmaxspeed command");
            }
        }
    }

    private void executeChatMessage(Minecraft client, String payload) {
        String message = payload == null ? "" : payload.trim();
        if (!message.isEmpty()) {
            client.getConnection().sendChat(message);
        }
    }

    private void executeSelectHotbarItem(Minecraft client, String payload) {
        if (client.player == null) {
            return;
        }
        String query = payload == null ? "" : payload.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return;
        }
        if (query.startsWith("slot:")) {
            try {
                int slot = Integer.parseInt(query.substring("slot:".length()).trim());
                if (slot >= 0 && slot < 9) {
                    client.player.getInventory().setSelectedSlot(slot);
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        int slot = findHotbarSlot(client, query);
        if (slot >= 0) {
            client.player.getInventory().setSelectedSlot(slot);
        }
    }

    private void applyPerformanceMode(Minecraft client, String payload) {
        List<String> parts = split(payload);
        boolean enabled = !parts.isEmpty() && Boolean.parseBoolean(parts.getFirst());
        int maxFps = parts.size() >= 2 ? (int) parseDouble(parts, 1, 30) : 30;
        int viewDistance = parts.size() >= 3 ? (int) parseDouble(parts, 2, 2) : 2;
        modeController.setPerformanceMode(client, enabled, maxFps, viewDistance);
    }

    private void applyPipMode(Minecraft client, String payload) {
        boolean enabled = Boolean.parseBoolean(payload == null ? "false" : payload.trim());
        modeController.setPipMode(client, enabled);
    }

    private void applyMouseUngrab(Minecraft client, String payload) {
        boolean enabled = Boolean.parseBoolean(payload == null ? "false" : payload.trim());
        modeController.setMouseUngrab(client, enabled);
    }

    private void applyFreelookMode(Minecraft client, String payload) {
        boolean enabled = Boolean.parseBoolean(payload == null ? "false" : payload.trim());
        modeController.setFreelook(client, enabled);
    }

    private void applyUseKeyHold(Minecraft client, boolean pressed) {
        if (client == null) {
            return;
        }
        KeyBindUtils.holdUse(client, pressed);
    }

    private void playMovementRecording(Minecraft client, String payload) {
        String pattern = payload == null ? "" : payload.trim();
        if (pattern.isEmpty()) {
            return;
        }
        movementRecordingPlayer.playRandomRecording(pattern, client);
    }

    private void requestWindowAttention(String payload) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().handle();
        try {
            /* SDL: attention request unavailable */
        } catch (Throwable ignored) {
        }
        if (!isWindowFocused(handle)) {
            try {
                /* SDL: focus request unavailable */
            } catch (Throwable ignored) {
            }
        }
        if (!isWindowFocused(handle)) {
            altTabFallback();
        }
        FarmHelperFabric.getWebhookService().debugTrace(
                "failsafe",
                "requested window attention reason=" + (payload == null ? "failsafe" : payload)
        );
    }

    private boolean isWindowFocused(long handle) {
        try {
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void altTabFallback() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Robot robot = new Robot();
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            int modifier = osName.contains("mac") ? KeyEvent.VK_META : KeyEvent.VK_ALT;
            robot.keyPress(modifier);
            robot.delay(35);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(35);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.delay(35);
            robot.keyRelease(modifier);
        } catch (AWTException ignored) {
        }
    }

    private boolean tickMoveToPos(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.size() < 3) {
            return true;
        }
        double x = parseDouble(parts, 0, client.player.getX());
        double y = parseDouble(parts, 1, client.player.getY());
        double z = parseDouble(parts, 2, client.player.getZ());
        double tolerance = parts.size() >= 4 ? parseDouble(parts, 3, DEFAULT_STOP_DISTANCE) : DEFAULT_STOP_DISTANCE;
        return navigateTowards(client, action, new Vec3(x, y, z), tolerance, nowTick);
    }

    private boolean tickMoveToEntity(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        String namesCsv = parts.getFirst();
        boolean pestMode = isPestRequest(parseNames(namesCsv));
        double radius = parts.size() >= 2 ? parseDouble(parts, 1, 6.0) : 6.0;
        Entity nearest = findNearestEntity(client, namesCsv, 64.0);
        if (nearest == null) {
            stopMovement(client);
            debugThink(action, nowTick, "no entity match for move request names=" + namesCsv);
            if (pestMode && tickPestSearchFallback(client, action, nowTick)) {
                return false;
            }
            return retryOrFinish(action, nowTick);
        }
        if (pestMode) {
            renderPestTargetMarker(nearest, "Track");
        }
        return navigateTowards(client, action, entityPos(nearest), radius, nowTick);
    }

    private boolean tickFlyToPos(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.size() < 3) {
            return true;
        }
        double x = parseDouble(parts, 0, client.player.getX());
        double y = parseDouble(parts, 1, client.player.getY());
        double z = parseDouble(parts, 2, client.player.getZ());
        double tolerance = parts.size() >= 4 ? parseDouble(parts, 3, DEFAULT_STOP_DISTANCE) : DEFAULT_STOP_DISTANCE;
        return flyOrPathTowards(client, action, new Vec3(x, y, z), tolerance, nowTick);
    }

    private boolean tickFlyToPlotCenter(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        int plotNumber;
        try {
            plotNumber = Integer.parseInt(parts.getFirst());
        } catch (NumberFormatException ignored) {
            return true;
        }
        if (plotNumber < 0) {
            return true;
        }
        double tolerance = parts.size() >= 2 ? parseDouble(parts, 1, 15.0) : 15.0;
        BlockPos center = PlotUtils.getPlotCenter(plotNumber);
        double targetX = center.getX() + 0.5;
        double targetZ = center.getZ() + 0.5;
        double targetY = Math.max(80.0, center.getY());
        Vec3 player = playerPos(client);

        // Ensure player is flying before attempting any phase.
        ensureFlying(client);

        // Multi-phase flight: ascend → cruise → descend.
        // This avoids flying at y=80 straight into buildings between barn and plots.
        switch (action.plotFlightPhase) {
            case 0: // ASCEND — fly straight up to cruise altitude
                if (player.y >= PLOT_FLIGHT_CRUISE_Y - PLOT_FLIGHT_Y_TOLERANCE) {
                    action.plotFlightPhase = 1;
                    resetFlyStallTracking(action, nowTick);
                    debugThink(action, nowTick, "plot flight: ascend complete at y=" + String.format(Locale.US, "%.1f", player.y) + ", cruising to plot " + plotNumber);
                    // Fall through to cruise phase immediately.
                } else {
                    // Fly straight up. Target is directly above the player at cruise altitude.
                    Vec3 ascendTarget = new Vec3(player.x, PLOT_FLIGHT_CRUISE_Y, player.z);
                    return flyTowards(client, ascendTarget, 2.0, action, nowTick, false);
                }
                // intentional fall-through when phase just advanced
            case 1: // CRUISE — fly horizontally at cruise altitude to above plot center
                double horizontalDist = Math.sqrt(
                        (targetX - player.x) * (targetX - player.x) + (targetZ - player.z) * (targetZ - player.z)
                );
                if (horizontalDist <= tolerance) {
                    action.plotFlightPhase = 2;
                    resetFlyStallTracking(action, nowTick);
                    debugThink(action, nowTick, "plot flight: cruise complete above plot " + plotNumber + ", descending");
                    // Fall through to descend phase immediately.
                } else {
                    // Fly horizontally at cruise altitude. Keep Y high to stay above buildings.
                    Vec3 cruiseTarget = new Vec3(targetX, PLOT_FLIGHT_CRUISE_Y, targetZ);
                    // If we detect a horizontal stall during cruise, bump up further rather than
                    // falling back to the ground pathfinder (which can't handle flying).
                    if (client.player.horizontalCollision && player.y < PLOT_FLIGHT_CRUISE_Y + 10) {
                        cruiseTarget = new Vec3(targetX, PLOT_FLIGHT_CRUISE_Y + 15, targetZ);
                    }
                    return flyTowards(client, cruiseTarget, tolerance, action, nowTick, false);
                }
                // intentional fall-through when phase just advanced
            case 2: // DESCEND — fly down to the target Y at plot center
                Vec3 finalTarget = new Vec3(targetX, targetY, targetZ);
                return flyTowards(client, finalTarget, tolerance, action, nowTick, false);
            default:
                action.plotFlightPhase = 0;
                return false;
        }
    }

    private void ensureFlying(Minecraft client) {
        if (client.player != null && client.player.getAbilities().mayfly && !client.player.getAbilities().flying) {
            client.player.getAbilities().flying = true;
            client.player.onUpdateAbilities();
        }
    }

    private void resetFlyStallTracking(ActiveAction action, long nowTick) {
        action.flyLastProgressPos = null;
        action.flyLastProgressTick = nowTick;
        action.flyStallSamples = 0;
    }

    private boolean tickFlyToEntity(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        String namesCsv = parts.getFirst();
        boolean pestMode = isPestRequest(parseNames(namesCsv));
        double radius = parts.size() >= 2 ? parseDouble(parts, 1, 6.0) : 6.0;
        Entity nearest = findNearestEntity(client, namesCsv, 80.0);
        if (nearest == null) {
            stopMovement(client);
            debugThink(action, nowTick, "no entity match for fly request names=" + namesCsv);
            if (pestMode && tickPestSearchFallback(client, action, nowTick)) {
                return false;
            }
            return retryOrFinish(action, nowTick);
        }
        if (pestMode) {
            renderPestTargetMarker(nearest, "Fly");
        }
        return flyTowards(client, entityPos(nearest), radius, action, nowTick, pestMode);
    }

    private boolean tickEntityInteraction(Minecraft client, ActiveAction action, long nowTick, boolean attack) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        String namesCsv = parts.getFirst();
        List<String> requestedNames = parseNames(namesCsv);
        boolean pestMode = isPestRequest(requestedNames);
        double radius = parts.size() >= 2 ? parseDouble(parts, 1, 4.5) : 4.5;
        Entity nearest = findNearestEntity(client, namesCsv, 64.0);
        if (nearest == null) {
            stopMovement(client);
            if (attack && pestMode) {
                applyUseKeyHold(client, false);
                action.pestConsecutiveNoTargetTicks++;
                debugThink(action, nowTick, "pest target lost, scanning plot");
                if (tickPestSearchFallback(client, action, nowTick)) {
                    if (action.pestConsecutiveNoTargetTicks >= 52L) {
                        return true;
                    }
                    return false;
                }
                long probeInterval = resolvePestProbeIntervalTicks();
                if (action.pestConsecutiveNoTargetTicks >= 20L
                        && nowTick - action.pestLastTrackerProbeTick >= probeInterval) {
                    KeyBindUtils.leftClick(client);
                    action.pestLastTrackerProbeTick = nowTick;
                }
                if (action.entityInteractionHits > 0 && action.pestConsecutiveNoTargetTicks >= 24L) {
                    return true;
                }
                if (action.pestConsecutiveNoTargetTicks >= 70L) {
                    return true;
                }
            }
            return retryOrFinish(action, nowTick);
        }
        action.pestConsecutiveNoTargetTicks = 0L;
        if (pestMode) {
            renderPestTargetMarker(nearest, attack ? "Attack" : "Interact");
        }

        Vec3 targetPos = entityPos(nearest);
        if (pestMode && (client.player.getAbilities().flying || client.player.getAbilities().mayfly)) {
            if (!flyTowards(client, targetPos, Math.max(2.6, radius * 0.9), action, nowTick, true)) {
                debugThink(
                        action,
                        nowTick,
                        "flying toward pest " + nearest.getName().getString() + " dist="
                                + String.format(Locale.US, "%.2f", targetPos.distanceTo(playerPos(client)))
                );
                return false;
            }
        } else if (!navigateTowards(client, action, targetPos, radius, nowTick)) {
            return false;
        }

        if (pestMode) {
            lookAtPest(client, targetPos, action, nowTick);
        } else {
            lookAt(client, targetPos);
        }
        stopMovement(client);
        if (attack) {
            if (action.entityInteractionTargetId != nearest.getId()) {
                action.entityInteractionTargetId = nearest.getId();
                action.entityInteractionHits = 0;
                action.entityInteractionLastHitTick = nowTick - 5L;
                action.pestSmoothedTarget = targetPos;
            }
            if (pestMode) {
                applyUseKeyHold(client, true);
                action.entityInteractionHits++;
                action.pestLastSeenTick = nowTick;
                // Keep chasing/holding vacuum for a while; the caller refreshes this action periodically.
                if (nowTick - action.startTick >= 160L) {
                    return true;
                }
                return false;
            }
            if (nowTick - action.entityInteractionLastHitTick >= 5L) {
                client.gameMode.attack(client.player, nearest);
                client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                action.entityInteractionLastHitTick = nowTick;
                action.entityInteractionHits++;
            }
            return action.entityInteractionHits >= 1;
        } else {
            client.gameMode.interact(client.player, nearest, new EntityHitResult(nearest), InteractionHand.MAIN_HAND);
            client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            return true;
        }
    }

    private boolean tickWaitForScreen(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        String expected = parts.getFirst().toLowerCase(Locale.ROOT);
        if (guiDecisionEngine.isScreenTitleMatch(client, expected)) {
            return true;
        }
        return retryOrFinish(action, nowTick);
    }

    private boolean tickClickSlotMatching(Minecraft client, ActiveAction action, long nowTick) {
        if (client.gui.screen() == null || client.player == null || client.gameMode == null) {
            return retryOrFinish(action, nowTick);
        }
        String query = parseSlotQuery(action.action.payload());
        OptionalInt slotId = guiDecisionEngine.findBestSlot(client, query);
        if (slotId.isPresent()) {
            InventoryUtils.clickContainerSlot(
                    client,
                    slotId.getAsInt(),
                    InventoryUtils.ClickType.LEFT,
                    InventoryUtils.ClickMode.PICKUP
            );
            return true;
        }
        return retryOrFinish(action, nowTick);
    }

    private boolean tickMineNearestBlock(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.isEmpty()) {
            return true;
        }
        String hintsCsv = parts.getFirst();
        double radius = parts.size() >= 2 ? parseDouble(parts, 1, 4.0) : 4.0;
        List<String> hints = parseNames(hintsCsv);
        if (hints.isEmpty()) {
            return true;
        }

        if (action.miningTarget == null || !isValidMiningTarget(client, action.miningTarget, hints, radius)) {
            action.miningTarget = findNearestMatchingBlock(client, hints, radius);
            action.miningTargetSinceTick = nowTick;
            if (action.miningTarget == null) {
                stopMovement(client);
                return retryOrFinish(action, nowTick);
            }
        }

        Vec3 targetVec = Vec3.atCenterOf(action.miningTarget);
        if (!navigateTowards(client, action, targetVec, 2.1, nowTick)) {
            return false;
        }

        lookAt(client, targetVec);
        stopMovement(client);
        equipBestToolForBlock(client, action.miningTarget);
        client.gameMode.startDestroyBlock(action.miningTarget, Direction.UP);
        client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        BlockState state = client.level.getBlockState(action.miningTarget);
        if (state.isAir()) {
            return true;
        }
        if (nowTick - action.miningTargetSinceTick > 14L) {
            action.miningTarget = null;
        }
        return false;
    }

    private boolean tickRotateTo(Minecraft client, ActiveAction action, long nowTick) {
        List<String> parts = split(action.action.payload());
        if (parts.size() < 2 || client.player == null) {
            return true;
        }

        float targetYaw = (float) parseDouble(parts, 0, client.player.getYRot());
        float targetPitch = (float) parseDouble(parts, 1, client.player.getXRot());
        long durationTicks = parts.size() >= 3 ? Math.max(1L, Math.round(parseDouble(parts, 2, 12.0))) : 12L;
        long elapsed = Math.max(1L, nowTick - action.startTick + 1L);
        long remaining = Math.max(1L, durationTicks - elapsed + 1L);
        float yawDelta = Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot()));
        float pitchDelta = Math.abs(Mth.wrapDegrees(targetPitch - client.player.getXRot()));
        float yawStep = Math.max(3.5f, yawDelta / remaining);
        float pitchStep = Math.max(2.5f, pitchDelta / remaining);

        client.player.setYRot(approachAngle(client.player.getYRot(), targetYaw, yawStep));
        client.player.setXRot(Mth.clamp(approach(client.player.getXRot(), targetPitch, pitchStep), -90f, 90f));

        return Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot())) <= 1.5f
                && Math.abs(Mth.wrapDegrees(targetPitch - client.player.getXRot())) <= 1.5f;
    }

    private boolean isValidMiningTarget(Minecraft client, BlockPos pos, List<String> hints, double radius) {
        if (client.level == null || client.player == null || pos == null) {
            return false;
        }
        if (!blockMatchesHints(client.level.getBlockState(pos), hints)) {
            return false;
        }
        return Vec3.atCenterOf(pos).distanceTo(playerPos(client)) <= Math.max(1.0, radius + 0.75);
    }

    private BlockPos findNearestMatchingBlock(Minecraft client, List<String> hints, double radius) {
        if (client.level == null || client.player == null) {
            return null;
        }
        BlockPos playerPos = client.player.blockPosition();
        int r = Math.max(1, (int) Math.ceil(radius));
        double maxDistanceSq = radius * radius;
        double bestDistanceSq = Double.MAX_VALUE;
        BlockPos best = null;

        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (state.isAir() || !blockMatchesHints(state, hints)) {
                        continue;
                    }
                    double distSq = Vec3.atCenterOf(pos).distanceToSqr(playerPos(client));
                    if (distSq > maxDistanceSq) {
                        continue;
                    }
                    if (distSq < bestDistanceSq) {
                        bestDistanceSq = distSq;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean blockMatchesHints(BlockState state, List<String> hints) {
        if (state == null || state.isAir()) {
            return false;
        }
        String id = state.getBlock().toString().toLowerCase(Locale.ROOT);
        String translation = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        for (String hint : hints) {
            if (hint.isEmpty()) {
                continue;
            }
            if (id.contains(hint) || translation.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private void equipBestToolForBlock(Minecraft client, BlockPos blockPos) {
        if (client.player == null || client.level == null || blockPos == null) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.autoChooseTool) {
            return;
        }

        BlockState state = client.level.getBlockState(blockPos);
        String id = state.getBlock().toString().toLowerCase(Locale.ROOT);
        String translation = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        String toolQuery;
        if (id.contains("log") || id.contains("wood") || translation.contains("log") || translation.contains("wood")) {
            toolQuery = "treecapitator,axe";
        } else if (id.contains("stone")
                || id.contains("cobble")
                || id.contains("slab")
                || translation.contains("stone")
                || translation.contains("cobble")) {
            toolQuery = "pickaxe,stonk";
        } else {
            toolQuery = "scythe";
        }

        int slot = findHotbarSlot(client, toolQuery);
        if (slot >= 0) {
            client.player.getInventory().setSelectedSlot(slot);
        }
    }

    private int findHotbarSlot(Minecraft client, String queryCsv) {
        if (client.player == null) {
            return -1;
        }
        List<String> queries = parseNames(queryCsv);
        if (queries.isEmpty()) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            for (String query : queries) {
                if (name.contains(query)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private boolean navigateTowards(Minecraft client, ActiveAction action, Vec3 target, double stopDistance, long nowTick) {
        Vec3 playerPos = playerPos(client);
        if (playerPos.distanceTo(target) <= Math.max(0.5, stopDistance)) {
            stopMovement(client);
            navigationState = null;
            return true;
        }

        if (navigationState == null || navigationState.goal.distanceTo(target) > 1.2) {
            navigationState = new NavigationState(target);
            planPath(client, navigationState, playerPos);
        }

        if (navigationState.path.isEmpty() || navigationState.index >= navigationState.path.size()) {
            // Fallback if no path was found.
            debugThink(action, nowTick, "path empty, fallback direct move");
            return moveDirect(client, target, stopDistance, action, nowTick);
        }

        Vec3 waypoint = navigationState.path.get(navigationState.index);
        renderNavigationPathMarkers(action, navigationState, playerPos);
        if (playerPos.distanceTo(waypoint) <= 0.9) {
            navigationState.index++;
            if (navigationState.index >= navigationState.path.size()) {
                return moveDirect(client, target, stopDistance, action, nowTick);
            }
            waypoint = navigationState.path.get(navigationState.index);
        }

        if (isStuck(playerPos, nowTick)) {
            if (navigationState.replanAttempts < action.maxRetries) {
                navigationState.replanAttempts++;
                debugThink(action, nowTick, "stuck while pathing, replanning attempt=" + navigationState.replanAttempts);
                planPath(client, navigationState, playerPos);
            } else {
                debugThink(action, nowTick, "stuck after max replans, direct move fallback");
                return moveDirect(client, target, stopDistance, action, nowTick);
            }
        }
        return moveToPoint(client, waypoint, Math.max(0.8, stopDistance * 0.5));
    }

    private void renderNavigationPathMarkers(ActiveAction action, NavigationState nav, Vec3 playerPos) {
        if (action == null || nav == null || nav.path == null || nav.path.isEmpty()) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        boolean show = isDebugModeEnabled() || (action.pestAction && config.pestsTracers);
        if (!show) {
            return;
        }
        int limit = Math.min(nav.path.size(), 20);
        Vec3 prev = playerPos;
        for (int i = Math.max(0, nav.index); i < limit; i++) {
            Vec3 point = nav.path.get(i);
            int color = i == nav.index ? 0xFFF4D35E : 0xFF7AC8F7;
            AABB box = new AABB(
                    point.x - 0.20, point.y - 0.08, point.z - 0.20,
                    point.x + 0.20, point.y + 0.08, point.z + 0.20
            );
            RenderUtils.drawBox(box, color, 4L);
            RenderUtils.drawTracer(point, color, 4L);
            if (i == nav.index) {
                RenderUtils.drawText(
                        point.add(0.0, 0.30, 0.0),
                        "Path " + (nav.index + 1) + "/" + nav.path.size(),
                        0xFFFDF4C8,
                        4L
                );
            }
            if (prev != null && i > nav.index) {
                RenderUtils.drawTracer(point.lerp(prev, 0.5), 0xFF4FA7D6, 3L);
            }
            prev = point;
        }
    }

    private void planPath(Minecraft client, NavigationState nav, Vec3 start) {
        nav.path = pathfinderService.findPath(client, start, nav.goal, 1800);
        nav.index = Math.min(1, Math.max(0, nav.path.size() - 1));
        nav.lastReplanTick = nav.lastProgressTick;
        if (isDebugModeEnabled()) {
            FarmHelperFabric.getWebhookService().debugTrace(
                    "automation-path",
                    String.format(
                            Locale.US,
                            "plan path start=(%.2f,%.2f,%.2f) goal=(%.2f,%.2f,%.2f) nodes=%d",
                            start.x, start.y, start.z,
                            nav.goal.x, nav.goal.y, nav.goal.z,
                            nav.path.size()
                    )
            );
        }
    }

    private boolean moveDirect(Minecraft client, Vec3 target, double stopDistance, ActiveAction action, long nowTick) {
        if (!moveToPoint(client, target, stopDistance)) {
            return false;
        }
        if (nowTick >= action.timeoutTick) {
            return retryOrFinish(action, nowTick);
        }
        return true;
    }

    private boolean moveToPoint(Minecraft client, Vec3 target, double stopDistance) {
        Vec3 player = playerPos(client);
        double dx = target.x - player.x;
        double dy = target.y - player.y;
        double dz = target.z - player.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= Math.max(0.5, stopDistance)) {
            stopMovement(client);
            return true;
        }

        lookAt(client, target);
        client.options.keyUp.setDown(true);
        client.options.keySprint.setDown(true);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyJump.setDown(client.player.horizontalCollision || dy > 1.0);
        client.options.keyShift.setDown(false);
        return false;
    }

    private boolean flyOrPathTowards(Minecraft client, ActiveAction action, Vec3 target, double stopDistance, long nowTick) {
        boolean canFly = client.player != null
                && (client.player.getAbilities().mayfly || client.player.getAbilities().flying);
        if (shouldFallbackFlyActionToPath(client, action, nowTick)) {
            if (canFly && client.player.horizontalCollision) {
                // When flying and hitting a wall, try ascending instead of falling back to
                // a ground-level pathfinder that cannot navigate 3D airspace.
                debugThink(action, nowTick, "fly stalled on collision, ascending to clear obstacle");
                Vec3 liftedTarget = new Vec3(target.x, Math.max(target.y, client.player.getY() + 12.0), target.z);
                resetFlyStallTracking(action, nowTick);
                return flyTowards(client, liftedTarget, stopDistance, action, nowTick, false);
            }
            debugThink(action, nowTick, "fly action stalled, switching to path navigation");
            return navigateTowards(client, action, target, stopDistance, nowTick);
        }
        return flyTowards(client, target, stopDistance, action, nowTick, false);
    }

    private boolean flyTowards(Minecraft client, Vec3 target, double stopDistance, ActiveAction action, long nowTick, boolean pestMode) {
        Vec3 player = playerPos(client);
        double dx = target.x - player.x;
        double dy = target.y - player.y;
        double dz = target.z - player.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = Math.abs(dy);
        // Consider reached only when both horizontally AND vertically close.
        if (horizontalDistance <= Math.max(0.5, stopDistance) && verticalDistance <= Math.max(2.0, stopDistance)) {
            stopMovement(client);
            return true;
        }

        if (pestMode) {
            lookAtPest(client, target, action, nowTick);
        } else {
            lookAt(client, target);
        }
        boolean canFly = client.player.getAbilities().mayfly || client.player.getAbilities().flying;

        client.options.keyUp.setDown(true);
        client.options.keySprint.setDown(true);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyDown.setDown(false);
        // When flying: go up if target is above OR if hitting a wall (fly over obstacles).
        // When walking: jump if hitting a wall or target is above.
        client.options.keyJump.setDown(canFly ? (dy > 0.6 || client.player.horizontalCollision) : (client.player.horizontalCollision || dy > 1.0));
        client.options.keyShift.setDown(canFly && dy < -0.8 && !client.player.horizontalCollision);
        if (isDebugModeEnabled()) {
            RenderUtils.drawTracer(target, 0xFF9FE2FF, 4L);
        }

        if (nowTick >= action.timeoutTick) {
            return retryOrFinish(action, nowTick);
        }
        return false;
    }

    private boolean shouldFallbackFlyActionToPath(Minecraft client, ActiveAction action, long nowTick) {
        if (client == null || client.player == null || client.level == null || action == null) {
            return true;
        }
        boolean canFly = client.player.getAbilities().mayfly || client.player.getAbilities().flying;
        if (!canFly) {
            return true;
        }

        Vec3 currentPos = playerPos(client);
        if (action.flyLastProgressPos == null) {
            action.flyLastProgressPos = currentPos;
            action.flyLastProgressTick = nowTick;
            action.flyStallSamples = 0;
            return false;
        }
        if (nowTick - action.flyLastProgressTick < FLY_PATH_PROGRESS_SAMPLE_TICKS) {
            return action.flyStallSamples >= FLY_PATH_STALL_SAMPLES;
        }

        double moved = currentPos.distanceTo(action.flyLastProgressPos);
        boolean blockedAbove = !client.level.noCollision(client.player, client.player.getBoundingBox().move(0.0, 1.0, 0.0));
        boolean stalled = moved < 0.35
                && (client.player.horizontalCollision || client.player.onGround() || blockedAbove);
        action.flyLastProgressPos = currentPos;
        action.flyLastProgressTick = nowTick;
        if (stalled) {
            action.flyStallSamples++;
        } else {
            action.flyStallSamples = 0;
        }
        return action.flyStallSamples >= FLY_PATH_STALL_SAMPLES;
    }

    private boolean isStuck(Vec3 currentPos, long nowTick) {
        if (navigationState == null) {
            return false;
        }
        if (navigationState.lastProgressPos == null) {
            navigationState.lastProgressPos = currentPos;
            navigationState.lastProgressTick = nowTick;
            return false;
        }

        if (nowTick - navigationState.lastProgressTick < 20L) {
            return false;
        }

        double moved = currentPos.distanceTo(navigationState.lastProgressPos);
        navigationState.lastProgressPos = currentPos;
        navigationState.lastProgressTick = nowTick;
        return moved < 0.25;
    }

    private void lookAt(Minecraft client, Vec3 target) {
        double dx = target.x - client.player.getX();
        double dy = target.y + client.player.getEyeHeight() - client.player.getEyeY();
        double dz = target.z - client.player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance)));

        float yaw = approachAngle(client.player.getYRot(), targetYaw, YAW_STEP);
        float pitch = approach(client.player.getXRot(), targetPitch, PITCH_STEP);
        client.player.setYRot(yaw);
        client.player.setXRot(Mth.clamp(pitch, -90f, 90f));
    }

    private void lookAtPest(Minecraft client, Vec3 target, ActiveAction action, long nowTick) {
        if (client == null || client.player == null || target == null || action == null) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        float deadzoneYaw = Math.max(0.5f, config.pestsAimDeadzoneYaw <= 0f ? PEST_DEADZONE_YAW_FALLBACK : config.pestsAimDeadzoneYaw);
        float deadzonePitch = Math.max(0.5f, config.pestsAimDeadzonePitch <= 0f ? PEST_DEADZONE_PITCH_FALLBACK : config.pestsAimDeadzonePitch);

        Vec3 jittered = new Vec3(
                target.x + randomBetween(-PEST_AIM_JITTER, PEST_AIM_JITTER),
                target.y + randomBetween(-PEST_AIM_JITTER * 0.55, PEST_AIM_JITTER * 0.55),
                target.z + randomBetween(-PEST_AIM_JITTER, PEST_AIM_JITTER)
        );
        if (action.pestSmoothedTarget == null) {
            action.pestSmoothedTarget = jittered;
        } else {
            action.pestSmoothedTarget = action.pestSmoothedTarget.lerp(jittered, PEST_AIM_LAG_ALPHA);
        }

        double dx = action.pestSmoothedTarget.x - client.player.getX();
        double dy = action.pestSmoothedTarget.y + client.player.getEyeHeight() - client.player.getEyeY();
        double dz = action.pestSmoothedTarget.z - client.player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance)));

        float yawDelta = Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot()));
        float pitchDelta = Math.abs(Mth.wrapDegrees(targetPitch - client.player.getXRot()));
        if (yawDelta <= deadzoneYaw && pitchDelta <= deadzonePitch) {
            if (isDebugModeEnabled() && nowTick - action.lastThinkLogTick >= DEBUG_THINK_INTERVAL_TICKS) {
                action.lastThinkLogTick = nowTick;
                FarmHelperFabric.getWebhookService().debugTrace(
                        "automation-aim",
                        String.format(
                                Locale.US,
                                "pest in deadzone yawΔ=%.2f pitchΔ=%.2f box=%.1f/%.1f",
                                yawDelta,
                                pitchDelta,
                                deadzoneYaw,
                                deadzonePitch
                        )
                );
            }
            return;
        }

        float yawStepBase = Mth.clamp(yawDelta * 0.32f, PEST_YAW_STEP_MIN, PEST_YAW_STEP_MAX);
        float pitchStepBase = Mth.clamp(pitchDelta * 0.28f, PEST_PITCH_STEP_MIN, PEST_PITCH_STEP_MAX);
        float yawStep = Mth.clamp(
                (float) (yawStepBase + randomBetween(-0.35, 0.35)),
                PEST_YAW_STEP_MIN,
                PEST_YAW_STEP_MAX
        );
        float pitchStep = Mth.clamp(
                (float) (pitchStepBase + randomBetween(-0.25, 0.25)),
                PEST_PITCH_STEP_MIN,
                PEST_PITCH_STEP_MAX
        );
        float yaw = approachAngle(client.player.getYRot(), targetYaw, yawStep);
        float pitch = approach(client.player.getXRot(), targetPitch, pitchStep);
        client.player.setYRot(yaw);
        client.player.setXRot(Mth.clamp(pitch, -90f, 90f));

        if (isDebugModeEnabled() && nowTick - action.lastThinkLogTick >= DEBUG_THINK_INTERVAL_TICKS) {
            action.lastThinkLogTick = nowTick;
            FarmHelperFabric.getWebhookService().debugTrace(
                    "automation-aim",
                    String.format(
                            Locale.US,
                            "pest aim lag target=(%.2f,%.2f,%.2f) yawStep=%.2f pitchStep=%.2f",
                            action.pestSmoothedTarget.x,
                            action.pestSmoothedTarget.y,
                            action.pestSmoothedTarget.z,
                            yawStep,
                            pitchStep
                    )
            );
        }
    }

    private Entity findNearestEntity(Minecraft client, String namesCsv, double maxDistance) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<String> normalizedNames = parseNames(namesCsv);
        if (normalizedNames.isEmpty()) {
            return null;
        }
        boolean pestMode = isPestRequest(normalizedNames);

        double effectiveDistance = pestMode ? Math.max(maxDistance, 192.0) : maxDistance;
        double bestDistanceSq = effectiveDistance * effectiveDistance;
        Entity best = null;
        double bestPestScore = -1.0;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == null || entity == client.player) {
                continue;
            }
            if (!entity.isAlive()) {
                continue;
            }
            String entityName = entity.getName().getString().toLowerCase(Locale.ROOT);
            boolean nameMatches = normalizedNames.stream().anyMatch(entityName::contains);
            if (!nameMatches && !pestMode) {
                continue;
            }
            String typeName = entity.getType().toString().toLowerCase(Locale.ROOT);

            double distSq = entity.distanceToSqr(client.player);
            if (distSq > bestDistanceSq) {
                continue;
            }

            if (pestMode) {
                if (entity instanceof Player) {
                    continue;
                }
                // Hunt uses ESP-hardened gate + confirmed; bare bats / vanilla names never qualify.
                boolean espOk = pestHeuristics.isEspPest(entity);
                boolean confirmed = pestHeuristics.isConfirmedPest(entity);
                if (!espOk && !confirmed) {
                    continue;
                }
                if (looksLikeNonPestTarget(entityName, typeName) && !confirmed) {
                    continue;
                }
                double pestScore = pestHeuristics.score(entity);
                if (pestScore <= 0.0 && !confirmed) {
                    continue;
                }
                if (pestScore > bestPestScore || (pestScore == bestPestScore && distSq < bestDistanceSq)) {
                    bestPestScore = pestScore;
                    bestDistanceSq = distSq;
                    best = entity;
                }
            } else if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                best = entity;
            }
        }
        return best;
    }

    private boolean tickPestSearchFallback(Minecraft client, ActiveAction action, long nowTick) {
        if (client == null || client.player == null) {
            return false;
        }
        Vec3 target = computePestSearchWaypoint(client, action);
        if (target == null) {
            return false;
        }
        RenderUtils.drawTracer(target, 0xFF4BC7FF, 8L);
        RenderUtils.drawText(target.add(0.0, 0.45, 0.0), "Searching pests...", 0xFFE8F9FF, 8L);
        boolean reached = navigateTowards(client, action, target, 2.6, nowTick);
        if (reached && nowTick - action.pestSearchLastAdvanceTick >= 8L) {
            action.pestSearchWaypointIndex++;
            action.pestSearchLastAdvanceTick = nowTick;
        }
        return true;
    }

    private Vec3 computePestSearchWaypoint(Minecraft client, ActiveAction action) {
        if (client == null || client.player == null) {
            return null;
        }
        int plot = PlotUtils.getPlotNumber(client.player.blockPosition()).orElse(-1);
        if (plot <= 0) {
            return null;
        }
        BlockPos center = PlotUtils.getPlotCenter(plot);
        if (center == null) {
            return null;
        }
        int index = Math.floorMod(action.pestSearchWaypointIndex, PEST_SEARCH_OFFSETS.length);
        Vec3 offset = PEST_SEARCH_OFFSETS[index];
        return new Vec3(
                center.getX() + 0.5 + offset.x,
                client.player.getY(),
                center.getZ() + 0.5 + offset.z
        );
    }

    private static List<String> parseNames(String csv) {
        String[] names = csv.split(",");
        List<String> normalized = new ArrayList<>(names.length);
        for (String name : names) {
            String value = name.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static boolean isPestRequest(List<String> names) {
        for (String name : names) {
            if (PEST_QUERY_TOKENS.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPestAction(ClientActionQueue.Action action) {
        if (action == null) {
            return false;
        }
        return switch (action.type()) {
            case MOVE_TO_ENTITY, FLY_TO_ENTITY, INTERACT_NEAREST_ENTITY, ATTACK_NEAREST_ENTITY ->
                    isPestRequest(parseNames(parseSlotQuery(action.payload())));
            default -> false;
        };
    }

    private boolean looksLikeNonPestTarget(String entityName, String typeName) {
        for (String keyword : NON_PEST_TARGET_KEYWORDS) {
            if (entityName.contains(keyword)) {
                return true;
            }
        }
        return typeName.contains("villager")
                || typeName.contains("merchant")
                || typeName.contains("wandering_trader");
    }

    private static String parseSlotQuery(String payload) {
        List<String> parts = split(payload);
        if (parts.isEmpty()) {
            return "";
        }
        return parts.getFirst();
    }

    private Vec3 entityPos(Entity entity) {
        return new Vec3(entity.getX(), entity.getY(), entity.getZ());
    }

    private Vec3 playerPos(Minecraft client) {
        return new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
    }

    private void renderPestTargetMarker(Entity entity, String phaseLabel) {
        if (entity == null) {
            return;
        }
        Vec3 target = entityPos(entity).add(0.0, Math.max(0.4, entity.getBbHeight() * 0.5), 0.0);
        RenderUtils.drawTracer(target, PEST_TRACER_COLOR, 8L);
        RenderUtils.drawBox(entity.getBoundingBox().inflate(0.08), PEST_BOX_COLOR, 8L);
        String name = entity.getName().getString();
        String label = (name == null || name.isBlank()) ? "target" : name.trim();
        String confidence = pestHeuristics.isConfirmedPest(entity) ? "confirmed" : "esp";
        RenderUtils.drawText(
                target.add(0.0, 0.35, 0.0),
                "Pest [" + confidence + "] " + phaseLabel + ": " + label,
                PEST_TEXT_COLOR,
                8L
        );
    }

    private void stopMovement(Minecraft client) {
        if (client == null || client.options == null) {
            return;
        }
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keySprint.setDown(false);
        client.options.keyShift.setDown(false);
    }

    private void clearActive(Minecraft client) {
        if (!isMacroDrivingMovement()) {
            stopMovement(client);
        }
        wasControllingMovement = false;
        activeAction = null;
        navigationState = null;
    }

    private boolean retryOrFinish(ActiveAction action, long nowTick) {
        if (nowTick - action.lastRetryTick < RETRY_INTERVAL_TICKS) {
            return false;
        }
        action.lastRetryTick = nowTick;
        action.retries++;
        return action.retries > action.maxRetries;
    }

    private boolean requiresPlayerContext(ClientActionQueue.ActionType type) {
        return switch (type) {
            case REQUEST_WINDOW_ATTENTION -> false;
            default -> true;
        };
    }

    private boolean requiresActiveProcessing(ClientActionQueue.ActionType type) {
        return switch (type) {
            case MOVE_TO_POS, MOVE_TO_ENTITY, FLY_TO_POS, FLY_TO_PLOT_CENTER, FLY_TO_ENTITY, INTERACT_NEAREST_ENTITY, ATTACK_NEAREST_ENTITY, WAIT_FOR_SCREEN, CLICK_SLOT_MATCHING, MINE_NEAREST_BLOCK, ROTATE_TO -> true;
            default -> false;
        };
    }

    private long parseTimeoutTicks(ClientActionQueue.ActionType type, String payload, long fallback) {
        List<String> parts = split(payload);
        if (parts.isEmpty()) {
            return fallback;
        }
        String candidate = switch (type) {
            case WAIT_FOR_SCREEN, CLICK_SLOT_MATCHING -> parts.size() >= 2 ? parts.get(1) : parts.getLast();
            case ROTATE_TO -> parts.size() >= 4 ? parts.get(3) : parts.getLast();
            default -> parts.getLast();
        };
        try {
            return Math.max(1L, Long.parseLong(candidate));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseRetries(ClientActionQueue.ActionType type, String payload, int fallback) {
        List<String> parts = split(payload);
        if (parts.size() < 3 || (type != ClientActionQueue.ActionType.WAIT_FOR_SCREEN && type != ClientActionQueue.ActionType.CLICK_SLOT_MATCHING)) {
            return fallback;
        }
        try {
            return Math.max(0, Integer.parseInt(parts.get(2)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static List<String> split(String payload) {
        List<String> result = new ArrayList<>();
        if (payload == null || payload.isBlank()) {
            return result;
        }
        for (String part : payload.split("\\|")) {
            result.add(part.trim());
        }
        return result;
    }

    private double parseDouble(List<String> parts, int index, double fallback) {
        if (index < 0 || index >= parts.size()) {
            return fallback;
        }
        try {
            return Double.parseDouble(parts.get(index));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long resolvePestProbeIntervalTicks() {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        long trackerCooldownTicks = 20L;
        // Tracker ability has cooldown, so only send probe clicks at that pace.
        double cooldownSeconds = Math.max(0.2, FarmHelperFabricClient.getRuntimeSnapshot().vacuumTrackerCooldownSeconds);
        trackerCooldownTicks = Math.max(4L, Math.round(cooldownSeconds * 20.0));
        if (config.debugMode) {
            return Math.max(6L, trackerCooldownTicks);
        }
        return Math.max(10L, trackerCooldownTicks);
    }

    private boolean isDebugModeEnabled() {
        return FarmHelperFabric.getConfigManager().getConfig().debugMode;
    }

    private void debugThink(ActiveAction action, long nowTick, String message) {
        if (!isDebugModeEnabled() || action == null || message == null || message.isBlank()) {
            return;
        }
        if (nowTick - action.lastThinkLogTick < DEBUG_THINK_INTERVAL_TICKS) {
            return;
        }
        action.lastThinkLogTick = nowTick;
        FarmHelperFabric.getWebhookService().debugTrace("automation-think", message);
    }

    private boolean shouldSuppressRotationForAction(ClientActionQueue.ActionType type) {
        return switch (type) {
            case MOVE_TO_POS, MOVE_TO_ENTITY, FLY_TO_POS, FLY_TO_PLOT_CENTER, FLY_TO_ENTITY, INTERACT_NEAREST_ENTITY, ATTACK_NEAREST_ENTITY, MINE_NEAREST_BLOCK, ROTATE_TO -> true;
            default -> false;
        };
    }

    private boolean isTeleportLikeCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.startsWith("warp ")
                || normalized.startsWith("lobby")
                || normalized.startsWith("hub")
                || normalized.startsWith("is")
                || normalized.startsWith("visit")
                || normalized.startsWith("plottp")
                || normalized.startsWith("tptoplot")
                || normalized.startsWith("spawn");
    }

    private float approach(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, delta);
    }

    private float approachAngle(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, delta);
    }

    private double randomBetween(double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + Math.random() * (max - min);
    }

    private boolean isMacroDrivingMovement() {
        return FarmHelperFabric.getMacroController().isToggled()
                && FarmHelperFabric.getMacroController().getState() == MacroState.FARMING
                && !FarmHelperFabric.getFailsafeManager().hasActiveFailsafe();
    }

    private static final class ActiveAction {
        private final ClientActionQueue.Action action;
        private final long startTick;
        private final long timeoutTick;
        private final int maxRetries;
        private final boolean pestAction;
        private int retries;
        private long lastRetryTick;
        private BlockPos miningTarget;
        private long miningTargetSinceTick;
        private int entityInteractionTargetId;
        private long entityInteractionLastHitTick;
        private int entityInteractionHits;
        private long pestConsecutiveNoTargetTicks;
        private long pestLastSeenTick;
        private long pestLastTrackerProbeTick;
        private Vec3 pestSmoothedTarget;
        private int pestSearchWaypointIndex;
        private long pestSearchLastAdvanceTick;
        private Vec3 flyLastProgressPos;
        private long flyLastProgressTick;
        private int flyStallSamples;
        private int plotFlightPhase;  // 0=ascend, 1=cruise, 2=descend
        private long lastThinkLogTick;

        private ActiveAction(ClientActionQueue.Action action, long startTick, long timeoutTick, int maxRetries) {
            this.action = action;
            this.startTick = startTick;
            this.timeoutTick = timeoutTick;
            this.maxRetries = maxRetries;
            this.pestAction = isPestAction(action);
            this.retries = 0;
            this.lastRetryTick = startTick;
            this.miningTarget = null;
            this.miningTargetSinceTick = startTick;
            this.entityInteractionTargetId = Integer.MIN_VALUE;
            this.entityInteractionLastHitTick = startTick - 5L;
            this.entityInteractionHits = 0;
            this.pestConsecutiveNoTargetTicks = 0L;
            this.pestLastSeenTick = startTick;
            this.pestLastTrackerProbeTick = startTick - 40L;
            this.pestSmoothedTarget = null;
            this.pestSearchWaypointIndex = 0;
            this.pestSearchLastAdvanceTick = startTick;
            this.flyLastProgressPos = null;
            this.flyLastProgressTick = startTick;
            this.flyStallSamples = 0;
            this.plotFlightPhase = 0;
            this.lastThinkLogTick = startTick - DEBUG_THINK_INTERVAL_TICKS;
        }
    }

    private static final class NavigationState {
        private final Vec3 goal;
        private List<Vec3> path = List.of();
        private int index = 0;
        private int replanAttempts = 0;
        private long lastReplanTick = 0L;
        private Vec3 lastProgressPos;
        private long lastProgressTick = 0L;

        private NavigationState(Vec3 goal) {
            this.goal = goal;
        }
    }
}
