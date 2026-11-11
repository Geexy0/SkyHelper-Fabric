package com.jelly.farmhelper.fabric.hud;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.FarmHelperFabricClient;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.module.ProfitCalculatorFeatureModule;
import com.jelly.farmhelper.fabric.state.GameStateHandler;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ProfitHudRenderer {
    private ProfitHudRenderer() {
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
        if (!config.enableProfitHud || !config.profitCalculatorEnabled) {
            return;
        }

        int x = Math.max(4, config.profitHudX);
        int y = Math.max(4, config.profitHudY);
        int color = 0xFFF7DCA4;
        String header = "Session Profit";
        String totalLine = String.format(Locale.US, "Total: $%,.0f", ProfitCalculatorFeatureModule.getTotalProfitCoins());
        String hourlyLine = String.format(Locale.US, "Hourly: $%,.0f/h", ProfitCalculatorFeatureModule.getHourlyProfitCoins());
        String bpsLine = String.format(Locale.US, "BPS: %.1f (avg %.1f)",
                com.jelly.farmhelper.fabric.feature.module.BpsTrackerFeatureModule.getCurrentBps(),
                com.jelly.farmhelper.fabric.feature.module.BpsTrackerFeatureModule.getAverageBps());
        String pricingMode = ProfitCalculatorFeatureModule.getBazaarPricingMode();
        String pricingLine = "Pricing: " + pricingMode;

        Map<String, Long> tracked = ProfitCalculatorFeatureModule.getTrackedCounts();
        List<Map.Entry<String, Long>> topEntries = tracked.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed())
                .limit(3)
                .collect(Collectors.toList());

        int width = client.font.width(header);
        width = Math.max(width, client.font.width(totalLine));
        width = Math.max(width, client.font.width(hourlyLine));
        width = Math.max(width, client.font.width(bpsLine));
        width = Math.max(width, client.font.width(pricingLine));
        for (Map.Entry<String, Long> entry : topEntries) {
            width = Math.max(width, client.font.width(entry.getKey() + ": " + entry.getValue()));
        }
        int lines = 5 + topEntries.size();
        int panelWidth = width + 10;
        int panelHeight = lines * 10 + 8;
        drawContext.fill(x - 4, y - 4, x - 4 + panelWidth, y - 4 + panelHeight, 0x78101824);
        drawContext.fill(x - 5, y - 5, x - 4 + panelWidth + 1, y - 4, 0xA03A4A64);
        drawContext.fill(x - 5, y - 4 + panelHeight, x - 4 + panelWidth + 1, y - 3 + panelHeight, 0xA03A4A64);

        drawContext.text(client.font, header, x, y, 0xFF8CD5A8, true);
        y += 10;
        drawContext.text(client.font, totalLine, x, y, color, true);
        y += 10;
        drawContext.text(client.font, hourlyLine, x, y, color, true);
        y += 10;
        drawContext.text(client.font, bpsLine, x, y, 0xFF55FF55, true);
        y += 10;
        drawContext.text(client.font, pricingLine, x, y,
                "live".equalsIgnoreCase(pricingMode) ? 0xFF55FF55 : 0xFFFFAA00, true);
        y += 10;
        for (Map.Entry<String, Long> entry : topEntries) {
            drawContext.text(client.font, entry.getKey() + ": " + entry.getValue(), x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }
}
