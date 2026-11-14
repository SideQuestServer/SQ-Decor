package com.sqdecor.registry;

import com.mojang.serialization.Codec;
import com.sqdecor.SQDecor;
import com.sqdecor.item.RemoteGroups;
import com.sqdecor.led.LedLook;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SQDecor.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LedLook>> LED_LOOK =
            COMPONENTS.registerComponentType("led_look",
                    builder -> builder.persistent(LedLook.CODEC).networkSynchronized(LedLook.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.sqdecor.block.industrial.BlockPaint>> BLOCK_PAINT =
            COMPONENTS.registerComponentType("block_paint",
                    builder -> builder.persistent(com.sqdecor.block.industrial.BlockPaint.CODEC)
                            .networkSynchronized(com.sqdecor.block.industrial.BlockPaint.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.level.block.Block>> BLOCK_TEMPLATE =
            COMPONENTS.registerComponentType("block_template",
                    builder -> builder.persistent(net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec())
                            .networkSynchronized(ByteBufCodecs.registry(Registries.BLOCK)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHANNEL =
            COMPONENTS.registerComponentType("channel",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RemoteGroups>> REMOTE_GROUPS =
            COMPONENTS.registerComponentType("remote_groups",
                    builder -> builder.persistent(RemoteGroups.CODEC).networkSynchronized(RemoteGroups.STREAM_CODEC));
}
