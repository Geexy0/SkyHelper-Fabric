package com.jelly.farmhelper.fabric.macro;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroPresetDefaultsTest {
    @Test
    void wheatLocksCamera() {
        FarmHelperConfig config = new FarmHelperConfig();
        config.useLegacyProfileDefaults = false;
        MacroPresetDefaults.applyOnSelect(config, LegacyMacroType.S_WHEAT);
        assertTrue(config.customYaw);
        assertEquals(-90f, config.customYawLevel, 0.01f);
        assertEquals(0f, config.customPitchLevel, 0.01f);
    }

    @Test
    void sugarCaneAndCocoaAndMushroomDefaults() {
        FarmHelperConfig cane = new FarmHelperConfig();
        cane.useLegacyProfileDefaults = false;
        MacroPresetDefaults.applyOnSelect(cane, LegacyMacroType.S_SUGAR_CANE);
        assertEquals(0f, cane.customPitchLevel, 0.01f);

        FarmHelperConfig cocoa = new FarmHelperConfig();
        cocoa.useLegacyProfileDefaults = false;
        MacroPresetDefaults.applyOnSelect(cocoa, LegacyMacroType.S_COCOA_BEANS_LEFT_RIGHT);
        assertEquals(-70f, cocoa.customPitchLevel, 0.01f);
        assertEquals(90f, cocoa.customYawLevel, 0.01f);

        FarmHelperConfig mush = new FarmHelperConfig();
        mush.useLegacyProfileDefaults = false;
        MacroPresetDefaults.applyOnSelect(mush, LegacyMacroType.S_MUSHROOM);
        assertEquals(-45f, mush.customPitchLevel, 0.01f);
    }
}
