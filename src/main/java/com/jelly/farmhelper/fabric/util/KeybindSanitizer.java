package com.jelly.farmhelper.fabric.util;

import java.util.regex.Pattern;

/**
 * SDL scancode sanitizers shared by config load and client keybind setup.
 */
public final class KeybindSanitizer {
    public static final int SDL_NUM_SCANCODES = 512;
    /** MC 26.3 / SDL left mouse button (legacy GLFW used 0). */
    public static final int PRIMARY_MOUSE_BUTTON = 1;
    private static final Pattern NUMERIC_KEYBOARD_NAME = Pattern.compile("^key\\.keyboard\\.-?\\d+$");

    private KeybindSanitizer() {
    }

    public static int sanitizeKeyCode(int stored, int fallback) {
        if (stored < 0 || stored >= SDL_NUM_SCANCODES) {
            return fallback;
        }
        return stored;
    }

    public static boolean isPrimaryClickButton(int button) {
        return button == PRIMARY_MOUSE_BUTTON;
    }

    /**
     * Detects corrupt keyboard bindings from pre-26.3 GLFW options (OOB values or numeric names).
     */
    public static boolean isCorruptKeyboardBinding(Integer keyValue, String keyName, boolean isKeyboardType) {
        if (!isKeyboardType || keyValue == null) {
            return false;
        }
        if (keyValue < 0 || keyValue >= SDL_NUM_SCANCODES) {
            return true;
        }
        return keyName != null && NUMERIC_KEYBOARD_NAME.matcher(keyName).matches();
    }
}
