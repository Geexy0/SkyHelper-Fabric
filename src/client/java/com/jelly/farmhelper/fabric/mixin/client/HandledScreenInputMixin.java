package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.InventoryInputEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenInputMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), require = 0)
    private void farmhelper$onInventoryKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input == null ? -1 : input.input();
        char typed = (keyCode >= 32 && keyCode <= 126) ? (char) keyCode : '\0';
        FarmHelperFabric.getEventBus().post(new InventoryInputEvent(keyCode, typed));
    }
}
