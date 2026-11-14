package com.sqdecor.item;

import java.util.List;

import com.sqdecor.CommonConfig;
import com.sqdecor.block.cable.CableClipBlock;
import com.sqdecor.block.cable.CableRackBlock;
import com.sqdecor.block.entity.CableRackBlockEntity;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.block.tape.TapeAnchorBlock;
import com.sqdecor.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CautionTapeItem extends Item {
    private static final String KEY = "tape_first";
    private static final String SLOT_KEY = "tape_first_slot";

    private final java.util.function.Supplier<? extends TapeAnchorBlock> anchor;
    private final String prefix;
    private final java.util.function.IntSupplier maxLength;

    public CautionTapeItem(Properties properties) {
        this(properties, ModBlocks.TAPE_ANCHOR, "tape", CommonConfig.TAPE_MAX_LENGTH::get);
    }

    public CautionTapeItem(Properties properties, java.util.function.Supplier<? extends TapeAnchorBlock> anchor,
                           String prefix, java.util.function.IntSupplier maxLength) {
        super(properties);
        this.anchor = anchor;
        this.prefix = prefix;
        this.maxLength = maxLength;
    }

    private String msg(String what) {
        return "message.sqdecor." + prefix + "_" + what;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();

        BlockPos anchorPos;
        int slot = -1;
        BlockState clickedState = level.getBlockState(clicked);
        if (racks() && clickedState.getBlock() instanceof CableRackBlock rack) {
            anchorPos = clicked;
            slot = rack.slotAt(clicked, clickedState, context.getClickLocation());
        } else if (clickedState.getBlock() == anchor.get()) {
            anchorPos = clicked;
        } else {
            anchorPos = clicked.relative(face);
            BlockState existing = level.getBlockState(anchorPos);
            if (!existing.canBeReplaced() || !player.mayUseItemAt(anchorPos, face, stack)) return InteractionResult.FAIL;
            BlockState state = anchor.get().defaultState(face);
            if (!state.canSurvive(level, anchorPos)) {
                player.displayClientMessage(Component.translatable(msg("no_wall")), true);
                return InteractionResult.FAIL;
            }
            level.setBlock(anchorPos, state, 3);
            if (level.getBlockEntity(anchorPos) instanceof TapeAnchorBlockEntity be) be.setColor(anchor.get().defaultColor());
            level.playSound(null, anchorPos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 1f);
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(KEY)) {
            tag.putLong(KEY, anchorPos.asLong());
            if (slot >= 0) tag.putInt(SLOT_KEY, slot);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(slot >= 0 ? Component.translatable("message.sqdecor.cable_rack_first", slot + 1)
                    : Component.translatable(msg("first")), true);
            return InteractionResult.CONSUME;
        }

        BlockPos first = BlockPos.of(tag.getLong(KEY));
        int firstSlot = tag.contains(SLOT_KEY) ? tag.getInt(SLOT_KEY) : -1;
        tag.remove(KEY);
        tag.remove(SLOT_KEY);
        setData(stack, tag);
        boolean linked = firstSlot < 0 && slot < 0 ? link(level, player, first, anchorPos)
                : linkRack(level, player, first, firstSlot, anchorPos, slot);
        if (linked && !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    private static void setData(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private boolean link(Level level, Player player, BlockPos a, BlockPos b) {
        if (a.equals(b)) return false;
        if (level.getBlockState(a).getBlock() != anchor.get()
                || !(level.getBlockEntity(a) instanceof TapeAnchorBlockEntity ea) || !(level.getBlockEntity(b) instanceof TapeAnchorBlockEntity eb)) {
            player.displayClientMessage(Component.translatable(msg("lost")), true);
            return false;
        }
        double dist = Math.sqrt(a.distSqr(b));
        int max = maxLength.getAsInt();
        if (dist > max) {
            player.displayClientMessage(Component.translatable(msg("too_far"), max), true);
            return false;
        }
        if (ea.links().contains(b) && eb.links().contains(a)) return false;
        ea.addLink(b);
        eb.addLink(a);
        eb.setColor(ea.getColor());
        level.playSound(null, b, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1f, 1f);
        player.displayClientMessage(Component.translatable(msg("linked")), true);
        return true;
    }

    private boolean racks() {
        return anchor.get() instanceof CableClipBlock;
    }

    private boolean validEnd(Level level, BlockPos pos, int slot) {
        if (slot >= 0) return level.getBlockEntity(pos) instanceof CableRackBlockEntity rack && slot < rack.slots();
        return level.getBlockState(pos).getBlock() == anchor.get() && level.getBlockEntity(pos) instanceof TapeAnchorBlockEntity;
    }

    private boolean linkRack(Level level, Player player, BlockPos a, int sa, BlockPos b, int sb) {
        if (a.equals(b)) return false;
        if (!validEnd(level, a, sa) || !validEnd(level, b, sb)) {
            player.displayClientMessage(Component.translatable(msg("lost")), true);
            return false;
        }
        int max = maxLength.getAsInt();
        if (Math.sqrt(a.distSqr(b)) > max) {
            player.displayClientMessage(Component.translatable(msg("too_far"), max), true);
            return false;
        }
        if (sa >= 0 && sb >= 0) {
            CableRackBlockEntity ra = (CableRackBlockEntity) level.getBlockEntity(a);
            CableRackBlockEntity rb = (CableRackBlockEntity) level.getBlockEntity(b);
            if (ra.hasLink(sa, b, sb)) return false;
            ra.addLink(sa, b, sb);
            rb.addLink(sb, a, sa);
            rb.setColor(sb, ra.color(sa));
        } else {
            BlockPos rackPos = sa >= 0 ? a : b, clipPos = sa >= 0 ? b : a;
            int rackSlot = Math.max(sa, sb);
            CableRackBlockEntity rack = (CableRackBlockEntity) level.getBlockEntity(rackPos);
            TapeAnchorBlockEntity clip = (TapeAnchorBlockEntity) level.getBlockEntity(clipPos);
            if (rack.hasLink(rackSlot, clipPos, -1)) return false;
            rack.addLink(rackSlot, clipPos, -1);
            clip.addLink(rackPos);
            if (sa < 0) rack.setColor(rackSlot, clip.getColor());
        }
        level.playSound(null, b, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1f, 1f);
        player.displayClientMessage(Component.translatable(msg("linked")), true);
        return true;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains(KEY)) {
                tag.remove(KEY);
                tag.remove(SLOT_KEY);
                setData(stack, tag);
                if (!level.isClientSide) player.displayClientMessage(Component.translatable(msg("reset")), true);
                return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".hint1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".hint2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
