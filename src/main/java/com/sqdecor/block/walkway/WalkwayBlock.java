package com.sqdecor.block.walkway;

import com.sqdecor.block.industrial.Industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WalkwayBlock extends Block {
    public static final EnumProperty<SlabType> HALF = EnumProperty.create("half", SlabType.class, SlabType.BOTTOM, SlabType.TOP);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final VoxelShape[] SHAPES = new VoxelShape[32];
    private static final VoxelShape[] COLLISIONS = new VoxelShape[32];

    static {
        for (int i = 0; i < 32; i++) {
            boolean top = (i & 16) != 0;
            double y0 = top ? 14 : 0, y1 = top ? 16 : 2;
            double r0 = y1, r1 = top ? 30 : 16, c1 = top ? 24 : 16;
            VoxelShape shape = Block.box(0, y0, 0, 16, y1, 16), coll = shape;
            int k = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if ((i & (1 << k++)) == 0) continue;
                double[] b = switch (d) {
                    case NORTH -> new double[]{0, 0, 16, 1.5};
                    case SOUTH -> new double[]{0, 14.5, 16, 16};
                    case WEST -> new double[]{0, 0, 1.5, 16};
                    default -> new double[]{14.5, 0, 16, 16};
                };
                shape = Shapes.or(shape, Block.box(b[0], r0, b[1], b[2], Math.min(16, r1), b[3]));
                coll = Shapes.or(coll, Block.box(b[0], r0, b[1], b[2], c1, b[3]));
            }
            SHAPES[i] = shape;
            COLLISIONS[i] = coll;
        }
    }

    public WalkwayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, SlabType.BOTTOM).setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false).setValue(Industrial.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, NORTH, EAST, SOUTH, WEST, Industrial.COLOR);
    }

    public static BooleanProperty rail(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    @SuppressWarnings("deprecation")
    private boolean open(BlockState neighbor) {
        return !(neighbor.getBlock() instanceof WalkwayBlock) && !neighbor.isSolid();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        double y = context.getClickLocation().y - context.getClickedPos().getY();
        SlabType half = face == Direction.DOWN || (face != Direction.UP && y > 0.5) ? SlabType.TOP : SlabType.BOTTOM;
        BlockState state = defaultBlockState().setValue(HALF, half);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            state = state.setValue(rail(d), open(context.getLevel().getBlockState(context.getClickedPos().relative(d))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isVertical()) return state;
        return state.setValue(rail(direction), open(neighbor));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return Industrial.dye(state, level, pos, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        Vec3 local = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction side = Math.abs(local.x) > Math.abs(local.z)
                ? (local.x > 0 ? Direction.EAST : Direction.WEST)
                : (local.z > 0 ? Direction.SOUTH : Direction.NORTH);
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(rail(side)), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int index(BlockState state) {
        int i = state.getValue(HALF) == SlabType.TOP ? 16 : 0, k = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) if (state.getValue(rail(d))) i |= 1 << k++; else k++;
        return i;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[index(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISIONS[index(state)];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(rail(rotation.rotate(d)), state.getValue(rail(d)));
        return out;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(rail(mirror.mirror(d)), state.getValue(rail(d)));
        return out;
    }
}
