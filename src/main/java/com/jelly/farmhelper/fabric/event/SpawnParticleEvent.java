package com.jelly.farmhelper.fabric.event;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

public final class SpawnParticleEvent {
    public final ParticleOptions effect;
    public final boolean longDistance;
    public final Vec3 position;
    public final Vec3 velocity;
    public final int count;

    public SpawnParticleEvent(ParticleOptions effect, boolean longDistance, Vec3 position, Vec3 velocity, int count) {
        this.effect = effect;
        this.longDistance = longDistance;
        this.position = position;
        this.velocity = velocity;
        this.count = count;
    }
}
