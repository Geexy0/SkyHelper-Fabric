package com.jelly.farmhelper.fabric.ui.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Base Screen for all custom FarmHelper UI built on the new framework.
 * It owns a root UiComponent tree, a shared theme, and dispatches input.
 */
public abstract class FHScreen extends Screen {
    protected final Screen parent;
    protected final UiTheme theme = UiTheme.DEFAULT_DARK;
    protected UiComponent rootComponent;

    protected FHScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildUi();
    }

    /** Called from init() and when the screen is resized to (re)construct layout. */
    protected abstract void buildUi();

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Blur / darken background
        extractBackground(context, mouseX, mouseY, delta);
        if (rootComponent != null) {
            rootComponent.tick(this);
            rootComponent.render(context, mouseX, mouseY, delta, theme);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean dblClick) {
        if (rootComponent != null && rootComponent.mouseClicked(click, dblClick)) {
            return true;
        }
        return super.mouseClicked(click, dblClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (rootComponent != null && rootComponent.mouseReleased(click)) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (rootComponent != null && rootComponent.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (rootComponent != null && rootComponent.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input == null ? -1 : input.key();
        int scanCode = input == null ? 0 : input.keycode();
        int modifiers = input == null ? 0 : input.modifiers();
        if (rootComponent != null && rootComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (input != null && input.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = input == null ? '\0' : (char) input.codepoint();
        int modifiers = 0;
        if (rootComponent != null && rootComponent.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        if (minecraft == null) {
            super.onClose();
            return;
        }
        minecraft.gui.setScreen(parent);
    }

    protected Minecraft mc() {
        return Minecraft.getInstance();
    }
}
