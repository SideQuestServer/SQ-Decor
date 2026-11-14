package com.sqdecor.network;

import java.util.List;

import com.sqdecor.SQDecor;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.item.TunerScene;
import com.sqdecor.item.TunerSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LedTunerPayload(TunerSettings settings, List<TunerScene> scenes) implements CustomPacketPayload {

    public static final Type<LedTunerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SQDecor.MODID, "led_tuner"));

    public static final StreamCodec<FriendlyByteBuf, LedTunerPayload> STREAM_CODEC = StreamCodec.composite(
            TunerSettings.STREAM_CODEC, LedTunerPayload::settings,
            TunerScene.LIST_STREAM_CODEC, LedTunerPayload::scenes,
            LedTunerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LedTunerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof LedTunerItem)) stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof LedTunerItem)) return;
            payload.settings().store(stack);
            TunerScene.store(stack, payload.scenes());
        });
    }
}
