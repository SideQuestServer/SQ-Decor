package com.sqdecor.item;

import java.util.List;

import com.sqdecor.block.led.LedBlock;
import com.sqdecor.block.switches.LightSwitchBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public class LightSwitchItem extends BlockItem implements ChannelItem {
    public LightSwitchItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean togglesOnUse() {
        return false;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof LedBlock || block instanceof LightSwitchBlock;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChannelItem.addTooltip(stack, tooltip);
        String prefix = "item.sqdecor." + BuiltInRegistries.ITEM.getKey(this).getPath() + ".hint";
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(prefix + i).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
