package com.sqdecor.network;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.block.marquee.MarqueeBlock;
import com.sqdecor.registry.ModSounds;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MarqueePayload(BlockPos pos, String text, int rgb, int speed, boolean on) implements CustomPacketPayload {

    public static final Type<MarqueePayload> TYPE = new Type<>(SQDecor.id("marquee"));

    public static final StreamCodec<ByteBuf, MarqueePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MarqueePayload::pos,
            ByteBufCodecs.stringUtf8(MarqueeBlockEntity.MAX_TEXT * 4), MarqueePayload::text,
            ByteBufCodecs.INT, MarqueePayload::rgb,
            ByteBufCodecs.VAR_INT, MarqueePayload::speed,
            ByteBufCodecs.BOOL, MarqueePayload::on,
            MarqueePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarqueePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 4.0) || !player.mayBuild()) return;
            if (!(level.getBlockState(pos).getBlock() instanceof MarqueeBlock)) return;
            for (BlockPos p : MarqueeBlock.row(level, pos)) {
                if (level.getBlockEntity(p) instanceof MarqueeBlockEntity be) {
                    be.set(payload.text(), payload.rgb(), payload.speed(), payload.on());
                }
            }
            ModSounds.play(level, pos, ModSounds.MARQUEE, 0.4f);
        });
    }
}
