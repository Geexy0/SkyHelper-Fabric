package com.jelly.farmhelper.fabric.util;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class AudioManager {
    private static final AudioManager INSTANCE = new AudioManager();

    private boolean alertActive;
    private long lastPingTick = -20L;
    private boolean playPlaceSoundNext = true;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    public void playFailsafeAlert() {
        alertActive = true;
        lastPingTick = -20L;
        playPlaceSoundNext = true;
    }

    public void stop() {
        alertActive = false;
    }

    public boolean isAlertActive() {
        return alertActive;
    }

    public void tick(long tickCount) {
        if (!alertActive) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.enableFailsafeSound || !config.enableFailsafeAnvilAlert) {
            return;
        }
        int intervalTicks = Math.max(1, config.failsafeAnvilAlertIntervalTicks);
        if (tickCount - lastPingTick < intervalTicks) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getSoundManager() != null) {
            float volume = Mth.clamp(config.failsafeAnvilAlertVolume, 0.1f, 2.0f);
            if (playPlaceSoundNext) {
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_PLACE, volume));
            } else {
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, volume));
            }
            playPlaceSoundNext = !playPlaceSoundNext;
        }
        lastPingTick = tickCount;
    }
}
