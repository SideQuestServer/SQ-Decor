package com.sqdecor.led;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sqdecor.block.entity.LedBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ChannelPrograms {
    private ChannelPrograms() {}

    public static int run(MinecraftServer server, int channel, int program) {
        LightChannels channels = LightChannels.get(server);
        Map<ResourceKey<Level>, List<BlockPos>> byLevel = new HashMap<>();
        for (GlobalPos member : channels.members(channel)) {
            ServerLevel level = server.getLevel(member.dimension());
            if (level == null || !level.isLoaded(member.pos())) continue;
            if (level.getBlockEntity(member.pos()) instanceof LedBlockEntity be && be.getChannel() == channel) {
                byLevel.computeIfAbsent(member.dimension(), k -> new ArrayList<>()).add(member.pos());
            }
        }
        if (program != 0) channels.set(server, channel, true);
        int count = 0;
        for (Map.Entry<ResourceKey<Level>, List<BlockPos>> entry : byLevel.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            List<BlockPos> leds = entry.getValue();
            leds.sort(Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getY()));
            count += leds.size();
            for (BlockPos p : leds) {
                LedAnimator.stopAt(level, p);
                if (program != 0 && level.getBlockEntity(p) instanceof LedBlockEntity be) be.stopFade();
            }
            int rgb = level.getBlockEntity(leds.get(0)) instanceof LedBlockEntity be ? be.getRgb() : LedBlockEntity.DEFAULT_RGB;
            int lvl = channels.level(channel, 15);
            switch (program) {
                case 1 -> LedAnimator.start(level, leds, LedAnimator.Type.RAINBOW, 0.2, 0, 0, 0, 0);
                case 2 -> LedAnimator.start(level, leds, LedAnimator.Type.CHASE, 4, rgb, 0, Math.max(1, lvl / 4), 0);
                case 3 -> LedAnimator.start(level, leds, LedAnimator.Type.PULSE, 0.5, rgb, 0, lvl, 0);
                case 4 -> LedAnimator.start(level, leds, LedAnimator.Type.BLINK, 1, rgb, 0, lvl, 0);
                default -> {
                    if (channels.isOn(channel)) channels.set(server, channel, true);
                }
            }
        }
        return count;
    }
}
