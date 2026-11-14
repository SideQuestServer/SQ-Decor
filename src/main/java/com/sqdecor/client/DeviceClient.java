package com.sqdecor.client;

import java.util.List;

import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.DeviceSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class DeviceClient {
    private DeviceClient() {}

    public static void open(BlockPos pos, List<DeviceSetting> settings) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be) {
            mc.setScreen(new DeviceScreen(pos, mc.level.getBlockState(pos).getBlock().getName(), settings, be));
        }
    }
}
