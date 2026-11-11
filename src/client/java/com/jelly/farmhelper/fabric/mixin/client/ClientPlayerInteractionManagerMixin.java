package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.ClickedBlockEvent;
import com.jelly.farmhelper.fabric.event.PlayerDestroyBlockEvent;
import com.jelly.farmhelper.fabric.event.WindowClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    private static BlockPos farmhelper$lastAttackPos;
    private static Direction farmhelper$lastAttackFacing = Direction.UP;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), require = 0)
    private void farmhelper$onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || pos == null || direction == null) {
            return;
        }
        farmhelper$lastAttackPos = pos.immutable();
        farmhelper$lastAttackFacing = direction;
        Block block = client.level.getBlockState(pos).getBlock();
        FarmHelperFabric.getEventBus().post(new ClickedBlockEvent(pos.immutable(), direction, block));
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void farmhelper$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || pos == null) {
            return;
        }
        Block block = client.level.getBlockState(pos).getBlock();
        Direction direction = resolveBreakDirection(client, pos);
        FarmHelperFabric.getEventBus().post(new PlayerDestroyBlockEvent(pos.immutable(), direction, block));
    }

    private Direction resolveBreakDirection(Minecraft client, BlockPos pos) {
        if (client.hitResult instanceof BlockHitResult hitResult
                && pos.equals(hitResult.getBlockPos())) {
            return hitResult.getDirection();
        }
        if (farmhelper$lastAttackPos != null && farmhelper$lastAttackPos.equals(pos)) {
            return farmhelper$lastAttackFacing;
        }
        return Direction.UP;
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), require = 0)
    private void farmhelper$onWindowClick(int syncId, int slotId, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        FarmHelperFabric.getEventBus().post(new WindowClickEvent(
                syncId,
                slotId,
                button,
                actionType,
                System.currentTimeMillis()
        ));
    }
}
