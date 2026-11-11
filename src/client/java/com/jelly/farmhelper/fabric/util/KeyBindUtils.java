package com.jelly.farmhelper.fabric.util;

import com.jelly.farmhelper.fabric.mixin.client.KeyBindingAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;

public final class KeyBindUtils {
    private static final Map<Integer, java.util.function.Function<Options, KeyMapping>> YAW_KEY_MAP = Map.of(
            0, options -> options.keyUp,
            90, options -> options.keyLeft,
            180, options -> options.keyDown,
            -90, options -> options.keyRight
    );

    private KeyBindUtils() {
    }

    public static void setPressed(KeyMapping key, boolean pressed) {
        if (key == null) {
            return;
        }
        key.setDown(pressed);
    }

    public static void holdAttack(Minecraft client, boolean pressed) {
        if (client == null || client.options == null) {
            return;
        }
        setPressed(client.options.keyAttack, pressed);
    }

    public static void holdUse(Minecraft client, boolean pressed) {
        if (client == null || client.options == null) {
            return;
        }
        setPressed(client.options.keyUse, pressed);
    }

    public static void holdMovement(
            Minecraft client,
            boolean forward,
            boolean back,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak,
            boolean sprint
    ) {
        if (client == null || client.options == null) {
            return;
        }
        Options options = client.options;
        setPressed(options.keyUp, forward);
        setPressed(options.keyDown, back);
        setPressed(options.keyLeft, left);
        setPressed(options.keyRight, right);
        setPressed(options.keyJump, jump);
        setPressed(options.keyShift, sneak);
        setPressed(options.keySprint, sprint);
    }

    public static void stopMovement(Minecraft client, boolean keepAttack) {
        if (client == null || client.options == null) {
            return;
        }
        Options options = client.options;
        setPressed(options.keyUp, false);
        setPressed(options.keyDown, false);
        setPressed(options.keyLeft, false);
        setPressed(options.keyRight, false);
        setPressed(options.keyJump, false);
        setPressed(options.keyShift, false);
        setPressed(options.keySprint, false);
        setPressed(options.keyUse, false);
        if (!keepAttack) {
            setPressed(options.keyAttack, false);
        }
    }

    public static void releaseAll(Minecraft client) {
        stopMovement(client, false);
    }

    public static void leftClick(Minecraft client) {
        if (client == null) {
            return;
        }
        tapKey(client.options == null ? null : client.options.keyAttack);
    }

    public static void rightClick(Minecraft client) {
        if (client == null) {
            return;
        }
        tapKey(client.options == null ? null : client.options.keyUse);
    }

    public static void middleClick(Minecraft client) {
        if (client == null) {
            return;
        }
        tapKey(client.options == null ? null : client.options.keyPickItem);
    }

    public static void holdThese(Minecraft client, boolean withAttack, KeyMapping... keys) {
        if (client == null || client.options == null) {
            return;
        }
        releaseAllExcept(client, keys);
        if (keys != null) {
            for (KeyMapping key : keys) {
                setPressed(key, true);
            }
        }
        if (withAttack) {
            setPressed(client.options.keyAttack, true);
        }
    }

    public static void holdThese(Minecraft client, KeyMapping... keys) {
        holdThese(client, false, keys);
    }

    public static void releaseAllExcept(Minecraft client, KeyMapping... keepPressed) {
        if (client == null || client.options == null) {
            return;
        }
        List<KeyMapping> keep = keepPressed == null ? List.of() : List.of(keepPressed);
        KeyMapping[] all = {
                client.options.keyAttack,
                client.options.keyUse,
                client.options.keyDown,
                client.options.keyUp,
                client.options.keyLeft,
                client.options.keyRight,
                client.options.keyJump,
                client.options.keyShift,
                client.options.keySprint
        };
        for (KeyMapping key : all) {
            if (!keep.contains(key)) {
                setPressed(key, false);
            }
        }
    }

    public static boolean areAllMovementKeysReleased(Minecraft client) {
        if (client == null || client.options == null) {
            return true;
        }
        return !client.options.keyUp.isDown()
                && !client.options.keyDown.isDown()
                && !client.options.keyLeft.isDown()
                && !client.options.keyRight.isDown()
                && !client.options.keyJump.isDown();
    }

    public static List<KeyMapping> getHoldingKeybinds(Minecraft client) {
        if (client == null || client.options == null) {
            return List.of();
        }
        KeyMapping[] all = {
                client.options.keyAttack,
                client.options.keyUse,
                client.options.keyUp,
                client.options.keyDown,
                client.options.keyLeft,
                client.options.keyRight,
                client.options.keyJump,
                client.options.keyShift,
                client.options.keySprint
        };
        List<KeyMapping> pressed = new ArrayList<>();
        for (KeyMapping key : all) {
            if (key.isDown()) {
                pressed.add(key);
            }
        }
        return pressed;
    }

    public static List<KeyMapping> getNeededKeyPresses(Minecraft client, Vec3 origin, Vec3 destination) {
        if (client == null || client.options == null || client.player == null || origin == null || destination == null) {
            return List.of();
        }
        double[] delta = {origin.x - destination.x, origin.z - destination.z};
        float requiredAngle = (float) (Math.atan2(delta[0], -delta[1]) * (180.0 / Math.PI));
        float angleDifference = AngleUtils.normalize180(requiredAngle - client.player.getYRot()) * -1.0f;
        return mapYawToKeys(client.options, angleDifference);
    }

    public static List<KeyMapping> getNeededKeyPresses(Minecraft client, float neededYaw) {
        if (client == null || client.options == null || client.player == null) {
            return List.of();
        }
        float relative = AngleUtils.normalize180(neededYaw - client.player.getYRot()) * -1.0f;
        return mapYawToKeys(client.options, relative);
    }

    public static List<KeyMapping> getOppositeKeys(Minecraft client, List<KeyMapping> keys) {
        if (client == null || client.options == null || keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<KeyMapping> opposite = new ArrayList<>();
        for (KeyMapping key : keys) {
            if (key == client.options.keyUp) opposite.add(client.options.keyDown);
            else if (key == client.options.keyDown) opposite.add(client.options.keyUp);
            else if (key == client.options.keyLeft) opposite.add(client.options.keyRight);
            else if (key == client.options.keyRight) opposite.add(client.options.keyLeft);
        }
        return opposite;
    }

    public static List<KeyMapping> getKeyPressesToDecelerate(Minecraft client, Vec3 origin, Vec3 destination) {
        return getOppositeKeys(client, getNeededKeyPresses(client, origin, destination));
    }

    private static List<KeyMapping> mapYawToKeys(Options options, float relativeYaw) {
        List<KeyMapping> keys = new ArrayList<>();
        for (Map.Entry<Integer, java.util.function.Function<Options, KeyMapping>> entry : YAW_KEY_MAP.entrySet()) {
            int yaw = entry.getKey();
            if (Math.abs(yaw - relativeYaw) < 67.5f || Math.abs(yaw - (relativeYaw + 360.0f)) < 67.5f) {
                keys.add(entry.getValue().apply(options));
            }
        }
        return keys;
    }

    private static void tapKey(KeyMapping keyBinding) {
        if (keyBinding == null) {
            return;
        }
        setPressed(keyBinding, true);
        InputConstants.Key bound = ((KeyBindingAccessor) (Object) keyBinding).farmhelper$getBoundKey();
        if (bound != null) {
            KeyMapping.click(bound);
            KeyMapping.set(bound, true);
            KeyMapping.set(bound, false);
        }
        setPressed(keyBinding, false);
    }
}
