package com.sqdecor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sqdecor.compat.cc.CcCompat;
import com.sqdecor.compat.create.CreateCompat;
import com.sqdecor.datagen.DataGenerators;
import com.sqdecor.item.HandbookItem;
import com.sqdecor.led.LedAnimator;
import com.sqdecor.led.LightLinks;
import com.sqdecor.network.ModNetworking;
import com.sqdecor.registry.ModBlockEntities;
import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModCreativeTabs;
import com.sqdecor.registry.ModDataComponents;
import com.sqdecor.registry.ModItems;
import com.sqdecor.registry.ModSounds;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SQDecor.MODID)
public class SQDecor {
    public static final String MODID = "sqdecor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SQDecor(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        com.sqdecor.registry.DecorBlocks.init();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        modBus.addListener(ModNetworking::register);
        modBus.addListener(DataGenerators::gather);

        NeoForge.EVENT_BUS.addListener(LedAnimator::onServerTick);
        NeoForge.EVENT_BUS.addListener(LedAnimator::onServerStopping);
        NeoForge.EVENT_BUS.addListener(HandbookItem::onLogin);
        NeoForge.EVENT_BUS.addListener(LightLinks::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(LightLinks::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.sqdecor.network.ImageMonitorPayload::onChunkSent);
        NeoForge.EVENT_BUS.addListener(com.sqdecor.block.industrial.PaintEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(com.sqdecor.block.industrial.PaintEvents::onPlace);

        if (ModList.get().isLoaded("create")) CreateCompat.register(modBus);
        if (ModList.get().isLoaded("computercraft")) CcCompat.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
