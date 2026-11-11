package com.jelly.farmhelper.fabric.ui.framework;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Simple pill‑shaped button with hover/press states.
 * Drawn manually using rectangles and text; does not use vanilla ButtonWidget.
 */
public class UiButton extends UiComponent {
    private final Supplier<Component> labelSupplier;
    private final Runnable onClick;

    private boolean hovered;
    private boolean pressed;

    public UiButton(Component label, Runnable onClick) {
        this(() -> label, onClick);
    }

    public UiButton(Supplier<Component> labelSupplier, Runnable onClick) {
        this.labelSupplier = labelSupplier;
        this.onClick = onClick;
        this.height = 24;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, UiTheme theme) {
        if (!visible) return;
        hovered = isInside(mouseX, mouseY);
        int bg = hovered ? theme.controlBackgroundHover : theme.controlBackground;
        int border = hovered ? theme.accent : theme.borderColor;

        UiDraw.drawRoundedPanel(context, x, y, x + width, y + height, theme.cornerRadiusSmall, border, bg);

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.font != null) {
            Component label = labelSupplier == null ? Component.empty() : labelSupplier.get();
            String labelText = client.font.plainSubstrByWidth(label.getString(), Math.max(12, width - 10));
            int tw = client.font.width(labelText);
            int tx = x + (width - tw) / 2;
            int ty = y + (height - 8) / 2;
            int color = enabled ? theme.textPrimary : theme.textSecondary;
            context.text(client.font, labelText, tx, ty, color, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean dblClick) {
        if (!enabled || !visible || !UiInput.isPrimaryClick(click)) {
            return false;
        }
        if (isInside(click.x(), click.y())) {
            pressed = true;
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (!enabled || !visible || !UiInput.isPrimaryClick(click)) {
            return false;
        }
        boolean wasPressed = pressed;
        pressed = false;
        return wasPressed && isInside(click.x(), click.y());
    }
}
