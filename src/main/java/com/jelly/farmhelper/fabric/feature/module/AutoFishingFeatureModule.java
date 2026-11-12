package com.jelly.farmhelper.fabric.feature.module;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.FeatureRuntimeState;
import com.jelly.farmhelper.fabric.fishing.FishingAntiDetect;
import com.jelly.farmhelper.fabric.fishing.FishingBiteLatch;
import com.jelly.farmhelper.fabric.fishing.FishingBiteSignals;
import com.jelly.farmhelper.fabric.fishing.FishingEntityHeuristics;
import com.jelly.farmhelper.fabric.fishing.FishingLineState;
import com.jelly.farmhelper.fabric.fishing.FishingWeaponMatcher;

/**
 * Auto fishing state machine: cast/wait/reel, optional sea-creature kill, trophy focus, anti-detect breaks.
 * Lava (Crimson Isle) safe: no particle bites, line-out gate, rod-use cooldown, no bobberless recast spam.
 */
public class AutoFishingFeatureModule extends MacroExclusiveFeatureModule {
    private static final int ROD_USE_COOLDOWN_TICKS = 18;

    private enum State {
        IDLE,
        CAST,
        WAIT_BITE,
        REEL,
        SEA_COMBAT,
        HUMAN_BREAK,
        TROPHY_FOCUS,
        RESUME_DELAY
    }

    private State state = State.IDLE;
    private long stateSinceTick;
    private long nextActionTick;
    private long biteDeadlineTick;
    private long lastTick;
    private boolean castPending;
    private boolean chatBiteHint;
    private final FishingAntiDetect antiDetect = new FishingAntiDetect();

    public AutoFishingFeatureModule(boolean enabled) {
        super("auto_fishing", "Auto Fishing", enabled);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset("disabled");
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        reset("disconnect");
    }

    @Override
    public void onChatMessage(String message) {
        String normalized = FishingBiteSignals.normalize(message);
        if (FishingBiteSignals.isBiteHint(normalized) && FishingLineState.isLineOut()) {
            chatBiteHint = true;
        }
        if (FishingBiteSignals.isStaffOrPauseHint(normalized)) {
            FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
            if (config.enableAutoFishing && isActionRunning()) {
                enterResumeDelay(Math.max(40L, antiDetect.sampleDelayTicks(800L, 2200L, strength(config))));
            }
        }
    }

    @Override
    public void onTick(FeatureRuntimeState runtime) {
        lastTick = runtime.tickCount;
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.enableAutoFishing) {
            if (isActionRunning()) {
                endTimedAction("auto fishing disabled");
            }
            reset("disabled");
            return;
        }
        if (!runtime.inWorld) {
            return;
        }
        if (runtime.activeFailsafe.isPresent()) {
            if (isActionRunning()) {
                endTimedAction("failsafe pause");
                reset("failsafe");
            }
            return;
        }
        if (runtime.macroToggled && state == State.IDLE) {
            return;
        }

        if (!isActionRunning()) {
            if (!beginTimedAction(runtime.tickCount, Long.MAX_VALUE / 4L, "auto fishing")) {
                return;
            }
            antiDetect.onSessionStart(runtime.tickCount);
            if (config.fishingBreaksEnabled) {
                antiDetect.scheduleNextBreak(
                        runtime.tickCount,
                        config.fishingBreakIntervalMinSeconds,
                        config.fishingBreakIntervalMaxSeconds
                );
            }
            setState(State.CAST, runtime.tickCount);
            return;
        }

        if (antiDetect.sessionExpired(runtime.tickCount, config.fishingSessionLimitMinutes)) {
            endTimedAction("session limit");
            reset("session limit");
            return;
        }

