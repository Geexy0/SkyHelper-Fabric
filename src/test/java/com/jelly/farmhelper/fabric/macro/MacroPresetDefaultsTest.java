package com.jelly.farmhelper.fabric.macro;

import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroPresetDefaultsTest {
    @Test
    void wheatLocksCameraToNegNinetyAndZero() {
        FarmHelperConfig config = new FarmHelperConfig();
        config.useLegacyProfileDefaults = false;
        MacroPresetDefaults.applyOnSelect(config, LegacyMacroType.S_WHEAT);

        assertEquals(MacroPattern.S_SHAPE, config.macroPattern);
        assertTrue(config.customYaw);
        assertEquals(-90f, config.customYawLevel, 0.001f);
        assertTrue(config.customPitch);
        assertEquals(0f, config.customPitchLevel, 0.001f);
    }

    @Test
    void potatoAndCarrotMatchWheatCameraDefaults() {
        for (LegacyMacroType type : new LegacyMacroType[]{LegacyMacroType.S_POTATO, LegacyMacroType.S_CARROT}) {
            FarmHelperConfig config = new FarmHelperConfig();
            MacroPresetDefaults.applyOnSelect(config, type);
            assertTrue(config.customYaw, type.name());
            assertEquals(-90f, config.customYawLevel, 0.001f, type.name());
            assertTrue(config.customPitch, type.name());
            assertEquals(0f, config.customPitchLevel, 0.001f, type.name());
        }
    }

    @Test
    void netherWartSetsPitchBandWithoutForcingYaw() {
        FarmHelperConfig config = new FarmHelperConfig();
        config.customYaw = false;
        MacroPresetDefaults.applyOnSelect(config, LegacyMacroType.S_NETHER_WART);

        assertEquals(MacroPattern.S_SHAPE, config.macroPattern);
        assertTrue(config.customPitch);
        assertEquals(3.0f, config.customPitchLevel, 0.001f);
        assertFalse(config.customYaw);
    }

    @Test
    void nullInputsAreNoOps() {
        FarmHelperConfig config = new FarmHelperConfig();
        config.macroType = LegacyMacroType.S_V_NORMAL_TYPE;
        MacroPresetDefaults.applyOnSelect(null, LegacyMacroType.S_WHEAT);
        MacroPresetDefaults.applyOnSelect(config, null);
        assertEquals(LegacyMacroType.S_V_NORMAL_TYPE, config.macroType);
    }
}
