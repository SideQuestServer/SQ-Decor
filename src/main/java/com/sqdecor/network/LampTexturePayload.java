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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LampTexturePayload(BlockPos pos, boolean offhand) implements CustomPacketPayload {

    public static final Type<LampTexturePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SQDecor.MODID, "lamp_texture"));

    public static final StreamCodec<ByteBuf, LampTexturePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LampTexturePayload::pos,
            ByteBufCodecs.BOOL, LampTexturePayload::offhand,
            LampTexturePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LampTexturePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
            if (!(level.getBlockState(pos).getBlock() instanceof LampBlock lamp)) return;
            if (!(level.getBlockEntity(pos) instanceof LampBlockEntity be)) return;

            ItemStack stack = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof BlockItem item) || !LampBlockEntity.canUseAsTexture(item.getBlock())) return;
            LampPart part = be.textureNext(lamp.clickOrder(), item.getBlock());
            if (part == null) {
                player.displayClientMessage(Component.translatable("message.sqdecor.lamp_no_case"), true);
            } else {
                LampBlock.show(player, part, item.getBlock().getName());
            }
        });
    }
}
