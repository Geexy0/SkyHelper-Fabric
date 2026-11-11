package com.jelly.farmhelper.fabric.state;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class FreelookController {
    private static final FreelookController INSTANCE = new FreelookController();

    private boolean active;
    private CameraType previousPerspective = CameraType.FIRST_PERSON;
    private float cameraYaw;
    private float cameraPitch;
    private float cameraPrevYaw;
    private float cameraPrevPitch;
    private float distance = 4.0f;
    private FreelookController() {
    }

    public static FreelookController getInstance() {
        return INSTANCE;
    }

    public void setEnabled(Minecraft client, boolean enabled) {
        if (client == null || client.options == null) {
            return;
        }

        if (enabled) {
            if (!active) {
                previousPerspective = client.options.getCameraType();
                syncFromPlayer(client.player);
                distance = 4.0f;
            }
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            active = true;
            return;
        }

        if (!active) {
            return;
        }
        client.options.setCameraType(previousPerspective);
        active = false;
    }

    public void syncFromPlayer(LocalPlayer player) {
        if (player == null) {
            return;
        }
        cameraYaw = player.getYRot();
        cameraPitch = player.getXRot();
        cameraPrevYaw = cameraYaw;
        cameraPrevPitch = cameraPitch;
    }

    public void onMouseLook(double deltaX, double deltaY) {
        if (!active) {
            return;
        }
        cameraPrevYaw = cameraYaw;
        cameraPrevPitch = cameraPitch;
        cameraYaw += (float) (deltaX * 0.15f);
        cameraPitch += (float) (deltaY * 0.15f);
        cameraYaw = Mth.wrapDegrees(cameraYaw);
        cameraPitch = Mth.clamp(cameraPitch, -90.0f, 90.0f);
    }

    public void adjustDistance(double scrollDelta) {
        if (!active || scrollDelta == 0.0) {
            return;
        }
        distance = Mth.clamp(distance - (float) scrollDelta, 1.0f, 20.0f);
    }

    public boolean isActive() {
        return active;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public float getCameraPrevYaw() {
        return cameraPrevYaw;
    }

    public float getCameraPrevPitch() {
        return cameraPrevPitch;
    }

    public float getDistance() {
        return distance;
    }

    public void applyModelPose(Minecraft client) {
        if (!active || client == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        // Keep model/head aligned to real player yaw while freelook camera rotates separately.
        float yaw = player.getYRot();
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
    }
}
