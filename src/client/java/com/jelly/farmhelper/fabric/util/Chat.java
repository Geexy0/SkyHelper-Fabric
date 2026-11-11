package com.jelly.farmhelper.fabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Chat {
    private static final String PREFIX = "[FarmHelper] ";

    private Chat() {
    }

    public static void info(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(PREFIX + message));
        }
    }
}
