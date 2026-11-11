package com.jelly.farmhelper.fabric.util;

import net.minecraft.tags.ItemTags;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.config.struct.RewarpPoint;
import com.jelly.farmhelper.fabric.macro.LegacyMacroType;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class PlayerUtils {
    private static final Pattern SKYBLOCK_ID_PATTERN = Pattern.compile("id[=:]\"?([A-Z0-9_]{5,})");
    private static final Pattern SKYBLOCK_TIER_PATTERN = Pattern.compile("_(\\d+)$");
    private static final Pattern VACUUM_DAMAGE_PATTERN = Pattern.compile("deals\\s*([0-9,.]+)\\s*.*damage\\s*per\\s*second", Pattern.CASE_INSENSITIVE);
    private static final Pattern VACUUM_COOLDOWN_PATTERN = Pattern.compile("cooldown\\s*:?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*s", Pattern.CASE_INSENSITIVE);

    private PlayerUtils() {
    }

    public enum FarmingCrop {
        WHEAT,
        CARROT,
        POTATO,
        NETHER_WART,
        SUGAR_CANE,
        COCOA_BEANS,
        MELON,
        PUMPKIN,
        MUSHROOM,
        CACTUS,
        ROSE,
        SUNFLOWER,
        NONE
    }

    public record VacuumStats(double range, double dps, double trackerCooldownSeconds, String source) {
        public static VacuumStats empty() {
            return new VacuumStats(0.0, 0.0, 1.0, "");
        }
    }

    public static boolean isInventoryEmpty(LocalPlayer player) {
        if (player == null) {
            return true;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isInventoryFull(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSpawnLocationSet(FarmHelperConfig config) {
        return config != null && config.spawnPosY > 0;
    }

    public static BlockPos getConfiguredSpawn(FarmHelperConfig config) {
        if (!isSpawnLocationSet(config)) {
            return null;
        }
        return new BlockPos(config.spawnPosX, config.spawnPosY, config.spawnPosZ);
    }

    public static boolean isNearSpawn(LocalPlayer player, FarmHelperConfig config, int xzTolerance, int yTolerance) {
        if (player == null || !isSpawnLocationSet(config)) {
            return false;
        }
        BlockPos playerPos = player.blockPosition();
        return Math.abs(playerPos.getX() - config.spawnPosX) <= Math.max(0, xzTolerance)
                && Math.abs(playerPos.getY() - config.spawnPosY) <= Math.max(0, yTolerance)
                && Math.abs(playerPos.getZ() - config.spawnPosZ) <= Math.max(0, xzTolerance);
    }

    public static Optional<RewarpPoint> nearestRewarpPoint(LocalPlayer player, FarmHelperConfig config) {
        if (player == null || config == null || config.rewarpPoints == null || config.rewarpPoints.isEmpty()) {
            return Optional.empty();
        }
        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());
        return config.rewarpPoints.stream()
                .filter(point -> point != null)
                .min(Comparator.comparingDouble(point -> pos.distanceToSqr(point.x + 0.5, point.y + 0.5, point.z + 0.5)));
    }

    public static boolean isNearRewarpPoint(LocalPlayer player, FarmHelperConfig config, int radius) {
        if (player == null) {
            return false;
        }
        int r = Math.max(1, radius);
        BlockPos pos = player.blockPosition();
        return nearestRewarpPoint(player, config)
                .map(point -> Math.abs(pos.getX() - point.x) <= r
                        && Math.abs(pos.getY() - point.y) <= Math.max(2, r)
                        && Math.abs(pos.getZ() - point.z) <= r)
                .orElse(false);
    }

    public static int selectBestToolForMacro(Minecraft client, LegacyMacroType macroType) {
        if (client == null || client.player == null) {
            return -1;
        }
        LocalPlayer player = client.player;
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String displayName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String itemId = extractSkyblockItemId(stack);
            String metadata = (displayName + " " + stack.getItem() + " " + stack.getComponents()).toLowerCase(Locale.ROOT);
            int score = scoreToolForType(macroType, stack, metadata, itemId);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0 && bestScore > 0) {
            player.getInventory().setSelectedSlot(bestSlot);
            return bestSlot;
        }
        return -1;
    }

    public static double resolveBestVacuumRange(Minecraft client) {
        return resolveBestVacuumStats(client).range();
    }

    public static VacuumStats resolveBestVacuumStats(Minecraft client) {
        if (client == null || client.player == null) {
            return VacuumStats.empty();
        }

        LocalPlayer player = client.player;
        VacuumStats selected = resolveVacuumStats(player.getMainHandItem(), player);
        if (selected.range() > 0.0) {
            return selected;
        }

        VacuumStats best = VacuumStats.empty();
        for (int slot = 0; slot < 9; slot++) {
            VacuumStats stats = resolveVacuumStats(player.getInventory().getItem(slot), player);
            if (stats.range() > best.range()) {
                best = stats;
            }
        }
        if (best.range() > 0.0) {
            return best;
        }

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            VacuumStats stats = resolveVacuumStats(stack, player);
            if (stats.range() > best.range()) {
                best = stats;
            }
        }
        return best;
    }

    public static double resolveVacuumRange(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        return resolveVacuumStats(stack, client == null ? null : client.player).range();
    }

    public static VacuumStats resolveVacuumStats(ItemStack stack, LocalPlayer player) {
        if (stack == null || stack.isEmpty()) {
            return VacuumStats.empty();
        }
        String itemId = extractSkyblockItemId(stack);
        String metadata = (stack.getHoverName().getString() + " " + stack.getItem() + " " + stack.getComponents())
                .toLowerCase(Locale.ROOT);
        if (!metadata.contains("vacuum") && !itemId.contains("VACUUM")) {
            return VacuumStats.empty();
        }
        double range = resolveVacuumRangeFromTier(metadata, itemId);
        double dps = resolveVacuumDpsFromTooltip(stack, player);
        double cooldown = resolveVacuumCooldownFromTooltip(stack, player);
        return new VacuumStats(range, dps, cooldown, stack.getHoverName().getString());
    }

    private static double resolveVacuumRangeFromTier(String metadata, String itemId) {
        if (metadata.contains("hooverius") || itemId.contains("HOOVERIUS")) {
            return 15.0;
        }
        if (metadata.contains("infinivacuum") || itemId.contains("INFINIVACUUM")) {
            return 12.5;
        }
        if (metadata.contains("hyper vacuum") || itemId.contains("HYPER_VACUUM")) {
            return 10.0;
        }
        if (metadata.contains("turbo vacuum") || itemId.contains("TURBO_VACUUM")) {
            return 7.5;
        }
        if (metadata.contains("skymart vacuum") || itemId.contains("SKYMART_VACUUM")) {
            return 5.0;
        }
        return 5.0;
    }

    private static double resolveVacuumDpsFromTooltip(ItemStack stack, LocalPlayer player) {
        String dump = buildTooltipDump(stack, player);
        if (dump.isEmpty()) {
            return 0.0;
        }
        Matcher matcher = VACUUM_DAMAGE_PATTERN.matcher(stripColorCodes(dump));
        if (!matcher.find()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static double resolveVacuumCooldownFromTooltip(ItemStack stack, LocalPlayer player) {
        String dump = buildTooltipDump(stack, player);
        if (dump.isEmpty()) {
            return 1.0;
        }
        Matcher matcher = VACUUM_COOLDOWN_PATTERN.matcher(stripColorCodes(dump));
        if (!matcher.find()) {
            return 1.0;
        }
        try {
            return Math.max(0.2, Double.parseDouble(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    private static String buildTooltipDump(ItemStack stack, LocalPlayer player) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Component> tooltip = stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.NORMAL);
            for (Component line : tooltip) {
                if (line == null) {
                    continue;
                }
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(line.getString());
            }
        } catch (Throwable ignored) {
        }
        if (sb.isEmpty()) {
            for (String loreLine : InventoryUtils.getItemLore(stack)) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(loreLine);
            }
        }
        return sb.toString();
    }

    private static String stripColorCodes(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replaceAll("§.", "");
    }

    public static boolean canBreakBlock(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return false;
        }
        Block block = client.level.getBlockState(pos).getBlock();
        return block.defaultBlockState().getDestroySpeed(client.level, pos) >= 0.0f;
    }

    public static FarmingCrop getCropBasedOnMouseOver(Minecraft client) {
        if (client == null || client.level == null || !(client.hitResult instanceof BlockHitResult hitResult)) {
            return FarmingCrop.NONE;
        }
        return fromBlockState(client.level.getBlockState(hitResult.getBlockPos()));
    }

    public static FarmingCrop getFarmingCrop(Minecraft client) {
        FarmingCrop hovered = getCropBasedOnMouseOver(client);
        if (hovered != FarmingCrop.NONE) {
            return hovered;
        }
        if (client == null || client.level == null || client.player == null) {
            return FarmingCrop.NONE;
        }
        BlockPos center = client.player.blockPosition();
        Vec3 playerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        FarmingCrop best = FarmingCrop.NONE;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    FarmingCrop crop = fromBlockState(state);
                    if (crop == FarmingCrop.NONE) {
                        continue;
                    }
                    double distanceSq = Vec3.atCenterOf(pos).distanceToSqr(playerPos);
                    if (distanceSq < bestDistanceSq) {
                        bestDistanceSq = distanceSq;
                        best = crop;
                    }
                }
            }
        }
        return best;
    }

    public static boolean shouldPushBack(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        LocalPlayer player = client.player;
        float yaw = AngleUtils.closestCardinal(player.getYRot());
        BlockPos behind = BlockUtils.relativeBlockPos(player, 0, 0, -1, yaw);
        BlockPos ahead = BlockUtils.relativeBlockPos(player, 0, 0, 1, yaw);
        boolean blockedBehind = !BlockUtils.canWalkThrough(client, behind);
        boolean blockedAhead = !BlockUtils.canWalkThrough(client, ahead);
        return blockedBehind && !blockedAhead;
    }

    public static boolean shouldWalkForwards(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        if (FarmHelperFabric.getFailsafeManager().hasActiveFailsafe()) {
            return false;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        LegacyMacroType macroType = config == null || config.macroType == null
                ? LegacyMacroType.S_V_NORMAL_TYPE
                : config.macroType;
        if (macroType == LegacyMacroType.S_CACTUS
                || macroType == LegacyMacroType.S_CACTUS_SUNTZU
                || macroType == LegacyMacroType.S_PUMPKIN_MELON
                || macroType == LegacyMacroType.S_PUMPKIN_MELON_DEFAULT_PLOT
                || macroType == LegacyMacroType.S_COCOA_BEANS_LEFT_RIGHT) {
            return false;
        }

        LocalPlayer player = client.player;
        float angle = AngleUtils.closestCardinal(player.getYRot());
        float yaw = config != null && config.customYaw
                ? AngleUtils.closestCardinal(config.customYawLevel)
                : angle;
        BlockPos ahead = BlockUtils.relativeBlockPos(player, 0, 0, 1, yaw);
        if (BlockUtils.canWalkThrough(client, ahead)) {
            return false;
        }

        double x = player.getX() % 1.0;
        double z = player.getZ() % 1.0;
        if (angle == 0.0f) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 90.0f) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        } else if (angle == 180.0f) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 270.0f) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        }
        return false;
    }

    public static boolean isPlayerSuffocating(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        BlockPos head = client.player.blockPosition().above();
        return BlockUtils.blockHasCollision(client, head) || client.player.isInWall();
    }

    public static boolean isInBarn(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        Optional<Integer> plot = PlotUtils.getPlotNumber(client.player.blockPosition());
        return plot.isPresent() && plot.get() == 0;
    }

    public static void setSpawnLocation(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        BlockPos pos = client.player.blockPosition();
        config.spawnPosX = pos.getX();
        config.spawnPosY = pos.getY();
        config.spawnPosZ = pos.getZ();
        config.spawnYaw = client.player.getYRot();
        config.spawnPitch = client.player.getXRot();
        config.spawnPlot = PlotUtils.getPlotNumber(pos).orElse(0);
        FarmHelperFabric.getConfigManager().save();
    }

    public static Vec3 getSpawnLocation(FarmHelperConfig config) {
        if (!isSpawnLocationSet(config)) {
            return null;
        }
        return new Vec3(config.spawnPosX + 0.5, config.spawnPosY + 0.5, config.spawnPosZ + 0.5);
    }

    private static FarmingCrop fromBlockState(BlockState state) {
        if (state == null) {
            return FarmingCrop.NONE;
        }
        if (state.is(Blocks.WHEAT)) return FarmingCrop.WHEAT;
        if (state.is(Blocks.CARROTS)) return FarmingCrop.CARROT;
        if (state.is(Blocks.POTATOES)) return FarmingCrop.POTATO;
        if (state.is(Blocks.NETHER_WART)) return FarmingCrop.NETHER_WART;
        if (state.is(Blocks.SUGAR_CANE)) return FarmingCrop.SUGAR_CANE;
        if (state.is(Blocks.COCOA)) return FarmingCrop.COCOA_BEANS;
        if (state.is(Blocks.MELON)) return FarmingCrop.MELON;
        if (state.is(Blocks.PUMPKIN)) return FarmingCrop.PUMPKIN;
        if (state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)) return FarmingCrop.MUSHROOM;
        if (state.is(Blocks.CACTUS)) return FarmingCrop.CACTUS;
        if (state.is(Blocks.ROSE_BUSH)) return FarmingCrop.ROSE;
        if (state.is(Blocks.SUNFLOWER)) return FarmingCrop.SUNFLOWER;
        return FarmingCrop.NONE;
    }

    private static int scoreToolForType(LegacyMacroType type, ItemStack stack, String metadata, String itemId) {
        int score = 0;
        boolean hoe = stack.is(h -> h.is(ItemTags.HOES));
        boolean axe = stack.is(h -> h.is(ItemTags.AXES));
        boolean shears = stack.getItem() instanceof ShearsItem;

        if (hoe) score += 20;
        if (axe) score += 12;
        if (shears) score += 8;

        int tier = extractItemTier(itemId);
        score += Math.min(20, tier * 2);

        if (!itemId.isEmpty()) {
            switch (type) {
                case S_COCOA_BEANS, S_COCOA_BEANS_TRAPDOORS, S_COCOA_BEANS_LEFT_RIGHT -> {
                    if (itemId.contains("COCO_CHOPPER")) score += 180;
                    else if (itemId.contains("CHOPPER")) score += 120;
                }
                case S_PUMPKIN_MELON, S_PUMPKIN_MELON_MELONGKINGDE, S_PUMPKIN_MELON_DEFAULT_PLOT -> {
                    if (itemId.contains("MELON_DICER") || itemId.contains("PUMPKIN_DICER")) score += 180;
                    else if (itemId.contains("_DICER")) score += 120;
                }
                case S_MUSHROOM, S_MUSHROOM_ROTATE, S_MUSHROOM_SDS -> {
                    if (itemId.contains("FUNGI_CUTTER")) score += 180;
                    if (itemId.contains("DAEDALUS_AXE")) score += 170;
                }
                case S_SUGAR_CANE -> {
                    if (itemId.contains("HOE_CANE")) score += 180;
                }
                case S_CACTUS, S_CACTUS_SUNTZU -> {
                    if (itemId.contains("CACTUS_KNIFE")) score += 180;
                }
                default -> {
                    if (itemId.contains("HOE_WHEAT")
                            || itemId.contains("HOE_CARROT")
                            || itemId.contains("HOE_POTATO")
                            || itemId.contains("HOE_WARTS")) {
                        score += 170;
                    } else if (itemId.contains("HOE_")) {
                        score += 110;
                    } else if (itemId.contains("_DICER") || itemId.contains("CHOPPER")) {
                        score += 90;
                    }
                }
            }
        }

        if (metadata.contains("vacuum")
                || metadata.contains("repellent")
                || metadata.contains("sprayonator")
                || metadata.contains("cookie")
                || metadata.contains("god pot")) {
            score -= 90;
        }
        return score;
    }

    private static String extractSkyblockItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag root = customData.copyTag();
                if (root != null) {
                    CompoundTag extra = root.getCompoundOrEmpty("ExtraAttributes");
                    String id = extra.getStringOr("id", "").trim();
                    if (!id.isEmpty()) {
                        return id.toUpperCase(Locale.ROOT);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        String componentsDump = String.valueOf(stack.getComponents()).toUpperCase(Locale.ROOT);
        Matcher matcher = SKYBLOCK_ID_PATTERN.matcher(componentsDump);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static int extractItemTier(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }
        Matcher matcher = SKYBLOCK_TIER_PATTERN.matcher(itemId);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
