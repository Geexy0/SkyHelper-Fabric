package com.jelly.farmhelper.fabric.fishing;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.FeatureRuntimeState;
import com.jelly.farmhelper.fabric.util.RenderUtils;
import java.lang.reflect.Field;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side bobber / sea-creature / golden-fish sensors for fishing runtime state.
 */
public final class ClientFishingSensor {
    private static EntityDataAccessor<Boolean> bitingAccessor;
    private static boolean bitingAccessorResolved;

    private ClientFishingSensor() {
    }

    public static void update(Minecraft client, FeatureRuntimeState runtime, FarmHelperConfig config) {
        if (runtime == null) {
            return;
        }
        runtime.fishingBobberPresent = false;
        runtime.fishingBiteReady = false;
        runtime.fishingHoldingRod = false;
        runtime.fishingSeaCreatureNearby = false;
        runtime.fishingNearestSeaCreature = "";
        runtime.fishingGoldenFishVisible = false;
        runtime.fishingGoldenFishName = "";
        if (client == null || client.player == null || client.level == null || config == null) {
            return;
        }
        if (!config.enableAutoFishing && !config.fishingTrophyMode && !config.fishingGoldenFishEsp) {
            return;
        }

        LocalPlayer player = client.player;
        ItemStack main = player.getMainHandItem();
        runtime.fishingHoldingRod = main != null && !main.isEmpty() && main.getItem() instanceof FishingRodItem;

        FishingHook ownHook = null;
        Entity nearestCreature = null;
        double nearestCreatureDist = Double.MAX_VALUE;
        Entity golden = null;
        double goldenDist = Double.MAX_VALUE;
        double combatRange = Math.max(4.0, config.fishingSeaCreatureRange);
        double combatRangeSq = combatRange * combatRange;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == null || entity == player || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof FishingHook hook) {
                if (hook.getPlayerOwner() == player) {
                    ownHook = hook;
                }
                continue;
            }
            String name = entity.getName() == null ? "" : entity.getName().getString();
            double distSq = entity.distanceToSqr(player);
            if (config.fishingKillSeaCreatures && distSq <= combatRangeSq
                    && FishingEntityHeuristics.isSeaCreature(name)) {
                if (distSq < nearestCreatureDist) {
                    nearestCreatureDist = distSq;
                    nearestCreature = entity;
                }
            }
            boolean trophyCandidate = FishingEntityHeuristics.isGoldenFish(name)
                    || (config.fishingTrophyMode
                    && FishingEntityHeuristics.isTrophyLabel(name, config.fishingTrophyExtraNames));
            if ((config.fishingTrophyMode || config.fishingGoldenFishEsp) && trophyCandidate && distSq < goldenDist) {
                goldenDist = distSq;
                golden = entity;
            }
        }

        if (ownHook != null) {
            runtime.fishingBobberPresent = true;
            runtime.fishingBiteReady = isBiting(ownHook);
        }

        if (nearestCreature != null) {
            runtime.fishingSeaCreatureNearby = true;
            runtime.fishingNearestSeaCreature = nearestCreature.getName().getString();
        }

        if (golden != null) {
            runtime.fishingGoldenFishVisible = true;
            runtime.fishingGoldenFishName = golden.getName().getString();
            runtime.fishingGoldenFishX = golden.getX();
            runtime.fishingGoldenFishY = golden.getY();
            runtime.fishingGoldenFishZ = golden.getZ();
            if (config.fishingGoldenFishEsp || config.fishingTrophyMode) {
                Vec3 target = new Vec3(golden.getX(), golden.getY() + Math.max(0.3, golden.getBbHeight() * 0.5), golden.getZ());
                String label = FishingEntityHeuristics.isGoldenFish(runtime.fishingGoldenFishName)
                        ? "Golden Fish"
                        : (runtime.fishingGoldenFishName.isBlank() ? "Trophy" : runtime.fishingGoldenFishName);
                RenderUtils.drawTracer(target, 0xFFFFD54A, 6L);
                RenderUtils.drawBox(golden.getBoundingBox().inflate(0.1), 0xAAFFC107, 6L);
                RenderUtils.drawText(target.add(0.0, 0.4, 0.0), label, 0xFFFFF3C0, 6L);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isBiting(FishingHook hook) {
        if (hook == null) {
            return false;
        }
        resolveBitingAccessor();
        if (bitingAccessor != null) {
            try {
                SynchedEntityData data = hook.getEntityData();
                Boolean value = data.get(bitingAccessor);
                return Boolean.TRUE.equals(value);
            } catch (Throwable ignored) {
                // Fall through.
            }
        }
        try {
            Field field = FishingHook.class.getDeclaredField("biting");
            field.setAccessible(true);
            return field.getBoolean(hook);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void resolveBitingAccessor() {
        if (bitingAccessorResolved) {
            return;
        }
        bitingAccessorResolved = true;
        try {
            for (Field field : FishingHook.class.getDeclaredFields()) {
                if (!EntityDataAccessor.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (!(name.contains("biting") || name.contains("bite"))) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof EntityDataAccessor<?> accessor) {
                    bitingAccessor = (EntityDataAccessor<Boolean>) accessor;
                    return;
                }
            }
        } catch (Throwable ignored) {
            bitingAccessor = null;
        }
    }
}
