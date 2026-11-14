package com.sqdecor.network;

import java.util.ArrayList;
import java.util.List;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.client.RemoteClient;
import com.sqdecor.item.RemoteGroups;
import com.sqdecor.led.LightChannels;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoteStatusPayload(List<Boolean> on, List<Integer> leds) implements CustomPacketPayload {

    public static final Type<RemoteStatusPayload> TYPE = new Type<>(SQDecor.id("remote_status"));

    public static final StreamCodec<ByteBuf, RemoteStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.apply(ByteBufCodecs.list(RemoteGroups.MAX)), RemoteStatusPayload::on,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(RemoteGroups.MAX)), RemoteStatusPayload::leds,
            RemoteStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, RemoteGroups groups) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        LightChannels channels = LightChannels.get(server);
        List<Boolean> on = new ArrayList<>();
        List<Integer> leds = new ArrayList<>();
        for (RemoteGroups.Group group : groups.groups()) {
            on.add(channels.isOn(group.channel()));
            int count = 0;
            for (GlobalPos member : channels.members(group.channel())) {
                ServerLevel level = server.getLevel(member.dimension());
                if (level != null && level.isLoaded(member.pos())
                        && level.getBlockEntity(member.pos()) instanceof LedBlockEntity be && be.getChannel() == group.channel()) {
                    count++;
                }
            }
            leds.add(count);
        }
        PacketDistributor.sendToPlayer(player, new RemoteStatusPayload(on, leds));
    }

    public static void handle(RemoteStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RemoteClient.status(payload.on(), payload.leds()));
    }
}
