package com.sqdecor.client;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.led.LedLogic;
import com.sqdecor.network.LedCopyPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HoldUseClient {
    private HoldUseClient() {}

    private static final int HOLD_TICKS = 5;

    private enum Kind { BRIGHTNESS, WHEEL, COPY }

    @Nullable
    private static Pending pending;

    private static final class Pending {
        final BlockHitResult hit;
        final InteractionHand hand;
        final Kind kind;
        int ticks;
        boolean done;

        Pending(BlockHitResult hit, InteractionHand hand, Kind kind) {
            this.hit = hit;
            this.hand = hand;
            this.kind = kind;
        }
    }

    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        if (pending != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator() || player.isSecondaryUseActive()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        Kind kind = kindFor(mc.level.getBlockState(hit.getBlockPos()), player.getItemInHand(event.getHand()), event.getHand());
        if (kind == null) return;

        pending = new Pending(hit, event.getHand(), kind);
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @Nullable
    private static Kind kindFor(BlockState state, ItemStack stack, InteractionHand hand) {
        if (!(state.getBlock() instanceof LedBlock led)) return null;
        if (stack.isEmpty()) {
            boolean light = !(state.getBlock() instanceof LampBlock lamp) || lamp.hasLight();
            return hand == InteractionHand.MAIN_HAND && light ? Kind.BRIGHTNESS : null;
        }
        if (stack.getItem() instanceof LedTunerItem) return led.switchable() ? Kind.COPY : null;
        if (stack.getItem() instanceof DyeItem && state.getBlock() instanceof LampBlock lamp && lamp.clickOrder().size() > 1) {
            return Kind.WHEEL;
        }
        return null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Pending p = pending;
        if (p == null) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean released = !mc.options.keyUse.isDown();
        if (player == null || mc.level == null || mc.gameMode == null || mc.screen != null
                || kindFor(mc.level.getBlockState(p.hit.getBlockPos()), player.getItemInHand(p.hand), p.hand) != p.kind) {
            pending = null;
            return;
        }
        if (p.done) {
            if (released) pending = null;
            return;
        }
        if (released) {
            pending = null;
            InteractionResult result = mc.gameMode.useItemOn(player, p.hand, p.hit);
            if (result.consumesAction() && result.shouldSwing()) player.swing(p.hand);
            return;
        }
        if (++p.ticks < HOLD_TICKS) return;

        switch (p.kind) {
            case BRIGHTNESS -> {
                pending = null;
                int level = mc.level.getBlockState(p.hit.getBlockPos()).getValue(LedLogic.LIGHT_LEVEL);
                mc.setScreen(new LedBrightnessScreen(p.hit.getBlockPos(), level));
            }
            case WHEEL -> {
                pending = null;
                LampBlock lamp = (LampBlock) mc.level.getBlockState(p.hit.getBlockPos()).getBlock();
                mc.setScreen(new LampWheelScreen(p.hit.getBlockPos(), p.hand, lamp.clickOrder()));
            }
            case COPY -> {
                p.done = true;
                PacketDistributor.sendToServer(new LedCopyPayload(p.hit.getBlockPos()));
                player.swing(p.hand);
            }
        }
    }
}
