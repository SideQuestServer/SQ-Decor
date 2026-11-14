package com.sqdecor.network;

import java.util.List;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.block.switches.LightSwitchBlock;
import com.sqdecor.led.DeviceSetting;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeviceSettingsPayload(BlockPos pos, List<Integer> values) implements CustomPacketPayload {

    public static final Type<DeviceSettingsPayload> TYPE = new Type<>(SQDecor.id("device_settings"));

    public static final StreamCodec<ByteBuf, DeviceSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DeviceSettingsPayload::pos,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), DeviceSettingsPayload::values,
            DeviceSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeviceSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 4.0) || !player.mayBuild()) return;
            if (!(level.getBlockState(pos).getBlock() instanceof LightSwitchBlock block)) return;
            if (!(level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be)) return;
            List<DeviceSetting> settings = block.settings();
            for (int i = 0; i < settings.size() && i < payload.values().size(); i++) {
                be.set(settings.get(i), payload.values().get(i));
            }
        });
    }
}
