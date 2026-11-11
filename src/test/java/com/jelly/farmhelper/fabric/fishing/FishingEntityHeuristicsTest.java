package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingEntityHeuristicsTest {
    @Test
    void detectsSeaCreatures() {
        assertTrue(FishingEntityHeuristics.isSeaCreature("§cLord Jawbus"));
        assertTrue(FishingEntityHeuristics.isSeaCreature("Water Hydra"));
        assertTrue(FishingEntityHeuristics.isSeaCreature("Nurse Shark"));
        assertFalse(FishingEntityHeuristics.isSeaCreature("Armor Stand"));
        assertFalse(FishingEntityHeuristics.isSeaCreature("Player"));
        assertFalse(FishingEntityHeuristics.isSeaCreature("Cow"));
    }

    @Test
    void detectsGoldenFish() {
        assertTrue(FishingEntityHeuristics.isGoldenFish("Golden Fish"));
        assertTrue(FishingEntityHeuristics.isGoldenFish("§6Golden Fish"));
        assertFalse(FishingEntityHeuristics.isGoldenFish("Fish"));
    }

    @Test
    void trophyExtrasFromCsv() {
        assertTrue(FishingEntityHeuristics.isTrophyLabel("Golden Fish", ""));
        assertTrue(FishingEntityHeuristics.isTrophyLabel("Sulphur Skitter", "skitter,blobfish"));
        assertFalse(FishingEntityHeuristics.isTrophyLabel("Cow", "skitter"));
    }
}
