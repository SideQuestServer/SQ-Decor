package com.sqdecor.datagen;

import java.util.concurrent.CompletableFuture;

import com.sqdecor.SQDecor;
import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockTagsProvider extends BlockTagsProvider {
    private static final TagKey<Block> CREATE_SAFE_NBT = external("create", "safe_nbt");
    private static final TagKey<Block> CREATE_WRENCH_PICKUP = external("create", "wrench_pickup");
    private static final TagKey<Block> SABLE_SUPER_LIGHT = external("sable", "super_light");
    private static final TagKey<Block> SABLE_QUARTER_VOLUME = external("sable", "quarter_volume");

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                ExistingFileHelper files) {
        super(output, lookup, SQDecor.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.LEDS).add(ModBlocks.LED_BULB.get(), ModBlocks.LED_CAGE_BULB.get());
        ModBlocks.LAMPS.forEach(lamp -> tag(ModTags.LEDS).add(lamp.get()));
        tag(ModTags.DECOR).add(ModBlocks.IMAGE_MONITOR.get(), ModBlocks.CABLE_CLIP.get());
        ModBlocks.MARQUEES.forEach(m -> tag(ModTags.DECOR).add(m.get()));
        ModBlocks.CABLE_RACKS.forEach(r -> tag(ModTags.DECOR).add(r.get()));
        ModBlocks.SWITCHES.forEach(device -> tag(ModTags.DECOR).add(device.get()));
        com.sqdecor.registry.DecorBlocks.ALL.forEach(block -> {
            tag(ModTags.DECOR).add(block.get());
            if (!com.sqdecor.registry.DecorBlocks.BY_HAND.contains(block))
                tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(block.get());
        });
        ModBlocks.INDUSTRIAL.forEach(block -> tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(block.get()));
        ModBlocks.INDUSTRIAL.forEach(block -> tag(ModTags.DECOR).add(block.get()));
        ModBlocks.FENCES.forEach(block -> {
                    tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(block.get());
                    tag(ModTags.DECOR).add(block.get());
                });
        ModBlocks.FENCES.forEach(fence -> tag(net.minecraft.tags.BlockTags.FENCES).add(fence.get()));

        tag(CREATE_SAFE_NBT).addTag(ModTags.LEDS).addTag(ModTags.DECOR);
        tag(CREATE_WRENCH_PICKUP).addTag(ModTags.LEDS).addTag(ModTags.DECOR);

        tag(SABLE_SUPER_LIGHT).addTag(ModTags.LEDS).addTag(ModTags.DECOR);
        tag(SABLE_QUARTER_VOLUME).addTag(ModTags.LEDS).addTag(ModTags.DECOR);
    }

    private static TagKey<Block> external(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
