package com.sqdecor.client;

import com.sqdecor.client.compendium.CompendiumScreen;

import net.minecraft.client.Minecraft;

public final class HandbookClient {
    private HandbookClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new CompendiumScreen());
    }
}
