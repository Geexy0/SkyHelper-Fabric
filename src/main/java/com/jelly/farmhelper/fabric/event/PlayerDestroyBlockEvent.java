package com.jelly.farmhelper.fabric.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

public final class PlayerDestroyBlockEvent {
    public final BlockPos pos;
    public final Direction facing;
    public final Block block;

    public PlayerDestroyBlockEvent(BlockPos pos, Direction facing, Block block) {
        this.pos = pos;
        this.facing = facing;
        this.block = block;
    }
}
