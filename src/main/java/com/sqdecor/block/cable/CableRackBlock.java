package com.sqdecor.block.cable;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sqdecor.block.entity.CableRackBlockEntity;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableRackBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<CableRackBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("slots").forGetter(CableRackBlock::slots),
            propertiesCodec()).apply(i, CableRackBlock::new));
    public static final BooleanProperty TURNED = BooleanProperty.create("turned");

    public static final double HOOK_TIP = 14.3;

    private final int slots;
    private final VoxelShape[] shapes = new VoxelShape[12];

    public CableRackBlock(int slots, Properties properties) {
        super(properties);
        this.slots = slots;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TURNED, false));
        for (Direction d : Direction.values()) {
            for (int t = 0; t < 2; t++) {
                double half = barHalf(slots);
                shapes[d.get3DDataValue() * 2 + t] = box(d, t == 1, 8 - half, 7, 14, 8 + half, 9, 16);
            }
        }
    }

    public int slots() {
        return slots;
    }

    public static double spacing(int slots) {
        return switch (slots) {
            case 2 -> 4;
            case 3 -> 3.5;
            case 5 -> 3;
            default -> 1.4;
        };
    }

    public static double barHalf(int slots) {
        return (slots - 1) * spacing(slots) / 2 + 1.5;
    }

    public static double slotPos(int slots, int slot) {
        return 8 - (slots - 1) * spacing(slots) / 2 + slot * spacing(slots);
    }

    public static Direction along(BlockState state) {
        return along(state.getValue(FACING), state.getValue(TURNED));
    }

    public static Direction cross(BlockState state) {
        return cross(state.getValue(FACING), state.getValue(TURNED));
    }

    private static Direction along(Direction d, boolean turned) {
        if (d.getAxis().isHorizontal()) return turned ? Direction.UP : d.getClockWise();
        return turned ? Direction.SOUTH : Direction.EAST;
    }

    private static Direction cross(Direction d, boolean turned) {
        if (d.getAxis().isHorizontal()) return turned ? d.getClockWise() : Direction.UP;
        return turned ? Direction.EAST : Direction.SOUTH;
    }

    private static VoxelShape box(Direction d, boolean turned, double a0, double c0, double z0, double a1, double c1, double z1) {
        Direction along = along(d, turned);
        Direction cross = cross(d, turned);
        Vec3 p = toBlock(d, along, cross, a0, c0, z0);
        Vec3 q = toBlock(d, along, cross, a1, c1, z1);
        return Block.box(Math.min(p.x, q.x), Math.min(p.y, q.y), Math.min(p.z, q.z),
                Math.max(p.x, q.x), Math.max(p.y, q.y), Math.max(p.z, q.z));
    }

    private static Vec3 toBlock(Direction d, Direction along, Direction cross, double a, double c, double z) {
        double x = 8, y = 8, w = 8;
        x += along.getStepX() * (a - 8) + cross.getStepX() * (c - 8) + d.getStepX() * (8 - z);
        y += along.getStepY() * (a - 8) + cross.getStepY() * (c - 8) + d.getStepY() * (8 - z);
        w += along.getStepZ() * (a - 8) + cross.getStepZ() * (c - 8) + d.getStepZ() * (8 - z);
        return new Vec3(x, y, w);
    }

    public static Vec3 tip(BlockPos pos, BlockState state, int slot) {
        CableRackBlock rack = (CableRackBlock) state.getBlock();
        Vec3 local = toBlock(state.getValue(FACING), along(state), cross(state), slotPos(rack.slots, slot), 8, HOOK_TIP);
        return Vec3.atLowerCornerOf(pos).add(local.x / 16, local.y / 16, local.z / 16);
    }

    public int slotAt(BlockPos pos, BlockState state, Vec3 hit) {
        Direction a = along(state);
        double offset = (hit.x - pos.getX() - 0.5) * a.getStepX() + (hit.y - pos.getY() - 0.5) * a.getStepY()
                + (hit.z - pos.getZ() - 0.5) * a.getStepZ();
        double px = offset * 16 + 8;
        int best = 0;
        for (int i = 1; i < slots; i++) {
            if (Math.abs(slotPos(slots, i) - px) < Math.abs(slotPos(slots, best) - px)) best = i;
        }
        return best;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TURNED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        boolean turned;
        if (face.getAxis().isHorizontal()) {
            turned = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        } else {
            turned = context.getHorizontalDirection().getAxis() == Direction.Axis.X;
        }
        BlockState state = defaultBlockState().setValue(FACING, face).setValue(TURNED, turned);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(FACING).get3DDataValue() * 2 + (state.getValue(TURNED) ? 1 : 0)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return !level.getBlockState(support).getCollisionShape(level, support).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CableRackBlockEntity be) {
            java.util.Set<BlockPos> released = new java.util.HashSet<>();
            for (CableRackBlockEntity.Link link : java.util.List.copyOf(be.links())) {
                boolean had = false;
                if (link.otherSlot() >= 0) {
                    if (level.getBlockEntity(link.other()) instanceof CableRackBlockEntity ob)
                        had = ob.removeLink(link.otherSlot(), pos, link.slot());
                } else if (released.contains(link.other())) {
                    had = true;
                } else if (level.getBlockEntity(link.other()) instanceof TapeAnchorBlockEntity clip && clip.links().contains(pos)) {
                    clip.removeLink(pos);
                    released.add(link.other());
                    had = true;
                }
                if (had) Block.popResource(level, pos, new ItemStack(ModItems.THIN_CABLE.get()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            int slot = slotAt(pos, state, hit.getLocation());
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof CableRackBlockEntity be) {
                be.setColor(slot, dye.getDyeColor());
                for (CableRackBlockEntity.Link link : be.links()) {
                    if (link.slot() != slot || link.otherSlot() < 0) continue;
                    if (level.getBlockEntity(link.other()) instanceof CableRackBlockEntity ob) ob.setColor(link.otherSlot(), dye.getDyeColor());
                }
                player.displayClientMessage(Component.translatable("message.sqdecor.cable_rack_painted", slot + 1,
                        Component.translatable("color.minecraft." + dye.getDyeColor().getName())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableRackBlockEntity(pos, state);
    }
}
