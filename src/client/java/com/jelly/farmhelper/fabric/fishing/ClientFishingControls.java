package com.jelly.farmhelper.fabric.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * Bypass the throttled action queue for urgent rod use (reel on bite).
 */
public final class ClientFishingControls {
    private static volatile boolean reelRequested;

    private ClientFishingControls() {
    }

    public static void requestReel() {
        reelRequested = true;
    }

    public static boolean consumeReelRequest() {
        if (!reelRequested) {
            return false;
        }
        reelRequested = false;
        return true;
    }

    public static boolean useHeldNow(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null) {
            return false;
        }
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        return true;
    }
}
