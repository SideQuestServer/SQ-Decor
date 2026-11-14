package com.sqdecor.client;

import com.mojang.brigadier.CommandDispatcher;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.client.render.ChunkExtras;
import com.sqdecor.client.render.Occlusion;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class PerfProbe {
    private PerfProbe() {}

    private static final int TICKS = 100;

    public static boolean on;
    public static int drawn, idle, hidden, hot, wireBuilds, wiresOutside;
    private static int ticksLeft, frames, rebuildsAtStart, occlusionTests, occlusionSkipped, outsideView;

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("sqdecorperf").executes(ctx -> {
            start();
            return 1;
        }));
    }

    private static void start() {
        drawn = idle = hidden = hot = frames = wireBuilds = wiresOutside = 0;
        occlusionTests = occlusionSkipped = outsideView = 0;
        rebuildsAtStart = LedBlockEntity.clientRebuilds;
        ticksLeft = TICKS;
        on = true;
        say(Component.translatable("message.sqdecor.perf.start"));
    }

    public static void onFrame(RenderLevelStageEvent event) {
        if (!on || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        frames++;
        occlusionTests += Occlusion.lastTests;
        occlusionSkipped += Occlusion.lastSkipped;
        outsideView += Occlusion.lastOutside;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!on || --ticksLeft > 0) return;
        on = false;
        int f = Math.max(1, frames);
        float seconds = TICKS / 20f;
        say(Component.translatable("message.sqdecor.perf.leds", drawn / f, idle / f, hidden / f, hot / f));
        say(Component.translatable("message.sqdecor.perf.chunks",
                String.format(java.util.Locale.ROOT, "%.1f", (LedBlockEntity.clientRebuilds - rebuildsAtStart) / seconds),
                ChunkExtras.chainsInMesh(), ChunkExtras.tapesInMesh(), ChunkExtras.notInMesh()));
        say(Component.translatable("message.sqdecor.perf.culling", occlusionTests / f, occlusionSkipped / f,
                (outsideView + wiresOutside) / f, String.format(java.util.Locale.ROOT, "%.1f", wireBuilds / seconds)));
    }

    private static void say(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(message, false);
    }
}
