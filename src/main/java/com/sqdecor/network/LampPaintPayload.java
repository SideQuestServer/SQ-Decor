package com.sqdecor.network;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.lamp.LampPart;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LampPaintPayload(BlockPos pos, int part, boolean offhand) implements CustomPacketPayload {

    public static final Type<LampPaintPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SQDecor.MODID, "lamp_paint"));

    public static final StreamCodec<ByteBuf, LampPaintPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LampPaintPayload::pos,
            ByteBufCodecs.VAR_INT, LampPaintPayload::part,
            ByteBufCodecs.BOOL, LampPaintPayload::offhand,
            LampPaintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LampPaintPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
            if (!(level.getBlockState(pos).getBlock() instanceof LampBlock lamp)) return;
            if (!(level.getBlockEntity(pos) instanceof LampBlockEntity be)) return;
            if (payload.part() < 0 || payload.part() >= LampPart.values().length) return;
            LampPart part = LampPart.values()[payload.part()];
            if (!lamp.clickOrder().contains(part)) return;

            ItemStack stack = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof DyeItem dye)) return;
            DyeColor color = dye.getDyeColor();
            be.paint(part, color.getTextureDiffuseColor());
            LampBlock.show(player, part, Component.translatable("color.minecraft." + color.getName()));
        });
    }
}
