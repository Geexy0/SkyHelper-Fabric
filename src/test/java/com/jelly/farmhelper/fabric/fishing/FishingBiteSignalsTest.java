package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingBiteSignalsTest {
    @Test
    void biteHintsRequireDoubleBangOrPhrase() {
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("!!!")));
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("§c!!!!")));
        assertFalse(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("!")));
    }

    @Test
    void paulAddonsSkyblockBiteParticles() {
        assertTrue(FishingBiteSignals.isSkyblockBiteParticle("minecraft:flame", 6, 0.2f));
        assertTrue(FishingBiteSignals.isSkyblockBiteParticle("minecraft:fishing", 6, 0.2f));
        assertTrue(FishingBiteSignals.isSkyblockBiteParticle("water_wake", 6, 0.2f));
        assertFalse(FishingBiteSignals.isSkyblockBiteParticle("minecraft:flame", 5, 0.2f));
        assertFalse(FishingBiteSignals.isSkyblockBiteParticle("minecraft:flame", 6, 0.5f));
        assertFalse(FishingBiteSignals.isSkyblockBiteParticle("minecraft:splash", 6, 0.2f));
    }

    @Test
    void nearBobberMatchesPaulAddonsDistance() {
        assertTrue(FishingBiteSignals.isNearBobberXZ(10.05, 20.0, 10.0, 64.0, 20.0));
        assertFalse(FishingBiteSignals.isNearBobberXZ(10.5, 20.0, 10.0, 64.0, 20.0));
    }
}
