package com.jelly.farmhelper.fabric.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindSanitizerTest {
    @Test
    void sanitizeKeepsValidSdlScancodes() {
        assertEquals(53, KeybindSanitizer.sanitizeKeyCode(53, 9));
        assertEquals(0, KeybindSanitizer.sanitizeKeyCode(0, 9));
        assertEquals(511, KeybindSanitizer.sanitizeKeyCode(511, 9));
    }

    @Test
    void sanitizeFallsBackForOutOfRange() {
        assertEquals(9, KeybindSanitizer.sanitizeKeyCode(-1, 9));
        assertEquals(9, KeybindSanitizer.sanitizeKeyCode(512, 9));
        assertEquals(9, KeybindSanitizer.sanitizeKeyCode(708, 9));
    }

    @Test
    void corruptBindingDetectsNumericGlfwLeftovers() {
        assertTrue(KeybindSanitizer.isCorruptKeyboardBinding(296, "key.keyboard.296", true));
        assertTrue(KeybindSanitizer.isCorruptKeyboardBinding(700, "key.keyboard.f7", true));
        assertFalse(KeybindSanitizer.isCorruptKeyboardBinding(64, "key.keyboard.f7", true));
        assertFalse(KeybindSanitizer.isCorruptKeyboardBinding(999, "key.mouse.left", false));
    }

    @Test
    void primaryClickUsesSdlLeftButton() {
        assertTrue(KeybindSanitizer.isPrimaryClickButton(1));
        assertFalse(KeybindSanitizer.isPrimaryClickButton(0));
        assertFalse(KeybindSanitizer.isPrimaryClickButton(2));
    }
}
