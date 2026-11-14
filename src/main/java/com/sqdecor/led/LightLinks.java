package com.sqdecor.led;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.entity.LightSwitchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class LightLinks {
    private LightLinks() {}

    private static final int TIMEOUT_TICKS = 20 * 120;

    private record Pending(GlobalPos device, int channel, int until) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
    }

    public static List<BlockPos> targets(Level level, BlockPos pos) {
        if (LedLogic.isNode(level, pos)) return LedLogic.network(level, pos);
        Set<BlockPos> all = new LinkedHashSet<>();
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!all.contains(n) && LedLogic.isNode(level, n)) all.addAll(LedLogic.network(level, n));
        }
        return new ArrayList<>(all);
    }

    public static void link(Level level, BlockPos pos, int channel, Player player) {
        List<BlockPos> leds = targets(level, pos);
        if (leds.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.sqdecor.link_none"), true);
            return;
        }
        boolean unlink = level.getBlockEntity(leds.get(0)) instanceof LedBlockEntity first && first.getChannel() == channel;
        LightChannels channels = LightChannels.get(level);
        boolean on = channels != null && channels.isOn(channel);
        int count = 0;
        for (BlockPos p : leds) {
            if (level.getBlockEntity(p) instanceof LedBlockEntity be) {
                be.setChannel(unlink ? 0 : channel);
                if (!unlink) be.channelChanged(on);
                count++;
            }
        }
        String key = unlink ? "message.sqdecor.channel_unlinked" : "message.sqdecor.channel_linked";
        player.displayClientMessage(Component.translatable(key, count, LightChannels.label(channel)), true);
    }

    public static void toggle(Player player, Level level, BlockPos device, int channel) {
        MinecraftServer server = level.getServer();
        if (server == null || channel == 0) return;
        Pending old = PENDING.remove(player.getUUID());
        if (old != null && old.device().equals(GlobalPos.of(level.dimension(), device))) {
            player.displayClientMessage(Component.translatable("message.sqdecor.link_done"), true);
            return;
        }
        PENDING.put(player.getUUID(), new Pending(GlobalPos.of(level.dimension(), device.immutable()), channel,
                server.getTickCount() + TIMEOUT_TICKS));
        player.displayClientMessage(Component.translatable("message.sqdecor.link_start", LightChannels.label(channel)), true);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        MinecraftServer server = level.getServer();
        if (level.isClientSide || server == null || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return;
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) return;
        if (server.getTickCount() > pending.until() || !pending.device().dimension().equals(level.dimension())) {
            PENDING.remove(player.getUUID());
            return;
        }
        BlockPos pos = event.getPos();
        if (pos.equals(pending.device().pos())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        PENDING.put(player.getUUID(), new Pending(pending.device(), pending.channel(), server.getTickCount() + TIMEOUT_TICKS));
        if (level.getBlockEntity(pos) instanceof LightSwitchBlockEntity other) {
            other.setChannel(pending.channel());
            other.channelChanged(LightChannels.get(server).isOn(pending.channel()));
            player.displayClientMessage(Component.translatable("message.sqdecor.link_device",
                    LightChannels.label(pending.channel())), true);
            return;
        }
        link(level, pos, pending.channel(), player);
    }
}
