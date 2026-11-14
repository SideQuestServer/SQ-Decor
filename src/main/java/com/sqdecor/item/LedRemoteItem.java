package com.sqdecor.item;

import java.util.List;

import com.sqdecor.block.led.LedBlock;
import com.sqdecor.block.switches.LightSwitchBlock;
import com.sqdecor.client.RemoteClient;
import com.sqdecor.item.RemoteGroups.Group;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public class LedRemoteItem extends Item implements ChannelItem {
    public LedRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean togglesOnUse() {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) RemoteClient.open(hand);
        } else if (!level.isClientSide) {
            RemoteGroups groups = RemoteGroups.of(stack);
            if (groups.groups().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.sqdecor.remote_none"), true);
            } else {
                int next = (groups.selected() + 1) % groups.groups().size();
                RemoteGroups.save(stack, new RemoteGroups(groups.groups(), next));
                player.displayClientMessage(Component.translatable("message.sqdecor.remote_selected",
                        groups.groups().get(next).label(next)), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof LedBlock || block instanceof LightSwitchBlock;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RemoteGroups groups = RemoteGroups.of(stack);
        if (groups.groups().isEmpty()) {
            tooltip.add(Component.translatable("item.sqdecor.led_remote.none").withStyle(ChatFormatting.GRAY));
        } else {
            for (int i = 0; i < groups.groups().size(); i++) {
                Group group = groups.groups().get(i);
                boolean selected = i == groups.selected();
                tooltip.add(Component.literal(selected ? "▶ " : "   ").append(group.label(i))
                        .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            }
        }
        for (int i = 1; i <= 4; i++) {
            tooltip.add(Component.translatable("item.sqdecor.led_remote.hint" + i).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
