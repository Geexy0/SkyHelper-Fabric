package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.state.FreelookController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraFreelookMixin {
    // MC 26.3 replaced Camera.setup with alignWithEntity.
    // Ordinal 0 = minecart lerp path, 1 = normal entity view path.
    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 0
            )
    )
    private void farmhelper$applyFreelookRotationMinecart(Args args) {
        farmhelper$applyFreelookRotation(args);
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 1
            )
    )
    private void farmhelper$applyFreelookRotationNormal(Args args) {
        farmhelper$applyFreelookRotation(args);
    }

    private static void farmhelper$applyFreelookRotation(Args args) {
        FreelookController controller = FreelookController.getInstance();
        if (!controller.isActive()) {
            return;
        }
        args.set(0, controller.getCameraYaw());
        args.set(1, controller.getCameraPitch());
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private float farmhelper$useFreelookDistance(float desiredDistance) {
        FreelookController controller = FreelookController.getInstance();
        if (!controller.isActive()) {
            return desiredDistance;
        }
        return controller.getDistance();
    }
}
