package com.sqdecor.network;

import com.sqdecor.SQDecor;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.led.LedLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LedBrightnessPayload(BlockPos pos, int level) implements CustomPacketPayload {

    public static final Type<LedBrightnessPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SQDecor.MODID, "led_brightness"));

    public static final StreamCodec<ByteBuf, LedBrightnessPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LedBrightnessPayload::pos,
            ByteBufCodecs.VAR_INT, LedBrightnessPayload::level,
            LedBrightnessPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LedBrightnessPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
            if (!(level.getBlockState(pos).getBlock() instanceof LedBlock led) || !led.switchable()) return;
            if (!player.getMainHandItem().isEmpty()) return;
            int n = Mth.clamp(payload.level(), 0, 15);
            LedLogic.applyLevel(level, pos, n, true);
            LedLogic.remember(player, n);
            LedLogic.showLevel(player, n);
        });
    }
}
