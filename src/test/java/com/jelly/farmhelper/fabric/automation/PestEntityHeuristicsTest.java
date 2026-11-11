package com.jelly.farmhelper.fabric.automation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PestEntityHeuristicsTest {
    private final PestEntityHeuristics heuristics = new PestEntityHeuristics();

    @Test
    void ambientBatAtSpawnIsNotEspPest() {
        assertFalse(heuristics.isEspPestSignals("bat", "entity.minecraft.bat", ""));
        assertFalse(heuristics.isEspPestSignals("Bat", "entity.minecraft.bat", ""));
        assertFalse(heuristics.isEspPestSignals("§7Bat", "entity.minecraft.bat", ""));
        assertFalse(heuristics.isEspPestSignals("Pest", "entity.minecraft.bat", ""));
        assertFalse(heuristics.isEspPestSignals("pest 1", "entity.minecraft.bat", ""));
        // Accidental texture-hash substring in NBT must not promote hub bats.
        assertFalse(heuristics.isEspPestSignals(
                "Bat",
                "entity.minecraft.bat",
                "random nbt 70a1e836bf1968b2 noise"
        ));
        assertFalse(heuristics.isLikelyPestSignals("Bat", "entity.minecraft.bat", "", "", true, true));
    }

    @Test
    void flyingAttributeSubstringDoesNotMarkBat() {
        // Short token "fly" must not match inside "flying".
        assertFalse(heuristics.isEspPestSignals(
                "Bat",
                "entity.minecraft.bat",
                "generic.flying_speed attribute flying"
        ));
        assertFalse(heuristics.isLikelyPestSignals(
                "Bat",
                "entity.minecraft.bat",
                "",
                "generic.flying_speed",
                true,
                false
        ));
    }

    @Test
    void armorStandHoldingComparatorIsRejected() {
        assertFalse(heuristics.isEspPestSignals(
                "Armor Stand",
                "entity.minecraft.armor_stand",
                "redstone comparator minecraft:comparator"
        ));
    }

    @Test
    void namedBeetleOnBatTypeIsEspPest() {
        assertTrue(heuristics.isEspPestSignals("Beetle", "entity.minecraft.bat", ""));
        assertTrue(heuristics.isEspPestSignals("§aBeetle", "entity.minecraft.bat", ""));
        assertTrue(heuristics.isLikelyPestSignals("Beetle", "entity.minecraft.bat", "", "", true, false));
    }

    @Test
    void textureSignatureConfirmsEspPest() {
        assertTrue(heuristics.isEspPestSignals(
                "Armor Stand",
                "entity.minecraft.armor_stand",
                "textures/70a1e836bf1968b2/skin"
        ));
    }

    @Test
    void itemFrameAndBlockDisplayHardRejected() {
        assertFalse(heuristics.isEspPestSignals("pest beetle", "entity.minecraft.item_frame", ""));
        assertFalse(heuristics.isEspPestSignals("beetle", "entity.minecraft.block_display", "comparator"));
    }
}
