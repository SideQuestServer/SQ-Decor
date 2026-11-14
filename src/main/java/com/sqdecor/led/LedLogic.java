package com.sqdecor.led;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class LedLogic {
    private LedLogic() {}

    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    public static final int DEFAULT_LEVEL = 8;
    public static final int MAX_NETWORK = 256;
    private static final String REMEMBER_KEY = "sqdecor_led_level";

    public static int placementLevel(Player player) {
        if (player == null) return DEFAULT_LEVEL;
        CompoundTag data = player.getPersistentData();
        return data.contains(REMEMBER_KEY) ? Mth.clamp(data.getInt(REMEMBER_KEY), 0, 15) : DEFAULT_LEVEL;
    }

    public static void remember(Player player, int level) {
        if (player != null) player.getPersistentData().putInt(REMEMBER_KEY, Mth.clamp(level, 0, 15));
    }

    public static void applyLevel(Level level, BlockPos pos, int value, boolean manual) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LedBlock led) || !led.switchable()) return;
        int n = Mth.clamp(value, 0, 15);
        if (manual && level.getBlockEntity(pos) instanceof LedBlockEntity be) {
            be.stopFade();
            if (n > 0) be.setOnLevel(n);
        }
        if (state.getValue(LIGHT_LEVEL) != n) {
            level.setBlock(pos, state.setValue(LIGHT_LEVEL, n), 3);
        }
    }

    public static void cycle(Level level, BlockPos pos, BlockState state, Player player) {
        int n = (state.getValue(LIGHT_LEVEL) + 1) % 16;
        applyLevel(level, pos, n, true);
        remember(player, n);
        showLevel(player, n);
        ModSounds.playLevel(level, pos, n);
    }

    public static void showLevel(Player player, int n) {
        if (player == null) return;
        Component bar = Component.literal("|".repeat(n)).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("|".repeat(15 - n)).withStyle(ChatFormatting.DARK_GRAY));
        player.displayClientMessage(
                Component.translatable("message.sqdecor.led_brightness", n).append(" ").append(bar), true);
    }

    public static void updateRedstone(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof LedBlockEntity be)) return;
        RedstoneMode mode = be.getRedstoneMode();
        if (mode == RedstoneMode.IGNORE || be.getAutoMode() != AutoMode.OFF) return;
        int signal = level.getBestNeighborSignal(pos);
        if (signal == be.getLastSignal()) return;
        be.setLastSignal(signal);
        be.fadeTo(mode == RedstoneMode.ANALOG ? signal : (signal > 0 ? be.getOnLevel() : 0));
    }

    public static void setAutoMode(Level level, BlockPos pos, AutoMode mode) {
        if (!(level.getBlockEntity(pos) instanceof LedBlockEntity be)) return;
        be.setAutoMode(mode);
        if (mode == AutoMode.OFF) {
            be.setLastSignal(-1);
            updateRedstone(level, pos);
        }
    }

    public static void setRedstoneMode(Level level, BlockPos pos, RedstoneMode mode) {
        if (!(level.getBlockEntity(pos) instanceof LedBlockEntity be)) return;
        be.setRedstoneMode(mode);
        updateRedstone(level, pos);
    }

    public static boolean isNode(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LedBlock led && led.switchable();
    }

    public static List<BlockPos> network(Level level, BlockPos start) {
        List<BlockPos> leds = new ArrayList<>();
        if (level == null || !level.isLoaded(start) || !isNode(level, start)) return leds;

        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos first = start.immutable();
        seen.add(first);
        queue.add(first);

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            leds.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.size() >= MAX_NETWORK) break;
                if (!seen.contains(n) && level.isLoaded(n) && isNode(level, n)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        leds.sort(Comparator.comparingInt((BlockPos p) -> p.getX())
                .thenComparingInt(p -> p.getY())
                .thenComparingInt(p -> p.getZ()));
        return leds;
    }
}
