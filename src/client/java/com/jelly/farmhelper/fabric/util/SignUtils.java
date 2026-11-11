package com.jelly.farmhelper.fabric.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public final class SignUtils {
    private SignUtils() {
    }

    public static List<String> readLines(BlockEntity blockEntity) {
        List<String> lines = new ArrayList<>();
        if (!(blockEntity instanceof SignBlockEntity sign)) {
            return lines;
        }
        for (Component text : sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false)) {
            lines.add(text.getString());
        }
        return lines;
    }

    public static List<String> readLinesAt(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return List.of();
        }
        BlockEntity blockEntity = client.level.getBlockEntity(pos);
        return readLines(blockEntity);
    }

    public static boolean anyLineContains(Minecraft client, BlockPos pos, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String probe = text.toLowerCase(Locale.ROOT);
        for (String line : readLinesAt(client, pos)) {
            if (line.toLowerCase(Locale.ROOT).contains(probe)) {
                return true;
            }
        }
        return false;
    }
}
