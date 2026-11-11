package com.jelly.farmhelper.fabric.handler;

import com.jelly.farmhelper.fabric.util.AngleUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RotationHandler {
    public enum RotationType {
        CLIENT,
        SERVER
    }

    public enum Easing {
        LINEAR,
        EASE_OUT_CUBIC,
        EASE_OUT_QUART,
        EASE_OUT_EXPO,
        EASE_OUT_BACK
    }

    public record Rotation(float yaw, float pitch) {
    }

    public static final class RotationConfiguration {
        private final RotationType type;
        private final float targetYaw;
        private final float targetPitch;
        private final long durationMs;
        private final Easing easing;
        private final float randomYawRange;
        private final float randomPitchRange;
        private final Supplier<Vec3> followTarget;
        private final Runnable onComplete;
        private final boolean lockHeadToBody;

        private RotationConfiguration(Builder builder) {
            this.type = builder.type;
            this.targetYaw = builder.targetYaw;
            this.targetPitch = builder.targetPitch;
            this.durationMs = Math.max(1L, builder.durationMs);
            this.easing = builder.easing;
            this.randomYawRange = Math.max(0f, builder.randomYawRange);
            this.randomPitchRange = Math.max(0f, builder.randomPitchRange);
            this.followTarget = builder.followTarget;
            this.onComplete = builder.onComplete;
            this.lockHeadToBody = builder.lockHeadToBody;
        }

        public static final class Builder {
            private RotationType type = RotationType.CLIENT;
            private float targetYaw;
            private float targetPitch;
            private long durationMs = 350L;
            private Easing easing = Easing.EASE_OUT_CUBIC;
            private float randomYawRange;
            private float randomPitchRange;
            private Supplier<Vec3> followTarget;
            private Runnable onComplete;
            private boolean lockHeadToBody = true;

            public Builder target(float yaw, float pitch) {
                this.targetYaw = yaw;
                this.targetPitch = pitch;
                return this;
            }

            public Builder durationMs(long durationMs) {
                this.durationMs = durationMs;
                return this;
            }

            public Builder easing(Easing easing) {
                this.easing = Objects.requireNonNullElse(easing, Easing.EASE_OUT_CUBIC);
                return this;
            }

            public Builder type(RotationType type) {
                this.type = Objects.requireNonNullElse(type, RotationType.CLIENT);
                return this;
            }

            public Builder randomYawRange(float range) {
                this.randomYawRange = Math.max(0f, range);
                return this;
            }

            public Builder randomPitchRange(float range) {
                this.randomPitchRange = Math.max(0f, range);
                return this;
            }

            public Builder followTarget(Supplier<Vec3> followTarget) {
                this.followTarget = followTarget;
                return this;
            }

            public Builder onComplete(Runnable onComplete) {
                this.onComplete = onComplete;
                return this;
            }

            public Builder lockHeadToBody(boolean lockHeadToBody) {
                this.lockHeadToBody = lockHeadToBody;
                return this;
            }

            public RotationConfiguration build() {
                return new RotationConfiguration(this);
            }
        }
    }

    private static final RotationHandler INSTANCE = new RotationHandler();

    private RotationConfiguration activeConfig;
    private long startMs;
    private long endMs;
    private float startYaw;
    private float startPitch;
    private float targetYaw;
    private float targetPitch;
    private float serverYaw;
    private float serverPitch;
    private float clientSideYaw;
    private float clientSidePitch;
    private long lastTickMs;
    private boolean pendingServerRestore;
    private boolean rotating;

    private RotationHandler() {
    }

    public static RotationHandler getInstance() {
        return INSTANCE;
    }

    public void reset() {
        rotating = false;
        activeConfig = null;
        pendingServerRestore = false;
    }

    public boolean isRotating() {
        return rotating;
    }

    public Optional<RotationConfiguration> getActiveConfiguration() {
        return Optional.ofNullable(activeConfig);
    }

    public Rotation getServerRotation() {
        return new Rotation(serverYaw, serverPitch);
    }

    public void easeTo(float yaw, float pitch, long durationMs) {
        rotate(new RotationConfiguration.Builder()
                .target(yaw, pitch)
                .durationMs(durationMs)
                .build());
    }

    public void rotate(RotationConfiguration config) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || config == null) {
            reset();
            return;
        }

        startMs = System.currentTimeMillis();
        endMs = startMs + Math.max(1L, config.durationMs);
        lastTickMs = startMs;
        startYaw = player.getYRot();
        startPitch = player.getXRot();
        clientSideYaw = startYaw;
        clientSidePitch = startPitch;
        activeConfig = config;
        targetYaw = applyRandom(config.targetYaw, config.randomYawRange);
        targetPitch = Mth.clamp(applyRandom(config.targetPitch, config.randomPitchRange), -90f, 90f);
        pendingServerRestore = false;
        rotating = true;
    }

    public void rotateToTarget(RotationConfiguration.Builder builder, Supplier<Vec3> targetSupplier, long minMs, long maxMs) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || targetSupplier == null) {
            return;
        }
        Vec3 target = targetSupplier.get();
        if (target == null) {
            return;
        }
        Rotation needed = getNeededChange(player, target);
        float distance = (float) Math.hypot(Math.abs(needed.yaw), Math.abs(needed.pitch));
        long duration = computeDurationFromAngle(distance, minMs, maxMs);
        rotate(builder
                .target(player.getYRot() + needed.yaw, player.getXRot() + needed.pitch)
                .durationMs(duration)
                .followTarget(targetSupplier)
                .build());
    }

    public void easeBackFromServerRotation(long durationMs) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        rotate(new RotationConfiguration.Builder()
                .target(serverYaw, serverPitch)
                .durationMs(durationMs)
                .easing(Easing.EASE_OUT_QUART)
                .type(RotationType.CLIENT)
                .build());
    }

    public void tick(Minecraft client) {
        if (!rotating || activeConfig == null || client == null || client.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (client.gui.screen() != null) {
            long delta = Math.max(0L, now - lastTickMs);
            startMs += delta;
            endMs += delta;
            lastTickMs = now;
            return;
        }

        LocalPlayer player = client.player;
        if (activeConfig.followTarget != null) {
            Vec3 target = activeConfig.followTarget.get();
            if (target != null) {
                Rotation needed = getNeededChange(player, target);
                targetYaw = player.getYRot() + needed.yaw;
                targetPitch = Mth.clamp(player.getXRot() + needed.pitch, -90f, 90f);
            }
        }

        float progress = (float) (now - startMs) / Math.max(1f, (float) (endMs - startMs));
        float eased = applyEasing(activeConfig.easing, Mth.clamp(progress, 0f, 1f));
        float nextYaw = startYaw + AngleUtils.normalize180(targetYaw - startYaw) * eased;
        float nextPitch = startPitch + (targetPitch - startPitch) * eased;

        if (activeConfig.type == RotationType.CLIENT) {
            applyPlayerRotation(player, nextYaw, nextPitch, activeConfig.lockHeadToBody);
            pendingServerRestore = false;
        } else {
            clientSideYaw = player.getYRot();
            clientSidePitch = player.getXRot();
            serverYaw = nextYaw;
            serverPitch = nextPitch;
            applyPlayerRotation(player, nextYaw, nextPitch, activeConfig.lockHeadToBody);
            pendingServerRestore = true;
        }
        lastTickMs = now;

        if (progress >= 1f) {
            rotating = false;
            if (activeConfig.onComplete != null) {
                activeConfig.onComplete.run();
            }
            activeConfig = null;
            pendingServerRestore = false;
        }
    }

    public void onMotionPost(Minecraft client) {
        if (!pendingServerRestore || !rotating || activeConfig == null || activeConfig.type != RotationType.SERVER) {
            return;
        }
        if (client == null || client.player == null) {
            return;
        }
        client.player.setYRot(clientSideYaw);
        client.player.setXRot(Mth.clamp(clientSidePitch, -90f, 90f));
        pendingServerRestore = false;
    }

    private static Rotation getNeededChange(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double diffX = target.x - eye.x;
        double diffY = target.y - eye.y;
        double diffZ = target.z - eye.z;
        double horizontal = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontal));
        return new Rotation(
                AngleUtils.normalize180(yaw - player.getYRot()),
                AngleUtils.normalize180(pitch - player.getXRot())
        );
    }

    private static void applyPlayerRotation(LocalPlayer player, float yaw, float pitch, boolean lockHeadToBody) {
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90f, 90f));
        if (lockHeadToBody) {
            player.yHeadRot = yaw;
            player.yHeadRotO = yaw;
            player.yBodyRot = yaw;
            player.yBodyRotO = yaw;
        }
    }

    private static long computeDurationFromAngle(float angle, long minMs, long maxMs) {
        long min = Math.max(1L, minMs);
        long max = Math.max(min, maxMs);
        float normalized = Mth.clamp(angle / 180f, 0f, 1f);
        return min + (long) ((max - min) * normalized);
    }

    private static float applyRandom(float value, float randomRange) {
        if (randomRange <= 0f) {
            return value;
        }
        return value + ThreadLocalRandom.current().nextFloat(-randomRange, randomRange);
    }

    private static float applyEasing(Easing easing, float t) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_OUT_CUBIC -> easeOutCubic(t);
            case EASE_OUT_QUART -> easeOutQuart(t);
            case EASE_OUT_EXPO -> easeOutExpo(t);
            case EASE_OUT_BACK -> easeOutBack(t);
        };
    }

    public static float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    public static float easeOutQuart(float t) {
        return 1f - (float) Math.pow(1f - t, 4);
    }

    public static float easeOutExpo(float t) {
        return t >= 1f ? 1f : 1f - (float) Math.pow(2d, -10d * t);
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float p = t - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }
}
