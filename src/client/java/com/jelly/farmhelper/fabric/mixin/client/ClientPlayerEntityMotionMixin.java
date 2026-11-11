package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.MotionUpdateEvent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMotionMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), require = 0)
    private void farmhelper$postMotionPre(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        FarmHelperFabric.getEventBus().post(new MotionUpdateEvent.Pre(
                player.getYRot(),
                player.getXRot(),
                player.getX(),
                player.getY(),
                player.getZ()
        ));
    }

    @Inject(method = "sendPosition", at = @At("TAIL"), require = 0)
    private void farmhelper$postMotionPost(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        FarmHelperFabric.getEventBus().post(new MotionUpdateEvent.Post(
                player.getYRot(),
                player.getXRot(),
                player.getX(),
                player.getY(),
                player.getZ()
        ));
    }
}
