package com.jelly.farmhelper.fabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Overlay helpers. World-space ESP rendering is stubbed on 26.3 pending LevelRenderEvents port.
 */
public final class RenderUtils {
    private static final List<WorldBoxMarker> BOXES = new ArrayList<>();
    private static final List<WorldTracerMarker> TRACERS = new ArrayList<>();
    private static final List<WorldTextMarker> WORLD_TEXT = new ArrayList<>();

    private RenderUtils() {
    }

    public static void drawBlockBox(BlockPos pos, int color, long durationTicks) {
        if (pos == null) {
            return;
        }
        drawBox(new AABB(pos), color, durationTicks);
    }

    public static void drawBox(AABB box, int color, long durationTicks) {
        if (box == null) {
            return;
        }
        BOXES.add(new WorldBoxMarker(box, color, System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L));
    }

    public static void drawTracer(Vec3 target, int color, long durationTicks) {
        if (target == null) {
            return;
        }
        TRACERS.add(new WorldTracerMarker(target, color, System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L));
    }

    public static void drawText(Vec3 target, String text, int color, long durationTicks) {
        if (target == null || text == null || text.isBlank()) {
            return;
        }
        WORLD_TEXT.add(new WorldTextMarker(target, text, color, System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L));
    }

    public static void drawCenterTopText(GuiGraphicsExtractor context, String text, int y, int color) {
        Minecraft client = Minecraft.getInstance();
        if (context == null || client == null || client.font == null || text == null) {
            return;
        }
        int width = client.font.width(text);
        int x = Math.max(4, (client.getWindow().getGuiScaledWidth() - width) / 2);
        context.text(client.font, text, x, Math.max(4, y), color, true);
    }

    public static void drawMultiLineText(GuiGraphicsExtractor context, List<String> lines, int x, int y, int color) {
        Minecraft client = Minecraft.getInstance();
        if (context == null || client == null || client.font == null || lines == null || lines.isEmpty()) {
            return;
        }
        int lineY = y;
        for (String line : lines) {
            context.text(client.font, line, x, lineY, color, true);
            lineY += 10;
        }
    }

    public static void renderWorld() {
        // no-op on 26.3 until LevelRenderEvents port
    }

    public static void renderWorldTextAsHud(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (context == null || client == null || client.font == null || WORLD_TEXT.isEmpty()) {
            return;
        }
        int y = 60;
        for (WorldTextMarker marker : WORLD_TEXT) {
            context.text(client.font, Component.literal(marker.text), 8, y, marker.color, true);
            y += 10;
        }
    }

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        cleanup(now, BOXES);
        cleanup(now, TRACERS);
        cleanup(now, WORLD_TEXT);
    }

    private static void cleanup(long now, List<? extends ExpiringMarker> markers) {
        Iterator<? extends ExpiringMarker> it = markers.iterator();
        while (it.hasNext()) {
            if (it.next().expiresAtMs <= now) {
                it.remove();
            }
        }
    }

    private abstract static class ExpiringMarker {
        final long expiresAtMs;

        private ExpiringMarker(long expiresAtMs) {
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final class WorldBoxMarker extends ExpiringMarker {
        final AABB box;
        final int color;

        private WorldBoxMarker(AABB box, int color, long expiresAtMs) {
            super(expiresAtMs);
            this.box = box;
            this.color = color;
        }
    }

    private static final class WorldTracerMarker extends ExpiringMarker {
        final Vec3 target;
        final int color;

        private WorldTracerMarker(Vec3 target, int color, long expiresAtMs) {
            super(expiresAtMs);
            this.target = target;
            this.color = color;
        }
    }

    private static final class WorldTextMarker extends ExpiringMarker {
        final Vec3 target;
        final String text;
        final int color;

        private WorldTextMarker(Vec3 target, String text, int color, long expiresAtMs) {
            super(expiresAtMs);
            this.target = target;
            this.text = text;
            this.color = color;
        }
    }
}
