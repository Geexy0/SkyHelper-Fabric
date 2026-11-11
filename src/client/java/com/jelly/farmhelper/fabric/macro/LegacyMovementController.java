package com.jelly.farmhelper.fabric.macro;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import net.minecraft.client.Minecraft;

interface LegacyMovementController {
    void updateState(
            Minecraft client,
            FarmHelperConfig config,
            int forwardTicks,
            int sideTicks,
            boolean holdAttack
    );

    void invokeState(
            Minecraft client,
            FarmHelperConfig config,
            int forwardTicks,
            int sideTicks,
            boolean holdAttack
    );
}
