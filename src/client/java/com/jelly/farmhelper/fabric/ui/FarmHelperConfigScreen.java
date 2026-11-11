package com.jelly.farmhelper.fabric.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Legacy entrypoint kept for compatibility with older call sites.
 * It delegates directly to the modern configuration screen.
 */
public class FarmHelperConfigScreen extends Screen {
    private final Screen parent;
    private boolean delegated;

    public FarmHelperConfigScreen(Screen parent) {
        super(Component.literal("FarmHelper Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        delegateToModern();
    }

    @Override
    public void tick() {
        if (!delegated) {
            delegateToModern();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractBackground(context, mouseX, mouseY, delta);
    }

    private void delegateToModern() {
        if (delegated || minecraft == null) {
            return;
        }
        delegated = true;
        minecraft.gui.setScreen(new FarmHelperModernConfigScreen(parent));
    }
}
