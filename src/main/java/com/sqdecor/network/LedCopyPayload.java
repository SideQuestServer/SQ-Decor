package com.sqdecor.network;

import com.sqdecor.SQDecor;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.item.LedTunerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LedCopyPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<LedCopyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SQDecor.MODID, "led_copy"));

    public static final StreamCodec<ByteBuf, LedCopyPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(LedCopyPayload::new, LedCopyPayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LedCopyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
            if (!(level.getBlockState(pos).getBlock() instanceof LedBlock)) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof LedTunerItem)) stack = player.getOffhandItem();
            if (stack.getItem() instanceof LedTunerItem) LedTunerItem.copyLook(stack, level, pos, player);
        });
    }
}
