package com.sqdecor.registry;

import com.sqdecor.SQDecor;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SQDecor.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("sqdecor_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sqdecor"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.LED_BULB.get().getDefaultInstance())
                    .displayItems((params, output) -> TabGroups.fill(output, ModItems.ITEMS.getEntries().stream()
                            .map(item -> (net.minecraft.world.item.Item) item.get())
                            .filter(item -> !TabGroups.isLayoutItem(item))
                            .toList()))
                    .build());
}
