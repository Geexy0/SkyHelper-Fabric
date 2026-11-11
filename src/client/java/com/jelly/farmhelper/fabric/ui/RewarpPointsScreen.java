package com.jelly.farmhelper.fabric.ui;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.config.struct.RewarpPoint;
import com.jelly.farmhelper.fabric.util.Chat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class RewarpPointsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final FarmHelperConfig config;
    private final int requestedPage;
    private final List<RowData> rows = new ArrayList<>();

    private int currentPage;
    private int rowsPerPage;
    private Button previousButton;
    private Button nextButton;

    public RewarpPointsScreen(Screen parent) {
        this(parent, 0);
    }

    public RewarpPointsScreen(Screen parent, int page) {
        super(Component.literal("FarmHelper - Rewarp Points"));
        this.parent = parent;
        this.requestedPage = Math.max(0, page);
        this.config = FarmHelperFabric.getConfigManager().getConfig();
    }

    @Override
    protected void init() {
        rows.clear();
        rowsPerPage = Math.max(1, (height - 110) / ROW_HEIGHT);
        currentPage = Mth.clamp(requestedPage, 0, maxPage());

        addRenderableWidget(Button.builder(Component.literal("Add Current Position"), btn -> addCurrentPosition())
                .bounds(16, 34, 160, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear All"), btn -> clearAllPoints())
                .bounds(182, 34, 90, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Back"), btn -> onClose())
                .bounds(width - 84, 10, 70, 20)
                .build());

        previousButton = addRenderableWidget(Button.builder(Component.literal("< Prev"), btn -> reopen(currentPage - 1))
                .bounds(width - 196, 34, 88, 20)
                .build());
        nextButton = addRenderableWidget(Button.builder(Component.literal("Next >"), btn -> reopen(currentPage + 1))
                .bounds(width - 102, 34, 88, 20)
                .build());

        rebuildRows();
        updatePageButtons();
    }

    @Override
    public void onClose() {
        FarmHelperFabric.getConfigManager().save();
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractBackground(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.text(font, Component.literal("Rewarp Point Manager"), 16, 12, 0xFFE9EEF8, false);
        context.text(
                font,
                Component.literal("Add from your current location, rename points, and remove unwanted entries."),
                16,
                22,
                0xFFABB7C8,
                false
        );

        int startIndex = currentPage * rowsPerPage;
        if (config.rewarpPoints.isEmpty()) {
            context.text(font, Component.literal("No rewarp points configured."), 16, 70, 0xFFABB7C8, false);
        } else {
            for (RowData row : rows) {
                if (row.index < 0 || row.index >= config.rewarpPoints.size()) {
                    continue;
                }
                RewarpPoint point = config.rewarpPoints.get(row.index);
                String label = "#" + (row.index + 1) + " " + point.displayName(row.index + 1);
                String coords = String.format("XYZ: %d %d %d   Yaw/Pitch: %.1f / %.1f", point.x, point.y, point.z, point.yaw, point.pitch);
                context.text(font, Component.literal(label), row.nameFieldX, row.y - 9, 0xFFD7E4F8, false);
                context.text(font, Component.literal(coords), row.nameFieldX + row.nameFieldWidth + 10, row.y + 6, 0xFF9DB0C9, false);
            }
        }

        String pageText = "Page " + (currentPage + 1) + "/" + (maxPage() + 1)
                + "  (" + config.rewarpPoints.size() + " total)";
        context.text(font, Component.literal(pageText), width - 230, 60, 0xFFABB7C8, false);
        context.text(font, Component.literal("Tip: duplicate coordinates are rejected."), 16, height - 18, 0xFF8EA2BB, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean dblClick) {
        return super.mouseClicked(click, dblClick);
    }

    private void rebuildRows() {
        int start = currentPage * rowsPerPage;
        int end = Math.min(config.rewarpPoints.size(), start + rowsPerPage);
        int y = 86;
        int nameFieldX = 16;
        int nameFieldWidth = 190;

        for (int pointIndex = start; pointIndex < end; pointIndex++) {
            RewarpPoint point = config.rewarpPoints.get(pointIndex);
            point.normalizeInPlace(pointIndex + 1);
            int rowY = y;

            EditBox field = new EditBox(
                    font,
                    nameFieldX,
                    rowY,
                    nameFieldWidth,
                    20,
                    Component.literal("Name")
            );
            field.setMaxLength(48);
            field.setValue(point.displayName(pointIndex + 1));
            int indexForCallback = pointIndex;
            field.setResponder(text -> {
                if (indexForCallback < 0 || indexForCallback >= config.rewarpPoints.size()) {
                    return;
                }
                RewarpPoint target = config.rewarpPoints.get(indexForCallback);
                target.name = RewarpPoint.normalizeName(text, indexForCallback + 1);
                FarmHelperFabric.getConfigManager().save();
            });
            addRenderableWidget(field);

            addRenderableWidget(Button.builder(Component.literal("Remove"), btn -> removeAt(indexForCallback))
                    .bounds(nameFieldX + nameFieldWidth + 230, rowY, 74, 20)
                    .build());

            rows.add(new RowData(indexForCallback, rowY, nameFieldX, nameFieldWidth));
            y += ROW_HEIGHT;
        }
    }

    private void addCurrentPosition() {
        int previousCount = config.rewarpPoints.size();
        if (!addCurrentPlayerPosition()) {
            return;
        }
        int targetPage = Math.max(0, (Math.max(previousCount, config.rewarpPoints.size()) - 1) / Math.max(1, rowsPerPage));
        reopen(targetPage);
    }

    private void removeAt(int index) {
        if (index < 0 || index >= config.rewarpPoints.size()) {
            return;
        }
        RewarpPoint removed = config.rewarpPoints.remove(index);
        FarmHelperFabric.getConfigManager().save();
        Chat.info("Removed rewarp point: " + removed.displayName(index + 1));
        reopen(Math.min(currentPage, maxPage()));
    }

    private void clearAllPoints() {
        if (config.rewarpPoints.isEmpty()) {
            Chat.info("No rewarp points to clear");
            return;
        }
        config.rewarpPoints.clear();
        FarmHelperFabric.getConfigManager().save();
        Chat.info("Cleared all rewarp points");
        reopen(0);
    }

    private void reopen(int targetPage) {
        if (minecraft != null) {
            minecraft.gui.setScreen(new RewarpPointsScreen(parent, Math.max(0, targetPage)));
        }
    }

    private int maxPage() {
        if (config.rewarpPoints.isEmpty()) {
            return 0;
        }
        return Math.max(0, (config.rewarpPoints.size() - 1) / Math.max(1, rowsPerPage));
    }

    private void updatePageButtons() {
        if (previousButton != null) {
            previousButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = currentPage < maxPage();
        }
    }

    public static boolean addCurrentPlayerPosition() {
        Minecraft client = Minecraft.getInstance();
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (client == null || client.player == null) {
            Chat.info("Join a world before adding a rewarp point");
            return false;
        }

        RewarpPoint newPoint = new RewarpPoint(
                null,
                client.player.getBlockX(),
                client.player.getBlockY(),
                client.player.getBlockZ(),
                client.player.getYRot(),
                client.player.getXRot()
        );
        for (int i = 0; i < config.rewarpPoints.size(); i++) {
            RewarpPoint existing = config.rewarpPoints.get(i);
            if (existing.x == newPoint.x && existing.y == newPoint.y && existing.z == newPoint.z) {
                Chat.info("Rewarp already exists: " + existing.displayName(i + 1));
                return false;
            }
        }

        newPoint.normalizeInPlace(config.rewarpPoints.size() + 1);
        config.rewarpPoints.add(newPoint);
        FarmHelperFabric.getConfigManager().save();
        Chat.info("Added rewarp point: " + newPoint.displayName(config.rewarpPoints.size()));
        return true;
    }

    private static final class RowData {
        private final int index;
        private final int y;
        private final int nameFieldX;
        private final int nameFieldWidth;

        private RowData(int index, int y, int nameFieldX, int nameFieldWidth) {
            this.index = index;
            this.y = y;
            this.nameFieldX = nameFieldX;
            this.nameFieldWidth = nameFieldWidth;
        }
    }
}
