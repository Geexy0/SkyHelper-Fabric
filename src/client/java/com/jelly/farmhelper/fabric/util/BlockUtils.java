package com.jelly.farmhelper.fabric.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockUtils {
    private BlockUtils() {
    }

    public static float unitX(float yawDegrees) {
        float yaw = AngleUtils.normalize360(yawDegrees);
        if (yaw < 30.0f) {
            return 0.0f;
        }
        if (yaw < 150.0f) {
            return -1.0f;
        }
        if (yaw < 210.0f) {
            return 0.0f;
        }
        if (yaw < 330.0f) {
            return 1.0f;
        }
        return 0.0f;
    }

    public static float unitZ(float yawDegrees) {
        float yaw = AngleUtils.normalize360(yawDegrees);
        if (yaw < 60.0f) {
            return 1.0f;
        }
        if (yaw < 120.0f) {
            return 0.0f;
        }
        if (yaw < 240.0f) {
            return -1.0f;
        }
        if (yaw < 300.0f) {
            return 0.0f;
        }
        return 1.0f;
    }

    public static BlockPos relativeBlockPos(LocalPlayer player, float x, float y, float z, float yaw) {
        if (player == null) {
            return BlockPos.ZERO;
        }
        return BlockPos.containing(
                player.getX() + unitX(yaw) * z + unitZ(yaw) * -x,
                player.getY() + y,
                player.getZ() + unitZ(yaw) * z + unitX(yaw) * x
        );
    }

    public static BlockPos relativeBlockPos(LocalPlayer player, float x, float y, float z) {
        return relativeBlockPos(player, x, y, z, player == null ? 0.0f : player.getYRot());
    }

    public static BlockState getBlockState(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return client.level.getBlockState(pos);
    }

    public static Block getBlock(Minecraft client, BlockPos pos) {
        return getBlockState(client, pos).getBlock();
    }

    public static boolean canWalkThrough(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return false;
        }
        BlockState feet = client.level.getBlockState(pos);
        BlockState head = client.level.getBlockState(pos.above());
        boolean feetOpen = feet.isAir() || feet.getCollisionShape(client.level, pos).isEmpty();
        boolean headOpen = head.isAir() || head.getCollisionShape(client.level, pos.above()).isEmpty();
        return feetOpen && headOpen;
    }

    public static boolean hasNearbyBlock(Minecraft client, BlockPos center, Predicate<BlockState> predicate, int radius) {
        return countNearbyBlocks(client, center, predicate, radius, radius, radius) > 0;
    }

    public static int countNearbyBlocks(
            Minecraft client,
            BlockPos center,
            Predicate<BlockState> predicate,
            int radiusX,
            int radiusY,
            int radiusZ
    ) {
        if (client == null || client.level == null || center == null || predicate == null) {
            return 0;
        }
        int count = 0;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (predicate.test(client.level.getBlockState(pos))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static List<BlockPos> findNearbyBlocks(
            Minecraft client,
            BlockPos center,
            Predicate<BlockState> predicate,
            int radius
    ) {
        List<BlockPos> found = new ArrayList<>();
        if (client == null || client.level == null || center == null || predicate == null) {
            return found;
        }
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (predicate.test(client.level.getBlockState(pos))) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        return found;
    }

    public static BlockPos nearestMatching(
            Minecraft client,
            Vec3 source,
            Predicate<BlockState> predicate,
            int radius
    ) {
        if (client == null || source == null) {
            return null;
        }
        BlockPos center = BlockPos.containing(source);
        BlockPos best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (BlockPos pos : findNearbyBlocks(client, center, predicate, radius)) {
            double distanceSq = source.distanceToSqr(Vec3.atCenterOf(pos));
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = pos;
            }
        }
        return best;
    }

    public static boolean isDirectionWalkable(Minecraft client, BlockPos origin, Direction direction) {
        if (origin == null || direction == null) {
            return false;
        }
        return canWalkThrough(client, origin.relative(direction));
    }

    public static boolean blockHasCollision(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return false;
        }
        return !client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty();
    }

    public static boolean isAboveHeadClear(Minecraft client, LocalPlayer player, int maxHeight) {
        if (client == null || client.level == null || player == null) {
            return false;
        }
        BlockPos start = player.blockPosition().above();
        int top = Math.min(start.getY() + Math.max(1, maxHeight), client.level.getMaxY());
        for (int y = start.getY(); y <= top; y++) {
            if (blockHasCollision(client, new BlockPos(start.getX(), y, start.getZ()))) {
                return false;
            }
        }
        return true;
    }

    public static int bedrockCount(Minecraft client, LocalPlayer player, int radiusX, int radiusY, int radiusZ) {
        if (client == null || client.level == null || player == null) {
            return 0;
        }
        return countNearbyBlocks(
                client,
                player.blockPosition(),
                state -> state.is(Blocks.BEDROCK),
                Math.max(1, radiusX),
                Math.max(1, radiusY),
                Math.max(1, radiusZ)
        );
    }

    public static BlockPos getBlockPosLookingAt(Minecraft client, double maxDistance, boolean includeFluids) {
        if (client == null || client.level == null || client.player == null || client.gameRenderer == null) {
            return null;
        }
        Vec3 start = client.player.getEyePosition();
        Vec3 end = start.add(client.player.getViewVector(1.0f).scale(Math.max(1.0, maxDistance)));
        BlockHitResult hit = rayTraceBlocks(client, start, end, includeFluids);
        return hit == null ? null : hit.getBlockPos().immutable();
    }

    public static BlockHitResult rayTraceBlocks(Minecraft client, Vec3 from, Vec3 to, boolean includeFluids) {
        if (client == null || client.level == null || from == null || to == null) {
            return null;
        }
        HitResult result = client.level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                client.player
        ));
        if (result instanceof BlockHitResult blockHitResult) {
            return blockHitResult;
        }
        return null;
    }

    public static boolean canBlockBeSeen(Minecraft client, LocalPlayer player, BlockPos pos, double maxDistance) {
        if (client == null || client.level == null || player == null || pos == null) {
            return false;
        }
        Vec3 start = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);
        if (start.distanceToSqr(target) > maxDistance * maxDistance) {
            return false;
        }
        BlockHitResult hit = rayTraceBlocks(client, start, target, true);
        return hit != null && pos.equals(hit.getBlockPos());
    }

    public static int cropAroundAmount(Minecraft client, BlockPos center, int radius) {
        return countNearbyBlocks(client, center, CropUtils::isCrop, radius, radius, radius);
    }

    public static List<BlockPos> getBlocksInBox(AABB box) {
        List<BlockPos> positions = new ArrayList<>();
        if (box == null) {
            return positions;
        }
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    public static double horizontalDistanceSq(Vec3 a, Vec3 b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
