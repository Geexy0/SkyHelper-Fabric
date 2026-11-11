package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.state.FreelookController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseFreelookMixin {
    @Redirect(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;turn(DD)V"
            ),
            require = 0
    )
    private void farmhelper$redirectMouseLookToFreelook(Entity entity, double deltaX, double deltaY) {
        FreelookController controller = FreelookController.getInstance();
        Minecraft client = Minecraft.getInstance();
        if (client != null && controller.isActive() && client.player != null && entity == client.player) {
            controller.onMouseLook(deltaX, deltaY);
            return;
        }
        entity.turn(deltaX, deltaY);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmhelper$consumeScrollForFreelook(long window, double horizontal, double vertical, CallbackInfo ci) {
        FreelookController controller = FreelookController.getInstance();
        Minecraft client = Minecraft.getInstance();
        if (client == null || !controller.isActive()) {
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }
        controller.adjustDistance(vertical);
        ci.cancel();
    }
}
