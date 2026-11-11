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
            return;
        }

        if (type == LegacyMacroType.S_SUGAR_CANE) {
            config.macroPattern = MacroPattern.S_SHAPE;
            config.customPitch = true;
            config.customPitchLevel = 0f;
            if (!config.customYaw) {
                config.customYaw = true;
                config.customYawLevel = 0f;
            }
            return;
        }

        if (type == LegacyMacroType.S_COCOA_BEANS_LEFT_RIGHT
                || type == LegacyMacroType.S_COCOA_BEANS
                || type == LegacyMacroType.S_COCOA_BEANS_TRAPDOORS) {
            config.macroPattern = MacroPattern.S_SHAPE;
            config.customPitch = true;
            config.customPitchLevel = -70f;
            if (!config.customYaw) {
                config.customYaw = true;
                config.customYawLevel = type == LegacyMacroType.S_COCOA_BEANS_LEFT_RIGHT ? 90f : -90f;
            }
            return;
        }

        if (type == LegacyMacroType.S_MUSHROOM
                || type == LegacyMacroType.S_MUSHROOM_ROTATE
                || type == LegacyMacroType.S_MUSHROOM_SDS) {
            config.macroPattern = MacroPattern.S_SHAPE;
            config.customPitch = true;
            // 45° mushroom looks slightly down; rotate variant keeps a mild pitch.
            config.customPitchLevel = type == LegacyMacroType.S_MUSHROOM ? -45f : -30f;
            if (!config.customYaw) {
                config.customYawLevel = -45f;
            }
        }
    }
}
