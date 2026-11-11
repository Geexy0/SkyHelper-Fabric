package com.jelly.farmhelper.fabric.ui.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * On/off pill switch used for boolean settings.
 */
public class UiToggle extends UiComponent {
    private final Component label;
    private final BooleanGetter getter;
    private final BooleanSetter setter;

    private boolean hovered;

    public interface BooleanGetter {
        boolean get();
    }

    public interface BooleanSetter {
        void set(boolean value);
    }

    public UiToggle(Component label, BooleanGetter getter, BooleanSetter setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.height = 24;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, UiTheme theme) {
        if (!visible) return;
        hovered = isInside(mouseX, mouseY);
        boolean value = getter.get();

        int switchWidth = 40;
        int switchHeight = 18;
        int switchX = x + width - switchWidth;
        int switchY = y + (height - switchHeight) / 2;

        int trackColor = value ? theme.accent : theme.controlBackground;
        int thumbColor = 0xFFFFFFFF;

        // Label
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.font != null) {
            context.text(client.font, label, x, y + (height - 8) / 2, theme.textPrimary, true);
        }

        // Track
        UiDraw.drawRoundedPanel(
                context,
                switchX,
                switchY,
                switchX + switchWidth,
                switchY + switchHeight,
                switchHeight / 2,
                hovered ? theme.borderColor : 0x66000000,
                trackColor
        );

        // Thumb
        int thumbSize = switchHeight - 4;
        int thumbX = value ? switchX + switchWidth - thumbSize - 2 : switchX + 2;
        int thumbY = switchY + 2;
        UiDraw.fillRoundedRect(context, thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, thumbSize / 2, thumbColor);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean dblClick) {
        if (!enabled || !visible || !UiInput.isPrimaryClick(click)) {
            return false;
        }
        if (isInside(click.x(), click.y())) {
            setter.set(!getter.get());
            return true;
        }
        return false;
    }
}
