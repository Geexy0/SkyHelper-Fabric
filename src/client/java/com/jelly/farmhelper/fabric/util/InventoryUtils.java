package com.jelly.farmhelper.fabric.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class InventoryUtils {
    private static final Pattern SKYBLOCK_ID_PATTERN = Pattern.compile("id[=:]\"?([A-Z0-9_]{4,})");

    private InventoryUtils() {
    }

    public static int inventoryFillPercent(LocalPlayer player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        int used = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            total++;
            if (!stack.isEmpty()) {
                used++;
            }
        }
        if (total == 0) {
            return 0;
        }
        return (int) (used * 100.0 / total);
    }

    public static boolean holdItemInHotbar(LocalPlayer player, String... nameContains) {
        int slot = findHotbarSlotByName(player, nameContains);
        if (slot < 0) {
            return false;
        }
        player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static int findHotbarSlotByName(LocalPlayer player, String... nameContains) {
        if (player == null || nameContains == null || nameContains.length == 0) {
            return -1;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            for (String probe : nameContains) {
                if (probe != null && !probe.isBlank() && name.contains(probe.toLowerCase(Locale.ROOT))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int findHotbarSlotByHypixelId(LocalPlayer player, String containsId) {
        if (player == null || containsId == null || containsId.isBlank()) {
            return -1;
        }
        String probe = containsId.toUpperCase(Locale.ROOT);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String id = readSkyblockId(stack);
            if (id != null && id.contains(probe)) {
                return i;
            }
        }
        return -1;
    }

    public static int findInventorySlotByName(LocalPlayer player, String nameContains) {
        if (player == null || nameContains == null || nameContains.isBlank()) {
            return -1;
        }
        String probe = nameContains.toLowerCase(Locale.ROOT);
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(probe)) {
                return i;
            }
        }
        return -1;
    }

    public static Optional<Slot> findOpenContainerSlotContains(Minecraft client, String nameContains) {
        if (client == null || client.player == null || client.player.containerMenu == null || nameContains == null) {
            return Optional.empty();
        }
        String probe = nameContains.toLowerCase(Locale.ROOT);
        for (Slot slot : client.player.containerMenu.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(probe)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public static List<String> getItemLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return lore;
        }
        Iterable<Component> lines = stack.getOrDefault(DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY).styledLines();
        for (Component line : lines) {
            lore.add(line.getString());
        }
        return lore;
    }

    public static String readSkyblockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        String raw = customData.toString();
        Matcher matcher = SKYBLOCK_ID_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean hasItemInInventory(LocalPlayer player, String nameContains) {
        return findInventorySlotByName(player, nameContains) >= 0;
    }

    public static boolean hasItemInHotbar(LocalPlayer player, String... names) {
        return findHotbarSlotByName(player, names) >= 0;
    }

    public static int getAmountOfItemInInventory(LocalPlayer player, String exactName) {
        if (player == null || exactName == null || exactName.isBlank()) {
            return 0;
        }
        int amount = 0;
        String expected = exactName.toLowerCase(Locale.ROOT);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).equals(expected)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    public static int getAmountOfItemInInventoryContains(LocalPlayer player, String nameContains) {
        if (player == null || nameContains == null || nameContains.isBlank()) {
            return 0;
        }
        int amount = 0;
        String probe = nameContains.toLowerCase(Locale.ROOT);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(probe)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    public static boolean canFitItemInInventory(LocalPlayer player, String exactName, int amount) {
        if (player == null || amount <= 0) {
            return true;
        }
        int freeCapacity = 0;
        String expected = exactName == null ? "" : exactName.toLowerCase(Locale.ROOT);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                freeCapacity += 64;
                continue;
            }
            if (!expected.isBlank() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).equals(expected)) {
                freeCapacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (freeCapacity >= amount) {
                return true;
            }
        }
        return freeCapacity >= amount;
    }

    public static int getRancherBootSpeed(LocalPlayer player) {
        if (player == null) {
            return -1;
        }
        Pattern speedPattern = Pattern.compile("current speed cap:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (String loreLine : getItemLore(stack)) {
                Matcher matcher = speedPattern.matcher(loreLine.replaceAll("§.", ""));
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return -1;
    }

    public static boolean isInventoryLoaded(Minecraft client) {
        if (client == null || client.player == null || client.player.containerMenu == null) {
            return false;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        return !client.player.containerMenu.slots.isEmpty();
    }

    public static String getInventoryName(Minecraft client) {
        if (client == null || client.gui.screen() == null || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }
        return screen.getTitle() == null ? null : screen.getTitle().getString();
    }

    public static void openInventory(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        client.gui.setScreen(new InventoryScreen(client.player));
    }

    public static void clickSlotWithId(Minecraft client, int syncId, int slotId, ClickType clickType, ClickMode clickMode) {
        if (client == null || client.player == null || client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(
                syncId,
                slotId,
                clickType.button,
                clickMode.actionType,
                client.player
        );
    }

    public static void clickContainerSlot(Minecraft client, int slotId, ClickType clickType, ClickMode clickMode) {
        if (client == null || client.player == null || client.player.containerMenu == null) {
            return;
        }
        clickSlotWithId(client, client.player.containerMenu.containerId, slotId, clickType, clickMode);
    }

    public static void clickSlot(Minecraft client, int slotId, ClickType clickType, ClickMode clickMode) {
        if (client == null || client.player == null || client.player.inventoryMenu == null) {
            return;
        }
        clickSlotWithId(client, client.player.inventoryMenu.containerId, slotId, clickType, clickMode);
    }

    public static void swapSlots(Minecraft client, int slot, int hotbarSlot) {
        if (client == null || client.player == null || client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(
                client.player.containerMenu.containerId,
                slot,
                hotbarSlot,
                net.minecraft.world.inventory.ContainerInput.SWAP,
                client.player
        );
    }

    public static ArrayList<Slot> getIndexesOfItemsFromContainer(Minecraft client, java.util.function.Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        if (client == null || client.player == null || client.player.containerMenu == null || predicate == null) {
            return indexes;
        }
        for (Slot slot : client.player.containerMenu.slots) {
            if (slot != null && slot.hasItem() && predicate.test(slot)) {
                indexes.add(slot);
            }
        }
        return indexes;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromInventory(Minecraft client, java.util.function.Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        if (client == null || client.player == null || client.player.inventoryMenu == null || predicate == null) {
            return indexes;
        }
        for (Slot slot : client.player.inventoryMenu.slots) {
            if (slot != null && slot.hasItem() && predicate.test(slot)) {
                indexes.add(slot);
            }
        }
        return indexes;
    }

    public enum ClickType {
        LEFT(0),
        RIGHT(1);

        private final int button;

        ClickType(int button) {
            this.button = button;
        }
    }

    public enum ClickMode {
        PICKUP(net.minecraft.world.inventory.ContainerInput.PICKUP),
        QUICK_MOVE(net.minecraft.world.inventory.ContainerInput.QUICK_MOVE),
        SWAP(net.minecraft.world.inventory.ContainerInput.SWAP),
        CLONE(net.minecraft.world.inventory.ContainerInput.CLONE),
        THROW(net.minecraft.world.inventory.ContainerInput.THROW),
        QUICK_CRAFT(net.minecraft.world.inventory.ContainerInput.QUICK_CRAFT),
        PICKUP_ALL(net.minecraft.world.inventory.ContainerInput.PICKUP_ALL);

        private final net.minecraft.world.inventory.ContainerInput actionType;

        ClickMode(net.minecraft.world.inventory.ContainerInput actionType) {
            this.actionType = actionType;
        }
    }
}
