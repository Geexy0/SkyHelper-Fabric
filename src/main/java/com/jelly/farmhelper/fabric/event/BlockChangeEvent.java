package com.jelly.farmhelper.fabric.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockChangeEvent {
    public final BlockPos pos;
    public final BlockState oldState;
    public final BlockState newState;
    public final ResourceKey<Level> worldKey;

    public BlockChangeEvent(BlockPos pos, BlockState oldState, BlockState newState, ResourceKey<Level> worldKey) {
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.oldState = oldState;
        this.newState = newState;
        this.worldKey = worldKey;
    }
}
