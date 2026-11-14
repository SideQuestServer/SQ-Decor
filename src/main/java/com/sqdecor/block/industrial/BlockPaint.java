package com.sqdecor.block.industrial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BlockPaint(int base, int accent) {
    public static final BlockPaint NONE = new BlockPaint(-1, -1);

    public static final Codec<BlockPaint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("base", -1).forGetter(BlockPaint::base),
            Codec.INT.optionalFieldOf("accent", -1).forGetter(BlockPaint::accent)
    ).apply(i, BlockPaint::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPaint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BlockPaint::base,
            ByteBufCodecs.INT, BlockPaint::accent,
            BlockPaint::new);

    public boolean isNone() {
        return base < 0 && accent < 0;
    }
}
