package com.sqdecor.datagen;

import java.util.Set;

import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModDataComponents;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import com.sqdecor.block.industrial.Industrial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropWithLook(ModBlocks.LED_BULB.get());
        dropWithLook(ModBlocks.LED_CAGE_BULB.get());
        ModBlocks.LAMPS.forEach(lamp -> dropWithLook(lamp.get()));
        add(ModBlocks.SHELF_TOP.get(), noDrop());

        java.util.stream.Stream.of(ModBlocks.INDUSTRIAL, ModBlocks.FENCES).flatMap(java.util.List::stream).forEach(block -> {
            Block b = block.get();
            add(b, LootTable.lootTable().withPool(applyExplosionCondition(b, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(b).apply(CopyBlockState.copyState(b).copy(Industrial.COLOR))))));
        });

        ModBlocks.MARQUEES.forEach(m -> dropSelf(m.get()));
        dropSelf(ModBlocks.IMAGE_MONITOR.get());
        add(ModBlocks.TAPE_ANCHOR.get(), noDrop());
        add(ModBlocks.CABLE_CLIP.get(), noDrop());
        ModBlocks.CABLE_RACKS.forEach(r -> dropSelf(r.get()));
        com.sqdecor.registry.DecorBlocks.ALL.forEach(holder -> {
            Block b = holder.get();
            if (com.sqdecor.registry.DecorBlocks.NO_DROP.contains(holder)) { add(b, noDrop()); return; }
            var keep = com.sqdecor.registry.DecorBlocks.KEEP.get(holder);
            if (keep == null) { dropSelf(b); return; }
            CopyBlockState.Builder copy = CopyBlockState.copyState(b);
            keep.forEach(copy::copy);
            add(b, LootTable.lootTable().withPool(applyExplosionCondition(b, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(b).apply(copy)))));
        });
        ModBlocks.SWITCHES.forEach(device -> dropWithComponent(device.get(), ModDataComponents.CHANNEL.get()));
    }

    private void dropWithLook(Block block) {
        dropWithComponent(block, ModDataComponents.LED_LOOK.get());
    }

    private void dropWithComponent(Block block, DataComponentType<?> component) {
        add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(component))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).toList();
    }
}
