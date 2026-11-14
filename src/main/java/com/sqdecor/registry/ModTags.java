package com.sqdecor.registry;

import com.sqdecor.SQDecor;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {}

    public static final TagKey<Block> LEDS = TagKey.create(Registries.BLOCK, SQDecor.id("leds"));
    public static final TagKey<Block> DECOR = TagKey.create(Registries.BLOCK, SQDecor.id("decor"));
}
