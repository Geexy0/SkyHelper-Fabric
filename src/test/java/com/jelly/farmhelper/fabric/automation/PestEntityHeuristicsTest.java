package com.jelly.farmhelper.fabric.automation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PestEntityHeuristicsTest {
    private final PestEntityHeuristics heuristics = new PestEntityHeuristics();

    @Test
    void ambientBatAtSpawnIsNotLikelyPest() {
        assertFalse(heuristics.isLikelyPestSignals(
                "bat",
                "entity.minecraft.bat",
                "",
                "",
                false,
                false
        ));
        assertFalse(heuristics.isLikelyPestSignals(
                "Bat",
                "entity.minecraft.bat",
                "",
                "",
                true,
                true
        ));
    }

    @Test
    void armorStandHoldingComparatorIsRejected() {
        assertFalse(heuristics.isLikelyPestSignals(
                "Armor Stand",
                "entity.minecraft.armor_stand",
                "",
                "redstone comparator minecraft:comparator",
                true,
                false
        ));
    }

    @Test
    void namedBeetleOnBatTypeIsLikely() {
        assertTrue(heuristics.isLikelyPestSignals(
                "Beetle",
                "entity.minecraft.bat",
                "",
                "",
                true,
                false
        ));
    }

    @Test
    void textureSignatureConfirmsPest() {
        assertTrue(heuristics.isLikelyPestSignals(
                "Armor Stand",
                "entity.minecraft.armor_stand",
                "",
                "textures/70a1e836bf1968b2/skin",
                false,
                true
        ));
    }

    @Test
    void itemFrameAndBlockDisplayHardRejected() {
        assertFalse(heuristics.isLikelyPestSignals(
                "pest beetle",
                "entity.minecraft.item_frame",
                "",
                "",
                true,
                false
        ));
        assertFalse(heuristics.isLikelyPestSignals(
                "beetle",
                "entity.minecraft.block_display",
                "",
                "comparator",
                true,
                false
        ));
    }
}
