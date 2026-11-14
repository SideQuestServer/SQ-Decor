package com.sqdecor.block.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class Industrial {
    private Industrial() {}

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public static java.util.List<ItemStack> withPaint(java.util.List<ItemStack> drops,
                                                      net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var be = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof com.sqdecor.block.entity.PaintBlockEntity paint && (!paint.paint().isNone() || paint.template() != null)) {
            for (ItemStack stack : drops) {
                if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof PaintedBlock) {
                    if (!paint.paint().isNone()) stack.set(com.sqdecor.registry.ModDataComponents.BLOCK_PAINT.get(), paint.paint());
                    if (paint.template() != null) stack.set(com.sqdecor.registry.ModDataComponents.BLOCK_TEMPLATE.get(), paint.template().getBlock());
                }
            }
        }
        return drops;
    }

    public static ItemStack pick(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (state.hasProperty(COLOR)) {
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                    net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(COLOR, state.getValue(COLOR)));
        }
        if (level.getBlockEntity(pos) instanceof com.sqdecor.block.entity.PaintBlockEntity be && !be.paint().isNone()) {
            stack.set(com.sqdecor.registry.ModDataComponents.BLOCK_PAINT.get(), be.paint());
        }
        if (level.getBlockEntity(pos) instanceof com.sqdecor.block.entity.PaintBlockEntity be && be.template() != null) {
            stack.set(com.sqdecor.registry.ModDataComponents.BLOCK_TEMPLATE.get(), be.template().getBlock());
        }
        return stack;
    }

    public static ItemInteractionResult dye(BlockState state, Level level, BlockPos pos, ItemStack stack) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && state.getValue(COLOR) != dye.getDyeColor()) {
            level.setBlock(pos, state.setValue(COLOR, dye.getDyeColor()), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof com.sqdecor.block.entity.PaintBlockEntity be) {
                if (be.base() >= 0) be.set(-1, be.accent());
                if (be.template() != null) be.setTemplate(null);
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
