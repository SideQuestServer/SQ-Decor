package com.sqdecor.network;

import java.util.List;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.ImageMonitorBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ImageMonitorPayload(BlockPos pos, int width, int height, int y, byte[] data) implements CustomPacketPayload {

    public static final Type<ImageMonitorPayload> TYPE = new Type<>(SQDecor.id("image_monitor"));

    private static final int PART_BYTES = 256 * 1024;

    public static final StreamCodec<ByteBuf, ImageMonitorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ImageMonitorPayload::pos,
            ByteBufCodecs.VAR_INT, ImageMonitorPayload::width,
            ByteBufCodecs.VAR_INT, ImageMonitorPayload::height,
            ByteBufCodecs.VAR_INT, ImageMonitorPayload::y,
            ByteBufCodecs.byteArray(PART_BYTES + 1024), ImageMonitorPayload::data,
            ImageMonitorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(BlockPos pos, int width, int height, byte[] pixels, int y0, int y1, List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        int row = width * 3;
        int rowsPerPart = Math.max(1, PART_BYTES / row);
        for (int y = y0; y < y1; y += rowsPerPart) {
            int rows = Math.min(rowsPerPart, y1 - y);
            ImageMonitorPayload payload = new ImageMonitorPayload(pos, width, height, y,
                    ImageMonitorBlockEntity.deflate(pixels, y * row, rows * row));
            for (ServerPlayer player : players) PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerPlayer player = event.getPlayer();
        for (BlockEntity be : event.getChunk().getBlockEntities().values()) {
            if (be instanceof ImageMonitorBlockEntity monitor && monitor.rawPixels() != null && monitor.inView(player)) {
                byte[] sent = monitor.sentPixels();
                byte[] image = sent != null && sent.length == monitor.rawPixels().length ? sent : monitor.rawPixels();
                send(monitor.getBlockPos(), monitor.resWidth(), monitor.resHeight(), image,
                        0, monitor.resHeight(), List.of(player));
                monitor.addViewer(player);
            }
        }
    }

    public static void handle(ImageMonitorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof ImageMonitorBlockEntity be) {
                be.receive(payload.width(), payload.height(), payload.y(), payload.data());
            }
        });
    }
}
