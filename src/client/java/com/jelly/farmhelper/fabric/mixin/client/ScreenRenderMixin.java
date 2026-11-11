package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.DrawScreenAfterEvent;
import com.jelly.farmhelper.fabric.ui.AutoReconnectOverlayRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void farmhelper$afterRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AbstractContainerScreen<?>) {
            return;
        }
        if (screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen) {
            AutoReconnectOverlayRenderer.render(context, screen.width);
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        FarmHelperFabric.getEventBus().post(new DrawScreenAfterEvent(screen.getClass().getName(), title));
    }
}
