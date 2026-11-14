package com.sqdecor.led;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;

public record LedLook(int rgb, int caseRgb, int case2Rgb, Optional<Block> caseBlock, Optional<Block> case2Block) {

    public static final Codec<LedLook> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("rgb").forGetter(LedLook::rgb),
            Codec.INT.optionalFieldOf("case_rgb", -1).forGetter(LedLook::caseRgb),
            Codec.INT.optionalFieldOf("case2_rgb", -1).forGetter(LedLook::case2Rgb),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("case_block").forGetter(LedLook::caseBlock),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("case2_block").forGetter(LedLook::case2Block)
    ).apply(i, LedLook::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LedLook> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, LedLook::rgb,
            ByteBufCodecs.INT, LedLook::caseRgb,
            ByteBufCodecs.INT, LedLook::case2Rgb,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.BLOCK)), LedLook::caseBlock,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.BLOCK)), LedLook::case2Block,
            LedLook::new);

    public static LedLook light(int rgb) {
        return new LedLook(rgb, -1, -1, Optional.empty(), Optional.empty());
    }
}
