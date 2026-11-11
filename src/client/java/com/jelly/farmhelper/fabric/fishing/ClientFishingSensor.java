package com.jelly.farmhelper.fabric.fishing;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.FeatureRuntimeState;
import com.jelly.farmhelper.fabric.util.RenderUtils;
import java.lang.reflect.Field;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side bobber / sea-creature / golden-fish sensors for fishing runtime state.
 * Bite detection uses multiple signals because Hypixel often never sets vanilla DATA_BITING.
 */
public final class ClientFishingSensor {
    private static EntityDataAccessor<Boolean> bitingAccessor;
    private static boolean bitingAccessorResolved;
    private static Field hudTitleField;
    private static Field hudSubtitleField;
    private static Field hudOverlayField;
    private static boolean hudFieldsResolved;

    private static int lastHookId = -1;
    private static double lastHookY = Double.NaN;
    private static double lastHookVelY = Double.NaN;
    private static long biteLatchUntilTick = -1L;

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

        FishingHook ownHook = player.fishing;
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
                if (ownHook == null && hook.getPlayerOwner() == player) {
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

        long nowTick = client.level.getGameTime();
        if (ownHook != null) {
            runtime.fishingBobberPresent = true;
            boolean biting = isBiting(ownHook)
                    || isMotionBite(ownHook, nowTick)
                    || titleSuggestsBite(client);
            if (biting) {
                biteLatchUntilTick = nowTick + 12L;
            }
            runtime.fishingBiteReady = biting || nowTick <= biteLatchUntilTick;
        } else {
            lastHookId = -1;
            lastHookY = Double.NaN;
            lastHookVelY = Double.NaN;
            biteLatchUntilTick = -1L;
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

    private static boolean isMotionBite(FishingHook hook, long nowTick) {
        int id = hook.getId();
        double y = hook.getY();
        double velY = hook.getDeltaMovement().y;
        boolean bite = false;
        if (id == lastHookId) {
            bite = FishingBiteSignals.isMotionBiteSignal(lastHookY, y, lastHookVelY, velY);
        }
        lastHookId = id;
        lastHookY = y;
        lastHookVelY = velY;
        return bite;
    }

    private static boolean titleSuggestsBite(Minecraft client) {
        resolveHudFields();
        Hud hud = client.gui == null ? null : client.gui.hud;
        if (hud == null) {
            return false;
        }
        return textIsBiteHint(readHudComponent(hud, hudTitleField))
                || textIsBiteHint(readHudComponent(hud, hudSubtitleField))
                || textIsBiteHint(readHudComponent(hud, hudOverlayField));
    }

    private static boolean textIsBiteHint(String raw) {
        return FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize(raw));
    }

    private static String readHudComponent(Hud hud, Field field) {
        if (hud == null || field == null) {
            return "";
        }
        try {
            Object value = field.get(hud);
            if (value instanceof Component component) {
                return component.getString();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static void resolveHudFields() {
        if (hudFieldsResolved) {
            return;
        }
        hudFieldsResolved = true;
        try {
            hudTitleField = Hud.class.getDeclaredField("title");
            hudTitleField.setAccessible(true);
            hudSubtitleField = Hud.class.getDeclaredField("subtitle");
            hudSubtitleField.setAccessible(true);
            hudOverlayField = Hud.class.getDeclaredField("overlayMessageString");
            hudOverlayField.setAccessible(true);
        } catch (Throwable ignored) {
            hudTitleField = null;
            hudSubtitleField = null;
            hudOverlayField = null;
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
                if (Boolean.TRUE.equals(value)) {
                    return true;
                }
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
