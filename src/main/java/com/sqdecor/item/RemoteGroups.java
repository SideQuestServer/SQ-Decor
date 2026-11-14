package com.sqdecor.item;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sqdecor.registry.ModDataComponents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record RemoteGroups(List<Group> groups, int selected) {
    public static final int MAX = 8;
    public static final int NAME_MAX = 24;
    public static final int PROGRAM_MAX = 4;
    public static final RemoteGroups EMPTY = new RemoteGroups(List.of(), 0);

    public record Group(String name, int channel, int program) {
        public Group {
            program = Mth.clamp(program, 0, PROGRAM_MAX);
        }

        public static final Codec<Group> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(Group::name),
                Codec.INT.fieldOf("channel").forGetter(Group::channel),
                Codec.INT.optionalFieldOf("program", 0).forGetter(Group::program)
        ).apply(i, Group::new));

        public static final StreamCodec<ByteBuf, Group> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Group::name,
                ByteBufCodecs.VAR_INT, Group::channel,
                ByteBufCodecs.VAR_INT, Group::program,
                Group::new);

        public Component label(int index) {
            return name.isBlank() ? Component.translatable("gui.sqdecor.remote.group", index + 1) : Component.literal(name);
        }
    }

    public static final Codec<RemoteGroups> CODEC = RecordCodecBuilder.create(i -> i.group(
            Group.CODEC.listOf().fieldOf("groups").forGetter(RemoteGroups::groups),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(RemoteGroups::selected)
    ).apply(i, RemoteGroups::new));

    public static final StreamCodec<ByteBuf, RemoteGroups> STREAM_CODEC = StreamCodec.composite(
            Group.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), RemoteGroups::groups,
            ByteBufCodecs.VAR_INT, RemoteGroups::selected,
            RemoteGroups::new);

    public static RemoteGroups of(ItemStack stack) {
        RemoteGroups groups = stack.get(ModDataComponents.REMOTE_GROUPS.get());
        if (groups != null) return groups;
        int channel = stack.getOrDefault(ModDataComponents.CHANNEL.get(), 0);
        return channel == 0 ? EMPTY : new RemoteGroups(List.of(new Group("", channel, 0)), 0);
    }

    public static void save(ItemStack stack, RemoteGroups groups) {
        int selected = groups.groups.isEmpty() ? 0 : Mth.clamp(groups.selected, 0, groups.groups.size() - 1);
        RemoteGroups clean = new RemoteGroups(List.copyOf(groups.groups), selected);
        stack.set(ModDataComponents.REMOTE_GROUPS.get(), clean);
        if (clean.groups.isEmpty()) {
            stack.remove(ModDataComponents.CHANNEL.get());
        } else {
            stack.set(ModDataComponents.CHANNEL.get(), clean.groups.get(selected).channel());
        }
    }

    public static void select(ItemStack stack, int channel) {
        RemoteGroups groups = of(stack);
        List<Group> list = new ArrayList<>(groups.groups);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).channel() == channel) {
                save(stack, new RemoteGroups(list, i));
                return;
            }
        }
        if (list.size() < MAX) {
            list.add(new Group("", channel, 0));
            save(stack, new RemoteGroups(list, list.size() - 1));
        } else {
            list.set(groups.selected, new Group("", channel, 0));
            save(stack, new RemoteGroups(list, groups.selected));
        }
    }

    public Group selectedGroup() {
        return groups.isEmpty() ? null : groups.get(Mth.clamp(selected, 0, groups.size() - 1));
    }
}
