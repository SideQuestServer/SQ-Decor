package com.sqdecor.client;

import com.sqdecor.block.entity.MarqueeBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class MarqueeClient {
    private MarqueeClient() {}

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof MarqueeBlockEntity be) {
            mc.setScreen(new MarqueeScreen(pos, be.getText(), be.getRgb(), be.getSpeed(), be.isOn()));
        }
    }
}
