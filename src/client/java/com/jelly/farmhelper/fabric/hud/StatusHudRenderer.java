package com.jelly.farmhelper.fabric.hud;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.FarmHelperFabricClient;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.module.AutoReconnectFeatureModule;
import com.jelly.farmhelper.fabric.feature.module.BpsTrackerFeatureModule;
import com.jelly.farmhelper.fabric.feature.module.LagDetectorFeatureModule;
import com.jelly.farmhelper.fabric.feature.module.SchedulerFeatureModule;
import com.jelly.farmhelper.fabric.feature.module.UsageStatsFeatureModule;
import com.jelly.farmhelper.fabric.macro.MacroState;
import com.jelly.farmhelper.fabric.state.GameStateHandler;
import com.jelly.farmhelper.fabric.util.RenderUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class StatusHudRenderer {
    private StatusHudRenderer() {
    }

    public static void render(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        if (client.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (FarmHelperFabricClient.getGameStateHandler().getLocation() != GameStateHandler.Location.GARDEN) {
            return;
        }
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        boolean macroToggled = FarmHelperFabric.getMacroController().isToggled();
        boolean reconnectPending = AutoReconnectFeatureModule.hasPendingReconnect();
        if (!config.enableStatusHud) {
            if (macroToggled || reconnectPending) {
                renderCompactIndicator(drawContext, client);
            }
            return;
        }

        int x = Math.max(4, config.statusHudX);
        int y = Math.max(4, config.statusHudY);
        int color = 0xFFFFFFFF;

        drawContext.text(client.font,
                "FarmHelper Fabric", x, y, 0xFF55FF55, true);
        y += 10;

        drawContext.text(client.font,
                "Macro: " + FarmHelperFabric.getMacroController().getState(), x, y, color, true);
        y += 10;

        drawContext.text(client.font,
                "Runtime ticks: " + FarmHelperFabric.getMacroController().getRuntimeTicks(), x, y, color, true);
        y += 10;
        drawContext.text(client.font,
                "Macro type: " + config.macroType,
                x, y, color, true);
        y += 10;

        SchedulerFeatureModule.RuntimeState schedulerState = SchedulerFeatureModule.getRuntimeState();
        long schedulerRemainingTicks = SchedulerFeatureModule.getRuntimeRemainingTicks();
        drawContext.text(client.font,
                "Scheduler: " + schedulerState + " (" + formatTicks(schedulerRemainingTicks) + ")",
                x, y, schedulerState == SchedulerFeatureModule.RuntimeState.BREAK ? 0xFF55AAFF : color, true);
        y += 10;

        if (reconnectPending) {
            long remaining = AutoReconnectFeatureModule.getRemainingReconnectTicks(System.currentTimeMillis() / 50L);
            drawContext.text(client.font,
                    "Reconnect: " + formatTicks(remaining) + " ("
                            + AutoReconnectFeatureModule.getCurrentAttempt() + "/"
                            + AutoReconnectFeatureModule.getMaxAttemptsSnapshot() + ")",
                    x, y, 0xFFFFAA55, true);
            y += 10;
        }

        String failsafe = FarmHelperFabric.getFailsafeManager().getActiveFailsafe().map(Enum::name).orElse("NONE");
        drawContext.text(client.font,
                "Failsafe: " + failsafe, x, y, failsafe.equals("NONE") ? color : 0xFFFF5555, true);
        y += 10;
        if (!failsafe.equals("NONE")) {
            drawContext.text(client.font,
                    "Reason: " + FarmHelperFabric.getFailsafeManager().getActiveReason(), x, y, 0xFFFFAA55, true);
            y += 10;
        }

        drawContext.text(client.font,
                "Pattern: " + FarmHelperFabric.getConfigManager().getConfig().macroPattern
                        + " Dir: " + FarmHelperFabricClient.getMovementMacroExecutor().getDirectionName(),
                x, y, color, true);
        y += 10;

        drawContext.text(client.font,
                "Stationary ticks: " + FarmHelperFabricClient.getGameStateTracker().getStationaryTicks(), x, y, color, true);
        y += 10;
        drawContext.text(client.font,
                "Location: " + FarmHelperFabricClient.getGameStateHandler().getLocation()
                        + " Plot: " + FarmHelperFabricClient.getGameStateHandler().getCurrentPlotName(),
                x, y, color, true);
        y += 10;
        drawContext.text(client.font,
                "Pests: " + FarmHelperFabricClient.getGameStateHandler().getTotalPests()
                        + " (top plot " + FarmHelperFabricClient.getGameStateHandler().getMostInfestedPlot() + ")",
                x, y, color, true);
        y += 10;

        drawContext.text(client.font,
                String.format("BPS: %.1f (avg %.1f)%s",
                        BpsTrackerFeatureModule.getCurrentBps(),
                        BpsTrackerFeatureModule.getAverageBps(),
                        BpsTrackerFeatureModule.isPaused() ? " paused" : ""),
                x, y, color, true);
        y += 10;

        drawContext.text(client.font,
                String.format("TPS: %.1f  Lag: %s (%dms)",
                        LagDetectorFeatureModule.getEstimatedTps(),
                        LagDetectorFeatureModule.isLagging() ? "YES" : "NO",
                        LagDetectorFeatureModule.getMillisSincePacket()),
                x, y, LagDetectorFeatureModule.isLagging() ? 0xFFFFAA00 : color, true);
        y += 10;

        if (config.showStatsTitle) {
            drawContext.text(client.font, "Usage Stats", x, y, 0xFFF7DCA4, true);
            y += 10;
        }
        if (config.showStatsSession) {
            drawContext.text(client.font,
                    "Session: " + UsageStatsFeatureModule.formatTicksAsHours(UsageStatsFeatureModule.getSessionMacroTicks()),
                    x, y, color, true);
            y += 10;
        }
        if (config.showStats24H) {
            drawContext.text(client.font,
                    "24h: " + UsageStatsFeatureModule.formatHours(UsageStatsFeatureModule.getActiveHoursInWindow(24L * 60L * 60L * 1000L)),
                    x, y, 0xFF55FF55, true);
            y += 10;
        }
        if (config.showStats7D) {
            drawContext.text(client.font,
                    "7d: " + UsageStatsFeatureModule.formatHours(UsageStatsFeatureModule.getActiveHoursInWindow(7L * 24L * 60L * 60L * 1000L)),
                    x, y, 0xFF55FF55, true);
            y += 10;
        }
        if (config.showStats30D) {
            drawContext.text(client.font,
                    "30d: " + UsageStatsFeatureModule.formatHours(UsageStatsFeatureModule.getActiveHoursInWindow(30L * 24L * 60L * 60L * 1000L)),
                    x, y, 0xFF55FF55, true);
            y += 10;
        }
        if (config.showStatsLifetime) {
            drawContext.text(client.font,
                    "Lifetime: " + UsageStatsFeatureModule.formatTicksAsHours(UsageStatsFeatureModule.getLifetimeMacroTicks()),
                    x, y, 0xFF55FF55, true);
            y += 10;
        }
        if (config.showStatsFailsafes) {
            drawContext.text(client.font,
                    "Failsafes: session " + UsageStatsFeatureModule.getSessionFailsafes()
                            + " / lifetime " + UsageStatsFeatureModule.getLifetimeFailsafeTriggers(),
                    x, y, 0xFFFFAA55, true);
        }
        RenderUtils.renderWorldTextAsHud(drawContext);
    }

    private static void renderCompactIndicator(GuiGraphicsExtractor drawContext, Minecraft client) {
        MacroState state = FarmHelperFabric.getMacroController().getState();
        String line = "FarmHelper [" + state.name() + "] " + FarmHelperFabricClient.getMovementMacroExecutor().getDirectionName();
        int textWidth = client.font.width(line);
        int x = Math.max(4, (client.getWindow().getGuiScaledWidth() - textWidth) / 2);
        int y = 6;
        int color = state == MacroState.FARMING ? 0xFF55FF55 : 0xFFFFAA55;
        drawContext.text(client.font, Component.literal(line), x, y, color, true);
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, ticks) / 20L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
