package com.jelly.farmhelper.fabric.ui.framework;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * MC 26.3 / SDL mouse buttons are 1-indexed (LEFT=1). Older GLFW code used 0.
 */
public final class UiInput {
    private UiInput() {
    }

    public static boolean isPrimaryClick(MouseButtonEvent click) {
        return click != null && click.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }
}
