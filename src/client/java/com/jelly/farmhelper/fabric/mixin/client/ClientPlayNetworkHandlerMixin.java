package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.BlockChangeEvent;
import com.jelly.farmhelper.fabric.event.ChunkServerLoadEvent;
import com.jelly.farmhelper.fabric.event.ReceivePacketEvent;
import com.jelly.farmhelper.fabric.event.SpawnObjectEvent;
import com.jelly.farmhelper.fabric.event.SpawnParticleEvent;
import com.jelly.farmhelper.fabric.fishing.FishingBiteLatch;
import com.jelly.farmhelper.fabric.fishing.FishingBiteSignals;
import com.jelly.farmhelper.fabric.util.TablistUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void farmhelper$onPlayerPositionLook(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet == null) {
            return;
        }
        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        PositionMoveRotation current = new PositionMoveRotation(playerPos, player.getDeltaMovement(), player.getYRot(), player.getXRot());
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(current, packet.change(), packet.relatives());

        double teleportDistance = current.position().distanceTo(absolute.position());
        float yawDelta = Math.abs(Mth.wrapDegrees(absolute.yRot() - player.getYRot()));
        float pitchDelta = Math.abs(Mth.wrapDegrees(absolute.xRot() - player.getXRot()));

        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        FarmHelperFabric.getFailsafeManager().onPositionLookPacket(
                teleportDistance,
                yawDelta,
                pitchDelta,
                current.position().x,
                current.position().y,
                current.position().z,
                absolute.position().x,
                absolute.position().y,
                absolute.position().z,
                current.yRot(),
                current.xRot(),
                absolute.yRot(),
                absolute.xRot()
        );
    }

    @Inject(method = "handleRotatePlayer", at = @At("HEAD"))
    private void farmhelper$onPlayerRotation(ClientboundPlayerRotationPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet == null) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        float targetYaw = packet.relativeY() ? player.getYRot() + packet.yRot() : packet.yRot();
        float targetPitch = packet.relativeX() ? player.getXRot() + packet.xRot() : packet.xRot();
        float yawDelta = Math.abs(Mth.wrapDegrees(targetYaw - player.getYRot()));
        float pitchDelta = Math.abs(Mth.wrapDegrees(targetPitch - player.getXRot()));
        FarmHelperFabric.getFailsafeManager().onPlayerRotationPacket(
                yawDelta,
                pitchDelta,
                player.getYRot(),
                player.getXRot(),
                targetYaw,
                targetPitch
        );
    }

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void farmhelper$onEntityVelocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet == null || packet.id() != player.getId()) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        Vec3 velocity = packet.movement();
        FarmHelperFabric.getFailsafeManager().onPlayerVelocityPacket(
                velocity.y,
                velocity.length()
        );
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void farmhelper$onScreenHandlerSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet == null) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        FarmHelperFabric.getFailsafeManager().onScreenHandlerSlotPacket(
                packet.getSlot(),
                player.getInventory().getSelectedSlot(),
                packet.getItem()
        );
    }

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void farmhelper$onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        FarmHelperFabric.getFailsafeManager().onWorldTimePacket();
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"), require = 0)
    private void farmhelper$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || packet == null) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new BlockChangeEvent(
                packet.getPos(),
                world.getBlockState(packet.getPos()),
                packet.getBlockState(),
                world.dimension()
        ));
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"), require = 0)
    private void farmhelper$onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        if (packet == null) {
            return;
        }
        int x = invokeInt(packet, "getChunkX", "chunkX", "x");
        int z = invokeInt(packet, "getChunkZ", "chunkZ", "z");
        FarmHelperFabric.getEventBus().post(new ChunkServerLoadEvent(x, z));
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), require = 0)
    private void farmhelper$onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        if (packet == null) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new SpawnParticleEvent(
                packet.particle(),
                packet.overrideLimiter() || packet.alwaysShow(),
                new Vec3(packet.x(), packet.y(), packet.z()),
                new Vec3(packet.xDist(), packet.yDist(), packet.zDist()),
                packet.count()
        ));
        // Particles intentionally ignored — Crimson Isle lava spams splash/bubble.
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), require = 0)
    private void farmhelper$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (packet == null || !FarmHelperFabric.getConfigManager().getConfig().enableAutoFishing) {
            return;
        }
        String soundId = "";
        try {
            if (packet.getSound() != null && packet.getSound().value() != null
                    && packet.getSound().value().location() != null) {
                soundId = packet.getSound().value().location().toString();
            }
        } catch (Throwable ignored) {
            soundId = String.valueOf(packet.getSound());
        }
        // Sound bites disabled — cast landing splash == bite splash on Hypixel.
    }

    @Inject(method = "setTitleText", at = @At("HEAD"), require = 0)
    private void farmhelper$onTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        markBiteFromComponent(packet == null ? null : packet.text(), "title");
    }

    @Inject(method = "setSubtitleText", at = @At("HEAD"), require = 0)
    private void farmhelper$onSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        markBiteFromComponent(packet == null ? null : packet.text(), "subtitle");
    }

    @Inject(method = "setActionBarText", at = @At("HEAD"), require = 0)
    private void farmhelper$onActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        markBiteFromComponent(packet == null ? null : packet.text(), "actionbar");
    }

    private static void markBiteFromComponent(Component text, String reason) {
        if (!FarmHelperFabric.getConfigManager().getConfig().enableAutoFishing || text == null) {
            return;
        }
        String normalized = FishingBiteSignals.normalize(text.getString());
        if (FishingBiteSignals.isBiteHint(normalized)) {
            Minecraft client = Minecraft.getInstance();
            long now = client != null && client.level != null ? client.level.getGameTime() : 0L;
            FishingBiteLatch.markPopup(now, reason);
        }
    }

    @Inject(method = "handleAddEntity", at = @At("HEAD"), require = 0)
    private void farmhelper$onEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        if (packet == null) {
            return;
        }
        FarmHelperFabric.getEventBus().post(new SpawnObjectEvent(
                packet.getId(),
                String.valueOf(packet.getType()),
                new Vec3(packet.getX(), packet.getY(), packet.getZ()),
                packet.getMovement(),
                packet.getYRot(),
                packet.getXRot()
        ));
    }

    @Inject(method = "handleTabListCustomisation", at = @At("TAIL"), require = 0)
    private void farmhelper$onPlayerListHeader(ClientboundTabListPacket packet, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new ReceivePacketEvent(packet));
        if (packet == null) {
            return;
        }
        String footer = packet.footer() == null ? "" : packet.footer().getString();
        List<String> lines = footer.isBlank() ? List.of() : Arrays.asList(footer.split("\\R"));
        TablistUtils.setFooterLines(lines);
    }

    private int invokeInt(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                Object value = method.invoke(target);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }
}
