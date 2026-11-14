package com.sqdecor.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class DataGenerators {
    private DataGenerators() {}

    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeClient(), new ModBlockStateProvider(output, files));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, files));
        gen.addProvider(event.includeClient(), new RotatedModelProvider(output, files));
        gen.addProvider(event.includeClient(), new ModLanguageProvider.English(output));
        gen.addProvider(event.includeClient(), new ModLanguageProvider.German(output));
        gen.addProvider(event.includeClient(), new ModLanguageProvider.Russian(output));

        gen.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)),
                lookup));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
        gen.addProvider(event.includeServer(), new ModBlockTagsProvider(output, lookup, files));
    }
}
