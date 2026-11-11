package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.state.FreelookController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityFreelookMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void farmhelper$redirectLocalPlayerLook(double yaw, double pitch, CallbackInfo ci) {
        FreelookController controller = FreelookController.getInstance();
        if (!controller.isActive()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || (Object) this != client.player) {
            return;
        }
        controller.onMouseLook(yaw, pitch);
        ci.cancel();
    }
}
