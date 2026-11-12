package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingBiteSignalsTest {
    @Test
    void biteHintsRequireDoubleBangOrPhrase() {
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("!!!")));
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("§c!!!!")));
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("§aSomething bites!")));
        assertFalse(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("!")));
        assertFalse(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("You caught a fish")));
    }

    @Test
    void soundAndParticleBitesDisabled() {
        assertFalse(FishingBiteSignals.isBiteSoundHint("minecraft:entity.fishing_bobber.splash"));
        assertFalse(FishingBiteSignals.isBiteParticleHint("splash"));
    }
}
