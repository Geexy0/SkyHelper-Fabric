package com.jelly.farmhelper.fabric.state;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public class ClientModeController {
    private boolean performanceApplied;
    private int previousMaxFps = 120;
    private int previousViewDistance = 12;

    private boolean pipApplied;
    private int previousWindowX;
    private int previousWindowY;
    private int previousWindowWidth;
    private int previousWindowHeight;
    private boolean previousWindowFullscreen;

    private boolean ungrabApplied;

    public void setPerformanceMode(Minecraft client, boolean enabled, int maxFps, int viewDistance) {
        if (client == null || client.options == null) {
            return;
        }
        if (enabled) {
            if (!performanceApplied) {
                previousMaxFps = client.options.framerateLimit().get();
                previousViewDistance = client.options.renderDistance().get();
                performanceApplied = true;
            }
            client.options.framerateLimit().set(Math.max(10, maxFps));
            client.options.renderDistance().set(Math.max(2, viewDistance));
        } else if (performanceApplied) {
            client.options.framerateLimit().set(Math.max(10, previousMaxFps));
            client.options.renderDistance().set(Math.max(2, previousViewDistance));
            performanceApplied = false;
        }
    }

    public void setPipMode(Minecraft client, boolean enabled) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        Window window = client.getWindow();
        if (enabled) {
            if (!pipApplied) {
                previousWindowX = window.getX();
                previousWindowY = window.getY();
                previousWindowWidth = window.getScreenWidth();
                previousWindowHeight = window.getScreenHeight();
                previousWindowFullscreen = window.isExclusiveFullscreen();
                pipApplied = true;
            }

            if (window.isExclusiveFullscreen()) {
                window.setFullscreen(!window.isExclusiveFullscreen());
            }
            window.setWindowed(420, 252);
            // glfw removed in 26.3 (SDL)
            // glfw removed in 26.3 (SDL)
        } else if (pipApplied) {
            // glfw removed in 26.3 (SDL)
            window.setWindowed(Math.max(320, previousWindowWidth), Math.max(240, previousWindowHeight));
            // glfw removed in 26.3 (SDL)
            if (previousWindowFullscreen && !window.isExclusiveFullscreen()) {
                window.setFullscreen(!window.isExclusiveFullscreen());
            }
            pipApplied = false;
        }
    }

    public void setFreelook(Minecraft client, boolean enabled) {
        FreelookController.getInstance().setEnabled(client, enabled);
    }

    public void setMouseUngrab(Minecraft client, boolean enabled) {
        if (client == null || client.mouseHandler == null || client.options == null) {
            return;
        }
        if (enabled) {
            client.options.pauseOnLostFocus = false;
            client.mouseHandler.releaseMouse();
            ungrabApplied = true;
        } else if (ungrabApplied) {
            client.mouseHandler.grabMouse();
            ungrabApplied = false;
        }
    }
}
