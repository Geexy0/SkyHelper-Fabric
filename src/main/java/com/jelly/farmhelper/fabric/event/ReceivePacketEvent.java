package com.jelly.farmhelper.fabric.event;

import net.minecraft.network.protocol.Packet;

public final class ReceivePacketEvent {
    public final Packet<?> packet;

    public ReceivePacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
