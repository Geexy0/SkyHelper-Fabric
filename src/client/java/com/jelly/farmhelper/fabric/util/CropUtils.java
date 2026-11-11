package com.jelly.farmhelper.fabric.util;

import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

public final class CropUtils {
    private CropUtils() {
    }

    public static boolean isCrop(BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof CropBlock
                || block instanceof CocoaBlock
                || block instanceof NetherWartBlock
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.MELON)
                || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM);
    }

    public static boolean isCropReady(BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return readIntProperty(state, NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return readIntProperty(state, CocoaBlock.AGE) >= 2;
        }
        return state.is(Blocks.CACTUS)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.MELON)
                || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM);
    }

    public static int getGrowthAge(BlockState state) {
        if (state == null) {
            return -1;
        }
        if (state.hasProperty(BlockStateProperties.AGE_7)) {
            return state.getValue(BlockStateProperties.AGE_7);
        }
        if (state.hasProperty(BlockStateProperties.AGE_3)) {
            return state.getValue(BlockStateProperties.AGE_3);
        }
        if (state.hasProperty(BlockStateProperties.AGE_2)) {
            return state.getValue(BlockStateProperties.AGE_2);
        }
        if (state.hasProperty(BlockStateProperties.AGE_15)) {
            return state.getValue(BlockStateProperties.AGE_15);
        }
        return -1;
    }

    public static AABB getExpandedHitbox(Level world, BlockPos pos, BlockState state, boolean expandedCrops, boolean expandedWarts, boolean expandedCocoa) {
        if (world == null || pos == null || state == null) {
            return AABB.unitCubeFromLowerCorner(Vec3.atCenterOf(pos)).inflate(0.5);
        }
        AABB shape = state.getShape(world, pos).bounds().move(pos);
        if (!isCrop(state)) {
            return shape;
        }
        double maxY = shape.maxY;
        if (state.getBlock() instanceof CropBlock && expandedCrops) {
            int age = getGrowthAge(state);
            maxY = pos.getY() + (0.125 + 0.875 * Math.max(0, age) / 7.0);
        } else if (state.getBlock() instanceof NetherWartBlock && expandedWarts) {
            int age = getGrowthAge(state);
            maxY = pos.getY() + (0.3125 + 0.5625 * Math.max(0, age) / 3.0);
        } else if (state.getBlock() instanceof CocoaBlock && expandedCocoa) {
            int age = getGrowthAge(state);
            maxY = pos.getY() + (0.5 + 0.25 * Math.max(0, age) / 2.0);
        }
        return new AABB(shape.minX, shape.minY, shape.minZ, shape.maxX, maxY, shape.maxZ);
    }

    private static int readIntProperty(BlockState state, IntegerProperty property) {
        if (state.hasProperty(property)) {
            return state.getValue(property);
        }
        return 0;
    }
}
