package com.sqdecor.block.lamp;

import java.util.List;

import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.lamp.LampPart;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AnimatedStripBlock extends LampBlock {
    private final StripLayout layout;

    public AnimatedStripBlock(Properties properties, String model, double[] base, StripLayout layout) {
        super(properties, model, base);
        this.layout = layout;
    }

    public StripLayout layout() {
        return layout;
    }

    @Override
    public List<LampPart> clickOrder() {
        return List.of(LampPart.BULB, LampPart.CASE, LampPart.CASE2);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean mode = stack.is(Items.REDSTONE);
        if (mode || stack.is(Items.GLOWSTONE_DUST)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LampBlockEntity be) {
                if (mode) {
                    be.setAnim(StripAnim.byIndex(be.anim()).next().ordinal());
                    player.displayClientMessage(Component.translatable("message.sqdecor.strip_anim",
                            Component.translatable("strip_anim.sqdecor." + StripAnim.byIndex(be.anim()).key())), true);
                } else {
                    be.setAnimSpeed((be.animSpeed() + 1) % 4);
                    player.displayClientMessage(Component.translatable("message.sqdecor.strip_speed",
                            Component.translatable("strip_speed.sqdecor." + be.animSpeed())), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