        tickState(runtime, config);
    }

    private void tickState(FeatureRuntimeState runtime, FarmHelperConfig config) {
        boolean biteNow = FishingLineState.isLineOut()
                && (runtime.fishingBiteReady || chatBiteHint || FishingBiteLatch.isActive(runtime.tickCount));
        if (biteNow && (state == State.WAIT_BITE || state == State.TROPHY_FOCUS || state == State.HUMAN_BREAK)) {
            nextActionTick = runtime.tickCount;
            setState(State.REEL, runtime.tickCount);
        }

        if (runtime.tickCount < nextActionTick) {
            return;
        }

        switch (state) {
            case CAST -> {
                if (isEntryTick(runtime.tickCount)) {
                    if (!FishingLineState.canUseRod(runtime.tickCount, ROD_USE_COOLDOWN_TICKS)) {
                        nextActionTick = runtime.tickCount + 2L;
                        return;
                    }
                    queueSelectHotbarItem("rod", runtime.tickCount);
                    float pitch = config.fishingCastPitch;
                    float yaw = runtime.yaw + (config.fishingHumanizeMouse || config.fishingAntiDetect
                            ? antiDetect.idleYawDelta(1.2f) : 0f);
                    float lookPitch = pitch + (config.fishingHumanizeMouse || config.fishingAntiDetect
                            ? antiDetect.idlePitchDelta(1.0f) : 0f);
                    long rotMs = config.fishingAntiDetect
                            ? antiDetect.rotationDurationMs(strength(config))
                            : 160L;
                    queueRotateTo(yaw, lookPitch, Math.max(2L, rotMs / 50L), runtime.tickCount);
                    long settle = config.fishingAntiDetect
                            ? antiDetect.sampleDelayTicks(100L, 240L, strength(config))
                            : 4L;
                    nextActionTick = runtime.tickCount + settle;
                    castPending = true;
                    return;
                }
                if (castPending) {
                    if (!FishingLineState.canUseRod(runtime.tickCount, ROD_USE_COOLDOWN_TICKS)) {
                        nextActionTick = runtime.tickCount + 2L;
                        return;
                    }
                    // Direct use — one cast only; mark line out before click.
                    FishingLineState.markCast(runtime.tickCount);
                    FarmHelperFabric.requestFishingReel();
                    castPending = false;
                    chatBiteHint = false;
                    long after = config.fishingAntiDetect
                            ? antiDetect.sampleDelayTicks(200L, 400L, strength(config))
                            : 8L;
                    nextActionTick = runtime.tickCount + after;
                    biteDeadlineTick = runtime.tickCount
                            + Math.max(40L, config.fishingBiteTimeoutSeconds) * 20L;
                    setState(State.WAIT_BITE, runtime.tickCount);
                    return;
                }
                setState(State.WAIT_BITE, runtime.tickCount);
            }
            case WAIT_BITE -> {
                if (!FishingLineState.isLineOut()) {
                    // Cast never landed / already reeled — recast after cooldown.
                    setState(State.CAST, runtime.tickCount);
                    return;
                }
                if (config.fishingAntiDetect && config.fishingBreaksEnabled
                        && antiDetect.shouldStartBreak(runtime.tickCount, true)) {
                    antiDetect.beginBreak(
                            runtime.tickCount,
                            config.fishingBreakLengthMinSeconds,
                            config.fishingBreakLengthMaxSeconds
                    );
                    setState(State.HUMAN_BREAK, runtime.tickCount);
                    return;
                }
                // Sea combat only after the line has been out a bit (avoid cast-frame false positives).
                if (config.fishingKillSeaCreatures
                        && runtime.fishingSeaCreatureNearby
                        && ticksInState(runtime.tickCount) > 40L) {
                    setState(State.SEA_COMBAT, runtime.tickCount);
                    return;
                }
                if (config.fishingTrophyMode && config.fishingGoldenFishPriority && runtime.fishingGoldenFishVisible) {
                    setState(State.TROPHY_FOCUS, runtime.tickCount);
                    return;
                }
                maybeIdleLook(runtime, config);
                maybeMicroMove(runtime, config);

                // Lava fishing often has no client FishingHook — do NOT recast just because bobber is missing.
                // Only recast on full bite timeout.
                if (runtime.tickCount >= biteDeadlineTick) {
                    if (FishingLineState.canUseRod(runtime.tickCount, ROD_USE_COOLDOWN_TICKS)) {
                        FishingLineState.markReel(runtime.tickCount);
                        FarmHelperFabric.requestFishingReel(); // reel empty / reset line
                    }
                    long delay = config.fishingAntiDetect
                            ? antiDetect.sampleDelayTicks(300L, 700L, strength(config))
                            : 10L;
                    nextActionTick = runtime.tickCount + delay;
                    setState(State.CAST, runtime.tickCount);
                }
            }
            case REEL -> {
                if (!FishingLineState.canUseRod(runtime.tickCount, ROD_USE_COOLDOWN_TICKS)) {
                    nextActionTick = runtime.tickCount + 2L;
                    return;
                }
                FishingLineState.markReel(runtime.tickCount);
                FarmHelperFabric.requestFishingReel();
                chatBiteHint = false;
                long delay = config.fishingAntiDetect
                        ? antiDetect.sampleDelayTicks(250L, 550L, strength(config))
                        : 10L;
                nextActionTick = runtime.tickCount + delay;
                setState(State.CAST, runtime.tickCount);
            }
            case SEA_COMBAT -> {
                if (isEntryTick(runtime.tickCount)) {
                    String target = runtime.fishingNearestSeaCreature;
                    if (target == null || target.isBlank()) {
                        target = FishingEntityHeuristics.seaCreatureNamesCsv();
                    }
                    String prefs = config.fishingWeaponPreference == null || config.fishingWeaponPreference.isBlank()
                            ? "hyperion,fire veil,frozen scythe,sword"
                            : config.fishingWeaponPreference;
                    long t = runtime.tickCount;
                    long settleSwap = config.fishingAntiDetect
                            ? antiDetect.sampleDelayTicks(80L, 200L, strength(config))
                            : 2L;
                    queueSelectHotbarItem("fishing-weapon:" + prefs, t);
                    FishingWeaponMatcher.WeaponClass predicted = FishingWeaponMatcher.classify(
                            FishingWeaponMatcher.primarySelectQuery(prefs), "");
                    long attackAt = t + settleSwap;
                    queueAttackNearestEntity(target, Math.max(4.0, config.fishingSeaCreatureRange), 80L, attackAt);
                    if (FishingWeaponMatcher.usesRightClick(predicted)
                            || prefs.toLowerCase().contains("hyperion")
                            || prefs.toLowerCase().contains("fire veil")
                            || prefs.toLowerCase().contains("frozen scythe")) {
                        queueUseHeldItem(attackAt + Math.max(2L, settleSwap));
                    }
                    long settle = Math.max(6L, config.fishingCombatResumeDelayMs / 50L);
                    if (config.fishingAntiDetect) {
                        settle += antiDetect.sampleDelayTicks(120L, 360L, strength(config));
                    }
                    nextActionTick = runtime.tickCount + settle;
                    return;
                }
                if (!runtime.fishingSeaCreatureNearby || ticksInState(runtime.tickCount) > 100L) {
                    long delay = config.fishingAntiDetect
                            ? antiDetect.sampleDelayTicks(200L, 600L, strength(config))
                            : 6L;
                    queueSelectHotbarItem("rod", runtime.tickCount);
                    nextActionTick = runtime.tickCount + delay;
                    // Line may still be out from before combat; keep waiting if so.
                    setState(FishingLineState.isLineOut() ? State.WAIT_BITE : State.CAST, runtime.tickCount);
                }
            }
            case TROPHY_FOCUS -> {
                if (runtime.fishingGoldenFishVisible) {
                    float yaw = yawTo(runtime.posX, runtime.posZ, runtime.fishingGoldenFishX, runtime.fishingGoldenFishZ);
                    float pitch = pitchTo(runtime.posX, runtime.posY + 1.6, runtime.posZ,
                            runtime.fishingGoldenFishX, runtime.fishingGoldenFishY, runtime.fishingGoldenFishZ);
                    long rotTicks = Math.max(3L, antiDetect.rotationDurationMs(strength(config)) / 50L);
                    queueRotateTo(yaw, pitch, rotTicks, runtime.tickCount);
                    nextActionTick = runtime.tickCount + rotTicks + 2L;
                    return;
                }
                setState(State.WAIT_BITE, runtime.tickCount);
            }
            case HUMAN_BREAK -> {
                if (!antiDetect.isInBreak(runtime.tickCount)) {
                    antiDetect.endBreak(
                            runtime.tickCount,
                            config.fishingBreakIntervalMinSeconds,
                            config.fishingBreakIntervalMaxSeconds
                    );
                    long resume = antiDetect.sampleDelayTicks(400L, 1200L, strength(config));
                    nextActionTick = runtime.tickCount + resume;
                    setState(State.WAIT_BITE, runtime.tickCount);
                }
            }
            case RESUME_DELAY -> {
                if (runtime.tickCount >= nextActionTick) {
                    setState(State.CAST, runtime.tickCount);
                }
            }
            case IDLE -> {
            }
        }
    }

    private void maybeIdleLook(FeatureRuntimeState runtime, FarmHelperConfig config) {
        if (!(config.fishingIdleLook || config.fishingHumanizeMouse) && !config.fishingAntiDetect) {
            return;
        }
        if (ticksInState(runtime.tickCount) % 47L != 0L) {
            return;
        }
        float yaw = runtime.yaw + antiDetect.idleYawDelta(config.fishingIdleLookAmplitude);
        float pitch = clampPitch(runtime.pitch + antiDetect.idlePitchDelta(config.fishingIdleLookAmplitude));
        long dur = Math.max(2L, antiDetect.rotationDurationMs(strength(config)) / 50L);
        queueRotateTo(yaw, pitch, dur, runtime.tickCount);
    }

    private void maybeMicroMove(FeatureRuntimeState runtime, FarmHelperConfig config) {
        if (!config.fishingMicroMove) {
            return;
        }
        if (ticksInState(runtime.tickCount) % 130L != 0L) {
            return;
        }
        float yaw = runtime.yaw + antiDetect.idleYawDelta(0.8f);
        queueRotateTo(yaw, runtime.pitch, 2L, runtime.tickCount);
    }

    private void enterResumeDelay(long delayTicks) {
        long now = Math.max(lastTick, Math.max(stateSinceTick, nextActionTick));
        nextActionTick = now + Math.max(1L, delayTicks);
        setState(State.RESUME_DELAY, now);
    }

    private void setState(State next, long nowTick) {
        state = next;
        stateSinceTick = nowTick;
    }

    private long ticksInState(long nowTick) {
        return Math.max(0L, nowTick - stateSinceTick);
    }

    private boolean isEntryTick(long nowTick) {
        return ticksInState(nowTick) <= 1L;
    }

    private void reset(String reason) {
        state = State.IDLE;
        stateSinceTick = 0L;
        nextActionTick = 0L;
        biteDeadlineTick = 0L;
        castPending = false;
        chatBiteHint = false;
        antiDetect.reset();
        FishingLineState.reset();
    }

    private static FishingAntiDetect.Strength strength(FarmHelperConfig config) {
        String raw = config.fishingAntiDetectStrength == null ? "NORMAL" : config.fishingAntiDetectStrength;
        try {
            return FishingAntiDetect.Strength.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return FishingAntiDetect.Strength.NORMAL;
        }
    }

    private static float clampPitch(float pitch) {
        if (pitch < -90f) {
            return -90f;
        }
        if (pitch > 90f) {
            return 90f;
        }
        return pitch;
    }

    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private static float pitchTo(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
    }
}
