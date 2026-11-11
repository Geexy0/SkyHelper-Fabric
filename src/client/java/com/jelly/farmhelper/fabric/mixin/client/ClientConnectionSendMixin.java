package com.jelly.farmhelper.fabric.mixin.client;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.event.SendPacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ClientConnectionSendMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), require = 0)
    private void farmhelper$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet != null) {
            FarmHelperFabric.getEventBus().post(new SendPacketEvent(packet));
        }
    }
}
