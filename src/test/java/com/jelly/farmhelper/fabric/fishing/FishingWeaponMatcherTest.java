package com.jelly.farmhelper.fabric.fishing;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingWeaponMatcherTest {
    @Test
    void classifiesKnownWeapons() {
        assertEquals(FishingWeaponMatcher.WeaponClass.HYPERION, FishingWeaponMatcher.classify("Hyperion", ""));
        assertEquals(FishingWeaponMatcher.WeaponClass.FIRE_VEIL, FishingWeaponMatcher.classify("Fire Veil Wand", ""));
        assertEquals(FishingWeaponMatcher.WeaponClass.FROZEN_SCYTHE, FishingWeaponMatcher.classify("Frozen Scythe", ""));
        assertEquals(FishingWeaponMatcher.WeaponClass.SWORD, FishingWeaponMatcher.classify("Aspect of the Dragons", ""));
        assertEquals(FishingWeaponMatcher.WeaponClass.ROD, FishingWeaponMatcher.classify("Rod of the Sea", ""));
    }

    @Test
    void pickBestPrefersHyperionOverSword() {
        List<String> hotbar = List.of(
                "Fishing Rod",
                "Aspect of the End",
                "Hyperion",
                "Dirt"
        );
        assertEquals(2, FishingWeaponMatcher.pickBestHotbarIndex(hotbar, "hyperion,fire veil,sword"));
    }

    @Test
    void pickBestRespectsPreferenceOrder() {
        List<String> hotbar = List.of("Hyperion", "Fire Veil Wand", "Iron Sword");
        assertEquals(1, FishingWeaponMatcher.pickBestHotbarIndex(hotbar, "fire veil,hyperion,sword"));
    }

    @Test
    void usesRightClickForAbilityWeapons() {
        assertTrue(FishingWeaponMatcher.usesRightClick(FishingWeaponMatcher.WeaponClass.HYPERION));
        assertTrue(FishingWeaponMatcher.usesRightClick(FishingWeaponMatcher.WeaponClass.FIRE_VEIL));
    }
}
