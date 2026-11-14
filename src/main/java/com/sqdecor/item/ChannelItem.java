package com.sqdecor.item;

import java.util.List;

import com.sqdecor.led.LightLinks;
import com.sqdecor.led.LightChannels;
import com.sqdecor.registry.ModDataComponents;
import com.sqdecor.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ChannelItem {
    boolean togglesOnUse();

    static int channel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHANNEL.get(), 0);
    }

    static void setChannel(ItemStack stack, int channel) {
        if (stack.getItem() instanceof LedRemoteItem && channel != 0) {
            RemoteGroups.select(stack, channel);
        } else if (channel == 0) {
            stack.remove(ModDataComponents.CHANNEL.get());
        } else {
            stack.set(ModDataComponents.CHANNEL.get(), channel);
        }
    }

    static int ensureChannel(ItemStack stack, Level level) {
        int channel = channel(stack);
        if (channel == 0) {
            channel = LightChannels.newChannel(level.random);
            setChannel(stack, channel);
        }
        return channel;
    }

    static void link(ItemStack stack, Level level, BlockPos pos, Player player) {
        LightLinks.link(level, pos, ensureChannel(stack, level), player);
    }

    static void toggle(ItemStack stack, Level level, Player player) {
        int channel = channel(stack);
        MinecraftServer server = level.getServer();
        if (server == null) return;
        if (channel == 0) {
            player.displayClientMessage(Component.translatable("message.sqdecor.channel_none"), true);
            return;
        }
        boolean on = LightChannels.get(server).toggle(server, channel);
        ModSounds.playSwitch(level, player.blockPosition(), ModSounds.REMOTE, on);
        player.displayClientMessage(Component.translatable("message.sqdecor.channel_toggled", LightChannels.label(channel),
                Component.translatable(on ? "message.sqdecor.on" : "message.sqdecor.off")), true);
    }

    static void addTooltip(ItemStack stack, List<Component> tooltip) {
        int channel = channel(stack);
        tooltip.add(channel == 0
                ? Component.translatable("item.sqdecor.channel.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.sqdecor.channel", LightChannels.label(channel)).withStyle(ChatFormatting.AQUA));
    }
}
