package com.jelly.farmhelper.fabric.util;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class FailsafeUtils {
    private FailsafeUtils() {
    }

    public static void suppressIntentionalAutomationPackets(long teleportTicks, long rotationTicks, long itemTicks, String reason) {
        FarmHelperFabric.getFailsafeManager().suppressPacketChecks(teleportTicks, rotationTicks, itemTicks, reason);
    }

    public static void bringWindowToFront() {
        try {
            Robot robot = new Robot();
            int modifier = System.getProperty("os.name", "").toLowerCase().contains("mac")
                    ? KeyEvent.VK_META
                    : KeyEvent.VK_ALT;
            robot.keyPress(modifier);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(100);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(modifier);
        } catch (AWTException ignored) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void playClientAlert() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        if (client.getSoundManager() != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f));
        }
    }
}
