package com.sqdecor.registry;

import com.sqdecor.SQDecor;
import com.sqdecor.item.HandbookItem;
import com.sqdecor.item.CautionTapeItem;
import com.sqdecor.item.LedRemoteItem;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.item.LightSwitchItem;
import com.sqdecor.led.LedLook;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SQDecor.MODID);

    public static final DeferredItem<BlockItem> LED_BULB = ITEMS.registerSimpleBlockItem(ModBlocks.LED_BULB);
    public static final DeferredItem<BlockItem> LED_CAGE_BULB = ITEMS.registerSimpleBlockItem(ModBlocks.LED_CAGE_BULB);

    static {
        ModBlocks.INDUSTRIAL.forEach(ITEMS::registerSimpleBlockItem);
    }

    static {
        ModBlocks.FENCES.forEach(ITEMS::registerSimpleBlockItem);
    }

    static {
        ModBlocks.LAMPS.forEach(lamp -> {
            LedLook look = ModBlocks.LAMP_LOOKS.get(lamp);
            if (look == null) {
                ITEMS.registerSimpleBlockItem(lamp);
            } else {
                ITEMS.register(lamp.getId().getPath(), () -> new BlockItem(lamp.get(),
                        new Item.Properties().component(ModDataComponents.LED_LOOK.get(), look)));
            }
        });
    }

    public static final DeferredItem<BlockItem> LED_MARQUEE = ITEMS.registerSimpleBlockItem(ModBlocks.LED_MARQUEE);
    public static final DeferredItem<BlockItem> MARQUEE_FRAMED = ITEMS.registerSimpleBlockItem(ModBlocks.MARQUEE_FRAMED);
    public static final DeferredItem<BlockItem> MARQUEE_HAZARD = ITEMS.registerSimpleBlockItem(ModBlocks.MARQUEE_HAZARD);
    public static final DeferredItem<BlockItem> MARQUEE_SLIM = ITEMS.registerSimpleBlockItem(ModBlocks.MARQUEE_SLIM);
    public static final DeferredItem<BlockItem> MARQUEE_BEZEL = ITEMS.registerSimpleBlockItem(ModBlocks.MARQUEE_BEZEL);
    public static final DeferredItem<BlockItem> MARQUEE_EXIT = ITEMS.registerSimpleBlockItem(ModBlocks.MARQUEE_EXIT);
    public static final DeferredItem<BlockItem> IMAGE_MONITOR = ITEMS.registerSimpleBlockItem(ModBlocks.IMAGE_MONITOR);

    static {
        ModBlocks.CABLE_RACKS.forEach(ITEMS::registerSimpleBlockItem);
    }

    static {
        ModBlocks.SWITCHES.forEach(block -> ITEMS.register(block.getId().getPath(),
                () -> new LightSwitchItem(block.get(), new Item.Properties())));
    }

    public static final DeferredItem<LedTunerItem> LED_TUNER = ITEMS.registerItem("led_tuner",
            LedTunerItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<CautionTapeItem> CAUTION_TAPE = ITEMS.registerItem("caution_tape",
            CautionTapeItem::new, new Item.Properties());
    public static final DeferredItem<CautionTapeItem> THIN_CABLE = ITEMS.registerItem("thin_cable",
            p -> new CautionTapeItem(p, ModBlocks.CABLE_CLIP, "cable", com.sqdecor.CommonConfig.CABLE_MAX_LENGTH::get), new Item.Properties());
    public static final DeferredItem<LedRemoteItem> LED_REMOTE = ITEMS.registerItem("led_remote",
            LedRemoteItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<HandbookItem> HANDBOOK = ITEMS.registerItem("handbook",
            HandbookItem::new, new Item.Properties().stacksTo(1));

    static {
        TabGroups.init();
    }
}
