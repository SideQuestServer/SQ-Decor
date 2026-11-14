package com.sqdecor.block.industrial;

import com.sqdecor.block.entity.PaintBlockEntity;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.item.TunerSettings;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PaintEvents {
    private PaintEvents() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof PaintedBlock)) return;
        ItemStack stack = event.getItemStack();
        boolean sneaking = event.getEntity().isShiftKeyDown();

        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide) remember(event.getEntity(), dye.getDyeColor(), -1, null);
            return;
        }

        if (stack.getItem() instanceof LedTunerItem) {
            if (!level.isClientSide && level.getBlockEntity(event.getPos()) instanceof PaintBlockEntity be) {
                TunerSettings s = TunerSettings.of(stack);
                Block block = LedTunerItem.block(s.caseBlock());
                if (block != null && PaintBlockEntity.canUseAsTemplate(block)) {
                    be.set(-1, -1);
                    be.setTemplate(block.defaultBlockState());
                } else {
                    int base = s.caseRgb() == TunerSettings.ORIGINAL ? -1 : s.caseRgb() >= 0 ? s.caseRgb() : be.base();
                    be.set(base, -1);
                    if (s.caseRgb() == TunerSettings.ORIGINAL || s.caseRgb() >= 0) be.setTemplate(null);
                }
                remember(event.getEntity(), level, event.getPos(), be);
                event.getEntity().displayClientMessage(Component.translatable("message.sqdecor.block_tuned"), true);
                level.playSound(null, event.getPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
            }
            finish(event, level);
            return;
        }

        if (!sneaking) return;
        if (stack.getItem() instanceof BlockItem item && PaintBlockEntity.canUseAsTemplate(item.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(event.getPos()) instanceof PaintBlockEntity be) {
                be.set(-1, -1);
                be.setTemplate(item.getBlock().defaultBlockState());
                remember(event.getEntity(), level, event.getPos(), be);
                event.getEntity().displayClientMessage(
                        Component.translatable("message.sqdecor.block_template", item.getBlock().getName()), true);
                level.playSound(null, event.getPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
            }
            finish(event, level);
            return;
        }
        if (stack.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND
                && level.getBlockEntity(event.getPos()) instanceof PaintBlockEntity be && be.template() != null) {
            if (!level.isClientSide) {
                be.setTemplate(null);
                remember(event.getEntity(), level, event.getPos(), be);
                event.getEntity().displayClientMessage(Component.translatable("message.sqdecor.block_template_cleared"), true);
                level.playSound(null, event.getPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
            }
            finish(event, level);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof Level level) || level.isClientSide) return;
        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock() instanceof PaintedBlock) || !state.hasProperty(Industrial.COLOR)) return;
        CompoundTag look = player.getPersistentData().getCompound(KEY);
        if (look.isEmpty()) return;
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof PaintBlockEntity be)) return;
        if (be.template() != null || !be.paint().isNone() || state.getValue(Industrial.COLOR) != DyeColor.WHITE) return;
        DyeColor color = DyeColor.byName(look.getString("color"), DyeColor.WHITE);
        if (color != DyeColor.WHITE) level.setBlock(pos, state.setValue(Industrial.COLOR, color), Block.UPDATE_ALL);
        int base = look.contains("base") ? look.getInt("base") : -1;
        if (base >= 0) be.set(base, -1);
        ResourceLocation id = ResourceLocation.tryParse(look.getString("template"));
        if (id != null) BuiltInRegistries.BLOCK.getOptional(id).filter(PaintBlockEntity::canUseAsTemplate)
                .ifPresent(block -> be.setTemplate(block.defaultBlockState()));
    }

    private static final String KEY = "sqdecor_paint";

    private static void remember(Player player, Level level, BlockPos pos, PaintBlockEntity be) {
        BlockState state = level.getBlockState(pos);
        DyeColor color = state.hasProperty(Industrial.COLOR) ? state.getValue(Industrial.COLOR) : DyeColor.WHITE;
        remember(player, color, be.base(), be.template());
    }

    private static void remember(Player player, DyeColor color, int base, BlockState template) {
        CompoundTag look = new CompoundTag();
        if (color != DyeColor.WHITE) look.putString("color", color.getName());
        if (base >= 0) look.putInt("base", base);
        if (template != null) look.putString("template", BuiltInRegistries.BLOCK.getKey(template.getBlock()).toString());
        if (look.isEmpty()) {
            player.getPersistentData().remove(KEY);
        } else {
            player.getPersistentData().put(KEY, look);
        }
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event, Level level) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
