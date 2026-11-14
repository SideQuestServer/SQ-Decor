package com.sqdecor.led;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.SQDecor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class LightChannels extends SavedData {
    private static final String NAME = SQDecor.MODID + "_light_channels";
    private static final Factory<LightChannels> FACTORY = new Factory<>(LightChannels::new, LightChannels::load);
    private static final int MAX_CHANNEL = 0xFFFFFF;

    private final IntSet on = new IntOpenHashSet();
    private final Int2IntMap levels = new Int2IntOpenHashMap();
    private final Int2ObjectMap<Set<GlobalPos>> members = new Int2ObjectOpenHashMap<>();

    public static LightChannels get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable
    public static LightChannels get(Level level) {
        MinecraftServer server = level.getServer();
        return level.isClientSide || server == null ? null : get(server);
    }

    public static int newChannel(RandomSource random) {
        return 1 + random.nextInt(MAX_CHANNEL);
    }

    public static Component label(int channel) {
        return Component.literal(String.format("%06X", channel & MAX_CHANNEL));
    }

    public boolean isOn(int channel) {
        return on.contains(channel);
    }

    public int level(int channel, int fallback) {
        return levels.getOrDefault(channel, fallback);
    }

    public void dim(MinecraftServer server, int channel, int value) {
        if (channel == 0) return;
        if (value <= 0) {
            set(server, channel, false);
            return;
        }
        levels.put(channel, Mth.clamp(value, 1, 15));
        setDirty();
        set(server, channel, true);
    }

    public boolean toggle(MinecraftServer server, int channel) {
        boolean now = !isOn(channel);
        set(server, channel, now);
        return now;
    }

    public void set(MinecraftServer server, int channel, boolean value) {
        if (channel == 0) return;
        boolean changed = value ? on.add(channel) : on.remove(channel);
        if (changed) setDirty();
        Set<GlobalPos> set = members.get(channel);
        if (set == null) return;
        for (GlobalPos member : List.copyOf(set)) {
            ServerLevel level = server.getLevel(member.dimension());
            if (level == null || !level.isLoaded(member.pos())) continue;
            if (level.getBlockEntity(member.pos()) instanceof ChannelMember m && m.getChannel() == channel) {
                m.channelChanged(value);
            }
        }
    }

    public List<GlobalPos> members(int channel) {
        Set<GlobalPos> set = members.get(channel);
        return set == null ? List.of() : List.copyOf(set);
    }

    public static void track(Level level, BlockPos pos, int channel) {
        LightChannels channels = get(level);
        if (channels == null || channel == 0) return;
        channels.members.computeIfAbsent(channel, c -> new HashSet<>()).add(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    public static void untrack(Level level, BlockPos pos, int channel) {
        LightChannels channels = get(level);
        if (channels == null || channel == 0) return;
        Set<GlobalPos> set = channels.members.get(channel);
        if (set == null) return;
        set.remove(GlobalPos.of(level.dimension(), pos));
        if (set.isEmpty()) channels.members.remove(channel);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putIntArray("on", on.toIntArray());
        int[] pairs = new int[levels.size() * 2];
        int i = 0;
        for (Int2IntMap.Entry entry : levels.int2IntEntrySet()) {
            pairs[i++] = entry.getIntKey();
            pairs[i++] = entry.getIntValue();
        }
        tag.putIntArray("levels", pairs);
        return tag;
    }

    private static LightChannels load(CompoundTag tag, HolderLookup.Provider registries) {
        LightChannels channels = new LightChannels();
        for (int channel : tag.getIntArray("on")) channels.on.add(channel);
        int[] pairs = tag.getIntArray("levels");
        for (int i = 0; i + 1 < pairs.length; i += 2) channels.levels.put(pairs[i], pairs[i + 1]);
        return channels;
    }
}
