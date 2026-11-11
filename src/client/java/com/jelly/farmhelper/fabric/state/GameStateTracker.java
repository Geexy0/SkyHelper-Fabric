package com.jelly.farmhelper.fabric.state;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class GameStateTracker {
    private Vec3 lastPlayerPos;
    private int stationaryTicks;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            lastPlayerPos = null;
            stationaryTicks = 0;
            return;
        }

        Vec3 current = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        if (lastPlayerPos != null && current.distanceToSqr(lastPlayerPos) < 0.0001D) {
            stationaryTicks++;
        } else {
            stationaryTicks = 0;
        }

        lastPlayerPos = current;
    }

    public int getStationaryTicks() {
        return stationaryTicks;
    }
}
