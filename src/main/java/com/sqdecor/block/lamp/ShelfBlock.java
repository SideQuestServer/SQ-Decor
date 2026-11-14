package com.sqdecor.block.lamp;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.item.ChannelItem;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ShelfBlock extends LampBlock {
    public static final int COLUMNS = 3;
    private static final double WIDTH = 14.8;
    private static final double BOARD = 0.8;

    private final boolean lit;
    private final int levels;
    private final double step;

    public ShelfBlock(Properties properties, String model, double[] base, boolean lit, int levels) {
        super(properties, model, base);
        this.lit = lit;
        this.levels = levels;
        this.step = ((levels <= 3 ? 16 : 32) - BOARD) / levels;
        if (!lit) registerDefaultState(defaultBlockState().setValue(LedLogic.LIGHT_LEVEL, 0));
    }

    @Override
    public boolean hasLight() {
        return lit;
    }

    public int levels() {
        return levels;
    }

    @Override
    protected BlockState placementState(BlockPlaceContext context) {
        BlockState state = super.placementState(context);
        return lit ? state : state.setValue(LedLogic.LIGHT_LEVEL, 0);
    }

    public double[] spot(int slot) {
        int level = slot / COLUMNS;
        int column = slot % COLUMNS;
        return new double[]{-WIDTH / 2 + WIDTH * (column + 0.5) / COLUMNS, level * step + BOARD, 0};
    }

    public double gap() {
        return step - BOARD;
    }

    public static Quaternionf rotation(BlockState state) {
        Direction facing = state.getValue(FACING);
        int xTurns = switch (facing) {
            case UP -> 0;
            case DOWN -> 2;
            default -> 1;
        };
        int yTurns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        double quarter = Math.PI / 2;
        return new Quaternionf().rotateY((float) (-yTurns * quarter)).rotateX((float) (-xTurns * quarter))
                .rotateY((float) (-state.getValue(TURN) * quarter));
    }

    private int slotAt(BlockState state, BlockPos pos, Vec3 location) {
        Vector3f v = new Vector3f((float) (location.x - pos.getX() - 0.5), (float) (location.y - pos.getY() - 0.5),
                (float) (location.z - pos.getZ() - 0.5));
        rotation(state).conjugate().transform(v);
        double x = v.x * 16;
        double y = (v.y + 0.5) * 16;
        int level = Mth.clamp((int) Math.floor(y / step), 0, levels - 1);
        int column = Mth.clamp((int) Math.floor((x + WIDTH / 2) / WIDTH * COLUMNS), 0, COLUMNS - 1);
        return level * COLUMNS + column;
    }

    private static int freeSlot(LampBlockEntity be, int slot) {
        int base = slot / COLUMNS * COLUMNS;
        int column = slot % COLUMNS;
        int best = -1;
        for (int c = 0; c < COLUMNS; c++) {
            if (!be.shelfItem(base + c).isEmpty()) continue;
            if (best < 0 || Math.abs(c - column) < Math.abs(best - base - column)) best = base + c;
        }
        return best;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean tool = stack.getItem() instanceof ChannelItem || stack.getItem() instanceof LedTunerItem;
        boolean paint = stack.getItem() instanceof DyeItem;
        if (tool || paint) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!(level.getBlockEntity(pos) instanceof LampBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        int slot = freeSlot(be, slotAt(state, pos, hit.getLocation()));
        if (slot < 0) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.sqdecor.shelf_full"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            be.setShelfItem(slot, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof LampBlockEntity be) {
                int slot = slotAt(state, pos, hit.getLocation());
                ItemStack item = be.shelfItem(slot);
                if (!item.isEmpty()) {
                    if (!level.isClientSide) {
                        be.setShelfItem(slot, ItemStack.EMPTY);
                        player.getInventory().placeItemBackInInventory(item);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (!lit) return InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof LampBlockEntity be) {
            be.dropShelfItems(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
