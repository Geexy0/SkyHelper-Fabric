package com.jelly.farmhelper.fabric.ui.framework;

import com.jelly.farmhelper.fabric.util.KeybindSanitizer;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * MC 26.3 / SDL mouse buttons are 1-indexed (LEFT=1). Older GLFW code used 0.
 */
public final class UiInput {
    private UiInput() {
    }

    public static boolean isPrimaryClick(MouseButtonEvent click) {
        return click != null && isPrimaryClickButton(click.button());
    }

    public static boolean isPrimaryClickButton(int button) {
        return KeybindSanitizer.isPrimaryClickButton(button);
    }
}
