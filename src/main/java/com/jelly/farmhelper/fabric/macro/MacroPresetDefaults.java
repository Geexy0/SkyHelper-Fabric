package com.jelly.farmhelper.fabric.macro;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;

/**
 * Applies type-specific camera / pattern defaults when a macro preset is selected.
 */
public final class MacroPresetDefaults {
    private MacroPresetDefaults() {
    }

    public static void applyOnSelect(FarmHelperConfig config, LegacyMacroType type) {
        if (config == null || type == null) {
            return;
        }
        if (config.useLegacyProfileDefaults) {
            MacroStrategyProfile profile = LegacyMacroProfiles.forType(type);
            config.macroPattern = profile.pattern();
            config.forwardTicksBeforeTurn = profile.defaultForwardTicks();
            config.sideStepTicks = profile.defaultSideStepTicks();
        } else {
            config.macroPattern = type.defaultPattern();
        }

        if (type.locksCameraLikeWheat()) {
            config.customYaw = true;
            config.customYawLevel = -90f;
            config.customPitch = true;
            config.customPitchLevel = 0f;
            return;
        }

        if (type == LegacyMacroType.S_NETHER_WART) {
            config.macroPattern = MacroPattern.S_SHAPE;
            config.customPitch = true;
            config.customPitchLevel = 3.0f;
            // Leave yaw unlocked so the lane can keep the current cardinal after warp.
            if (!config.customYaw) {
                config.customYawLevel = 0f;
            }
        }
    }
}
