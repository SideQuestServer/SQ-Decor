package com.sqdecor.network;

import java.util.ArrayList;
import java.util.List;

import com.sqdecor.SQDecor;
import com.sqdecor.item.LedRemoteItem;
import com.sqdecor.item.RemoteGroups;
import com.sqdecor.item.RemoteGroups.Group;
import com.sqdecor.led.ChannelPrograms;
import com.sqdecor.led.LightChannels;
import com.sqdecor.registry.ModSounds;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemotePayload(boolean mainHand, int action, int index, int value, String text) implements CustomPacketPayload {
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int PROGRAM = 2;
    public static final int ADD = 3;
    public static final int DELETE = 4;
    public static final int RENAME = 5;
    public static final int SELECT = 6;
    public static final int STATUS = 7;

    public static final Type<RemotePayload> TYPE = new Type<>(SQDecor.id("remote"));

    public static final StreamCodec<ByteBuf, RemotePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RemotePayload::mainHand,
            ByteBufCodecs.VAR_INT, RemotePayload::action,
            ByteBufCodecs.VAR_INT, RemotePayload::index,
            ByteBufCodecs.VAR_INT, RemotePayload::value,
            ByteBufCodecs.stringUtf8(RemoteGroups.NAME_MAX * 4), RemotePayload::text,
            RemotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            MinecraftServer server = player.getServer();
            ItemStack stack = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (server == null || !(stack.getItem() instanceof LedRemoteItem)) return;
            apply(payload, player, server, stack);
            if (player instanceof ServerPlayer serverPlayer) RemoteStatusPayload.send(serverPlayer, RemoteGroups.of(stack));
        });
    }

    private static void apply(RemotePayload payload, Player player, MinecraftServer server, ItemStack stack) {
        if (payload.action() == STATUS) return;
        RemoteGroups groups = RemoteGroups.of(stack);
        List<Group> list = new ArrayList<>(groups.groups());
        int i = payload.index();

        if (payload.action() == ADD) {
            if (list.size() >= RemoteGroups.MAX) return;
            list.add(new Group("", LightChannels.newChannel(player.getRandom()), 0));
            RemoteGroups.save(stack, new RemoteGroups(list, list.size() - 1));
            return;
        }
        if (i < 0 || i >= list.size()) return;
        Group group = list.get(i);
        switch (payload.action()) {
            case OFF, ON -> {
                boolean on = payload.action() == ON;
                LightChannels.get(server).set(server, group.channel(), on);
                ModSounds.playSwitch(player.level(), player.blockPosition(), ModSounds.REMOTE, on);
                player.displayClientMessage(Component.translatable("message.sqdecor.remote_switched", group.label(i),
                        Component.translatable(on ? "message.sqdecor.on" : "message.sqdecor.off")), true);
            }
            case PROGRAM -> {
                int program = Mth.clamp(payload.value(), 0, RemoteGroups.PROGRAM_MAX);
                list.set(i, new Group(group.name(), group.channel(), program));
                RemoteGroups.save(stack, new RemoteGroups(list, groups.selected()));
                int count = ChannelPrograms.run(server, group.channel(), program);
                player.displayClientMessage(count == 0
                        ? Component.translatable("message.sqdecor.remote_empty", group.label(i))
                        : Component.translatable("message.sqdecor.remote_program", group.label(i),
                                Component.translatable("gui.sqdecor.tuner.program." + PROGRAM_KEYS[program])), true);
            }
            case DELETE -> {
                list.remove(i);
                RemoteGroups.save(stack, new RemoteGroups(list, Math.min(groups.selected(), list.size() - 1)));
            }
            case RENAME -> {
                String name = payload.text().strip();
                if (name.length() > RemoteGroups.NAME_MAX) name = name.substring(0, RemoteGroups.NAME_MAX);
                list.set(i, new Group(name, group.channel(), group.program()));
                RemoteGroups.save(stack, new RemoteGroups(list, groups.selected()));
            }
            case SELECT -> RemoteGroups.save(stack, new RemoteGroups(list, i));
            default -> { }
        }
    }

    public static final String[] PROGRAM_KEYS = {"off", "rainbow", "chase", "pulse", "blink"};
}
