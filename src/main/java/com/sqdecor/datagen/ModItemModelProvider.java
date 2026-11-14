package com.sqdecor.datagen;

import com.sqdecor.SQDecor;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.registry.ModBlocks;

import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, SQDecor.MODID, files);
    }

    @Override
    protected void registerModels() {
        withExistingParent("led_bulb", modLoc("block/led/led_bulb"))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, 8, 0).scale(1.5f).end()
                .transform(ItemDisplayContext.FIXED).translation(0, -2, 0).scale(2f).end()
                .end();

        withExistingParent("led_cage_bulb", modLoc("block/led/led_cage_bulb"));
        ModBlocks.SWITCHES.forEach(device -> withExistingParent(device.getId().getPath(),
                modLoc("block/" + device.getId().getPath())));
        ModBlocks.MARQUEES.forEach(m -> withExistingParent(m.getId().getPath(), modLoc("block/" + m.getId().getPath())));
        withExistingParent("image_monitor", modLoc("block/image_monitor"));
        ModBlocks.CABLE_RACKS.forEach(r -> withExistingParent(r.getId().getPath(), modLoc("block/" + r.getId().getPath())));

        for (DeferredBlock<? extends LampBlock> lamp : ModBlocks.LAMPS) {
            String model = "block/" + lamp.get().model();
            String item = model + "_item";
            boolean own = existingFileHelper.exists(modLoc(item), PackType.CLIENT_RESOURCES, ".json", "models");
            withExistingParent(lamp.getId().getPath(), modLoc(own ? item : model));
        }

        for (DeferredBlock<? extends Block> block : ModBlocks.INDUSTRIAL) {
            withExistingParent(block.getId().getPath(), modLoc("block/industrial/" + block.getId().getPath()));
        }
        ModBlocks.FENCES.forEach(f -> withExistingParent(f.getId().getPath(), modLoc("block/style/" + f.getId().getPath() + "_inventory")));

        basicItem(SQDecor.id("led_remote"));
        basicItem(SQDecor.id("caution_tape"));
        basicItem(SQDecor.id("thin_cable"));
        basicItem(SQDecor.id("handbook"));
    }
}
