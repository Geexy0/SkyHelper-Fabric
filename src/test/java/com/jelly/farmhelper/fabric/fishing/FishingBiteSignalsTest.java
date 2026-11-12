package com.jelly.farmhelper.fabric.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingBiteSignalsTest {
    @Test
    void biteHints() {
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("§aSomething bites!")));
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("!!!")));
        assertTrue(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("§c!!!!")));
        assertFalse(FishingBiteSignals.isBiteHint(FishingBiteSignals.normalize("You caught a fish")));
    }

    @Test
    void staffPauseHints() {
        assertTrue(FishingBiteSignals.isStaffOrPauseHint(FishingBiteSignals.normalize("Sending to server mini123")));
        assertTrue(FishingBiteSignals.isStaffOrPauseHint(FishingBiteSignals.normalize("Warping...")));
        assertFalse(FishingBiteSignals.isStaffOrPauseHint(FishingBiteSignals.normalize("Good catch!")));
    }

    @Test
    void biteSoundAndParticleHints() {
        assertTrue(FishingBiteSignals.isBiteSoundHint("minecraft:entity.fishing_bobber.splash"));
        assertTrue(FishingBiteSignals.isBiteSoundHint("minecraft:block.note_block.pling"));
        assertFalse(FishingBiteSignals.isBiteSoundHint("minecraft:entity.cow.ambient"));
        assertTrue(FishingBiteSignals.isBiteParticleHint("fishing"));
        assertTrue(FishingBiteSignals.isBiteParticleHint("splash"));
    }
}