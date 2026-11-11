package com.jelly.farmhelper.fabric.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Overlay helpers. World markers are projected to HUD space on Minecraft 26.3
 * (Fabric world-render events are not available on this Fabric API line).
 */
public final class RenderUtils {
    private static final List<WorldBoxMarker> BOXES = new ArrayList<>();
    private static final List<WorldTracerMarker> TRACERS = new ArrayList<>();
    private static final List<WorldTextMarker> WORLD_TEXT = new ArrayList<>();
    private static final Matrix4f PROJECTION_SCRATCH = new Matrix4f();
    private static final Vector4f CLIP_SCRATCH = new Vector4f();
    private static final int MAX_MARKERS_PER_KIND = 64;

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
        if (BOXES.size() >= MAX_MARKERS_PER_KIND) {
            BOXES.remove(0);
        }
        BOXES.add(new WorldBoxMarker(box, color, System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L));
    }

    public static void drawTracer(Vec3 target, int color, long durationTicks) {
        if (target == null) {
            return;
        }
        if (TRACERS.size() >= MAX_MARKERS_PER_KIND) {
            TRACERS.remove(0);
        }
        TRACERS.add(new WorldTracerMarker(target, color, System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L));
    }

    public static void drawText(Vec3 target, String text, int color, long durationTicks) {
        if (target == null || text == null || text.isBlank()) {
            return;
        }
        if (WORLD_TEXT.size() >= MAX_MARKERS_PER_KIND) {
            WORLD_TEXT.remove(0);
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

    /**
     * Kept for call-site compatibility. Drawing happens in {@link #renderWorldOverlaysAsHud}.
     */
    public static void renderWorld() {
        // World-space GL path unavailable on this Fabric API; HUD projection draws instead.
    }

    public static void renderWorldTextAsHud(GuiGraphicsExtractor context) {
        renderWorldOverlaysAsHud(context);
    }

    public static void renderWorldOverlaysAsHud(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (context == null || client == null || client.font == null || client.gameRenderer == null) {
            return;
        }
        if (BOXES.isEmpty() && TRACERS.isEmpty() && WORLD_TEXT.isEmpty()) {
            return;
        }

        Camera camera = client.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) {
            renderFallbackList(context, client);
            return;
        }

        int guiWidth = client.getWindow().getGuiScaledWidth();
        int guiHeight = client.getWindow().getGuiScaledHeight();
        Vec3 camPos = camera.position();
        Matrix4f viewProj = camera.getViewRotationProjectionMatrix(PROJECTION_SCRATCH);

        for (WorldBoxMarker marker : BOXES) {
            ScreenPoint center = project(viewProj, camPos, marker.box.getCenter(), guiWidth, guiHeight);
            if (center == null) {
                continue;
            }
            ScreenPoint min = project(viewProj, camPos, new Vec3(marker.box.minX, marker.box.minY, marker.box.minZ), guiWidth, guiHeight);
            ScreenPoint max = project(viewProj, camPos, new Vec3(marker.box.maxX, marker.box.maxY, marker.box.maxZ), guiWidth, guiHeight);
            int half = 6;
            if (min != null && max != null) {
                half = Math.max(4, Math.min(28, Math.max(Math.abs(max.x - min.x), Math.abs(max.y - min.y)) / 2));
            }
            drawScreenBox(context, center.x, center.y, half, marker.color);
        }

        int crossX = guiWidth / 2;
        int crossY = guiHeight / 2;
        for (WorldTracerMarker marker : TRACERS) {
            ScreenPoint target = project(viewProj, camPos, marker.target, guiWidth, guiHeight);
            if (target == null) {
                continue;
            }
            drawScreenLine(context, crossX, crossY, target.x, target.y, marker.color);
            context.fill(target.x - 2, target.y - 2, target.x + 3, target.y + 3, marker.color);
        }

        for (WorldTextMarker marker : WORLD_TEXT) {
            ScreenPoint target = project(viewProj, camPos, marker.target, guiWidth, guiHeight);
            if (target == null) {
                continue;
            }
            int textWidth = client.font.width(marker.text);
            context.text(client.font, marker.text, target.x - textWidth / 2, target.y - 12, marker.color, true);
        }
    }

    private static void renderFallbackList(GuiGraphicsExtractor context, Minecraft client) {
        int y = 60;
        for (WorldTextMarker marker : WORLD_TEXT) {
            context.text(client.font, Component.literal(marker.text), 8, y, marker.color, true);
            y += 10;
        }
        for (WorldBoxMarker marker : BOXES) {
            Vec3 c = marker.box.getCenter();
            String line = String.format("box (%.0f, %.0f, %.0f)", c.x, c.y, c.z);
            context.text(client.font, Component.literal(line), 8, y, marker.color, true);
            y += 10;
        }
        for (WorldTracerMarker marker : TRACERS) {
            String line = String.format("tracer (%.0f, %.0f, %.0f)", marker.target.x, marker.target.y, marker.target.z);
            context.text(client.font, Component.literal(line), 8, y, marker.color, true);
            y += 10;
        }
    }

    private static ScreenPoint project(Matrix4f viewProj, Vec3 camPos, Vec3 world, int guiWidth, int guiHeight) {
        CLIP_SCRATCH.set(
                (float) (world.x - camPos.x),
                (float) (world.y - camPos.y),
                (float) (world.z - camPos.z),
                1.0f
        );
        viewProj.transform(CLIP_SCRATCH);
        if (CLIP_SCRATCH.w <= 0.05f) {
            return null;
        }
        float ndcX = CLIP_SCRATCH.x / CLIP_SCRATCH.w;
        float ndcY = CLIP_SCRATCH.y / CLIP_SCRATCH.w;
        if (ndcX < -1.35f || ndcX > 1.35f || ndcY < -1.35f || ndcY > 1.35f) {
            return null;
        }
        int x = Math.round((ndcX * 0.5f + 0.5f) * guiWidth);
        int y = Math.round((1.0f - (ndcY * 0.5f + 0.5f)) * guiHeight);
        return new ScreenPoint(x, y);
    }

    private static void drawScreenBox(GuiGraphicsExtractor context, int cx, int cy, int half, int color) {
        int x1 = cx - half;
        int y1 = cy - half;
        int x2 = cx + half;
        int y2 = cy + half;
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
        int fill = (color & 0x00FFFFFF) | 0x33000000;
        context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    private static void drawScreenLine(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        int steps = 0;
        int maxSteps = Math.max(dx, dy) + 2;
        while (steps++ <= maxSteps) {
            context.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
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

    private record ScreenPoint(int x, int y) {
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
