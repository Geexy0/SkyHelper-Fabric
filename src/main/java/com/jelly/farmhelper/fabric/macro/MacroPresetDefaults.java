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

        if (type == LegacyMacroType.S_WHEAT) {
            config.customYaw = true;
            config.customYawLevel = -90f;
            config.customPitch = true;
            config.customPitchLevel = 0f;
        }
    }
}
