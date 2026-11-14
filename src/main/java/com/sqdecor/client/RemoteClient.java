package com.sqdecor.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class RemoteClient {
    private RemoteClient() {}

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new RemoteScreen(hand));
    }

    public static void status(List<Boolean> on, List<Integer> leds) {
        if (Minecraft.getInstance().screen instanceof RemoteScreen screen) screen.status(on, leds);
    }
}
