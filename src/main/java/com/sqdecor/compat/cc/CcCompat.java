package com.sqdecor.compat.cc;

import com.sqdecor.block.led.LedBlock;
import com.sqdecor.registry.ModBlockEntities;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class CcCompat {
    private CcCompat() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CcCompat::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.LED.get(), (be, side) -> new LedPeripheral(be));
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.LAMP.get(),
                (be, side) -> be.getBlockState().getBlock() instanceof LedBlock led && led.switchable() ? new LedPeripheral(be) : null);
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.MARQUEE.get(), (be, side) -> new MarqueePeripheral(be));
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.IMAGE_MONITOR.get(), (be, side) -> new ImageMonitorPeripheral(be));
    }
}
