package com.sqdecor.block.monitor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.Attached;
import com.sqdecor.block.entity.ImageMonitorBlockEntity;
import com.sqdecor.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ImageMonitorBlock extends HorizontalDirectionalBlock implements EntityBlock, Attached {
    public static final MapCodec<ImageMonitorBlock> CODEC = simpleCodec(ImageMonitorBlock::new);
    public static final int MAX_SIZE = 8;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    private static final VoxelShape FLOOR_SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape CEILING_SHAPE = Block.box(0, 14, 0, 16, 16, 16);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            double[] b = {0, 0, 14, 16, 16, 16};
            int turns = switch (d) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            for (int i = 0; i < turns; i++) b = new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
            SHAPES[d.get2DDataValue()] = Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
    }

    public record Screen(BlockPos origin, int width, int height, Direction facing, AttachFace face) {
        public Direction right() {
            return ImageMonitorBlock.right(facing, face);
        }

        public Direction up() {
            return ImageMonitorBlock.up(facing, face);
        }
    }

    static Direction right(Direction facing, AttachFace face) {
        return face == AttachFace.FLOOR ? facing.getClockWise() : facing.getCounterClockWise();
    }

    static Direction up(Direction facing, AttachFace face) {
        return face == AttachFace.WALL ? Direction.UP : facing;
    }

    public static int[] modelRotation(Direction facing, AttachFace face) {
        int y = ((int) facing.toYRot() + 180) % 360;
        return switch (face) {
            case WALL -> new int[]{0, y};
            case FLOOR -> new int[]{270, (y + 180) % 360};
            case CEILING -> new int[]{90, y};
        };
    }

    public ImageMonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction look = context.getHorizontalDirection();
        boolean shift = context.isSecondaryUseActive();
        if (!shift) {
            BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(clicked.getOpposite()));
            if (against.getBlock() instanceof ImageMonitorBlock) {
                return defaultBlockState().setValue(FACING, against.getValue(FACING)).setValue(FACE, against.getValue(FACE));
            }
        }
        BlockState state;
        if (clicked.getAxis().isHorizontal()) {
            state = defaultBlockState().setValue(FACING, clicked).setValue(FACE, AttachFace.WALL);
        } else if (clicked == Direction.UP) {
            state = shift ? defaultBlockState().setValue(FACING, look).setValue(FACE, AttachFace.FLOOR)
                    : defaultBlockState().setValue(FACING, look.getOpposite()).setValue(FACE, AttachFace.WALL);
        } else {
            state = defaultBlockState().setValue(FACING, look.getOpposite()).setValue(FACE, shift ? AttachFace.WALL : AttachFace.CEILING);
        }
        if (clicked.getAxis().isHorizontal()) return state;
        return joinNeighbour(context.getLevel(), context.getClickedPos(), state);
    }

    private static BlockState joinNeighbour(BlockGetter level, BlockPos pos, BlockState state) {
        AttachFace face = state.getValue(FACE);
        for (Direction d : Direction.values()) {
            BlockState other = level.getBlockState(pos.relative(d));
            if (!(other.getBlock() instanceof ImageMonitorBlock) || other.getValue(FACE) != face) continue;
            boolean inPlane = face == AttachFace.WALL ? d.getAxis() != other.getValue(FACING).getAxis() : d.getAxis().isHorizontal();
            if (inPlane) return state.setValue(FACING, other.getValue(FACING));
        }
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImageMonitorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (lvl instanceof ServerLevel server && be instanceof ImageMonitorBlockEntity monitor) monitor.serverTick(server);
        };
    }

    @Nullable
    @Override
    public Direction supportDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(FACING).getOpposite();
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> SHAPES[state.getValue(FACING).get2DDataValue()];
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            Screen screen = screen(level, pos);
            if (screen != null && level.getBlockEntity(screen.origin()) instanceof ImageMonitorBlockEntity main) {
                if (player.isShiftKeyDown()) {
                    main.cyclePpb(screen);
                    ModSounds.play(level, pos, ModSounds.MONITOR_ON, 0.6f);
                } else if (touch(level, pos, screen, main, player, hit)) {
                    ModSounds.play(level, pos, ModSounds.MONITOR_TOUCH, 0.5f);
                    return InteractionResult.SUCCESS;
                }
                int[] res = main.resolution(screen);
                player.displayClientMessage(Component.translatable("message.sqdecor.monitor_info",
                        screen.width(), screen.height(), res[0], res[1]), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void notifyResized(Level level, Screen screen, int width, int height) {
        Direction right = screen.right();
        Direction up = screen.up();
        for (int i = 0; i < screen.width(); i++) {
            for (int j = 0; j < screen.height(); j++) {
                if (level.getBlockEntity(screen.origin().relative(right, i).relative(up, j)) instanceof ImageMonitorBlockEntity be) {
                    for (ImageMonitorBlockEntity.TouchListener listener : be.touchListeners.values()) listener.resized(width, height);
                }
            }
        }
    }

    private static boolean touch(Level level, BlockPos pos, Screen screen, ImageMonitorBlockEntity main, Player player, BlockHitResult hit) {
        Direction right = screen.right();
        Direction up = screen.up();
        BlockPos offset = pos.subtract(screen.origin());
        int a = step(offset, right);
        int b = step(offset, up);
        Vec3 f = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        double u = frac(f, right);
        double v = frac(f, up);
        int[] res = main.resolution(screen);
        int px = Math.max(1, Math.min(res[0], (int) Math.floor((a + u) / screen.width() * res[0]) + 1));
        int py = Math.max(1, Math.min(res[1], (int) Math.floor((screen.height() - (b + v)) / screen.height() * res[1]) + 1));
        boolean heard = false;
        for (int i = 0; i < screen.width(); i++) {
            for (int j = 0; j < screen.height(); j++) {
                BlockPos p = screen.origin().relative(right, i).relative(up, j);
                if (level.getBlockEntity(p) instanceof ImageMonitorBlockEntity be) {
                    for (ImageMonitorBlockEntity.TouchListener listener : be.touchListeners.values()) {
                        listener.touch(player.getName().getString(), px, py);
                        heard = true;
                    }
                }
            }
        }
        return heard;
    }

    private static int step(BlockPos offset, Direction d) {
        return offset.getX() * d.getStepX() + offset.getY() * d.getStepY() + offset.getZ() * d.getStepZ();
    }

    private static double frac(Vec3 f, Direction d) {
        double value = f.get(d.getAxis());
        return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? value : 1 - value;
    }

    private static boolean same(BlockGetter level, BlockPos pos, Direction facing, AttachFace face) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ImageMonitorBlock && state.getValue(FACING) == facing && state.getValue(FACE) == face;
    }

    @Nullable
    public static Screen screen(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ImageMonitorBlock)) return null;
        Direction facing = state.getValue(FACING);
        AttachFace face = state.getValue(FACE);
        Direction right = right(facing, face);
        Direction up = up(facing, face);
        Screen single = new Screen(pos.immutable(), 1, 1, facing, face);

        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(pos.immutable());
        queue.add(pos.immutable());
        int minA = 0, maxA = 0, minB = 0, maxB = 0;
        Direction[] steps = {right, right.getOpposite(), up, up.getOpposite()};
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockPos offset = p.subtract(pos);
            int a = step(offset, right);
            int b = step(offset, up);
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minB = Math.min(minB, b);
            maxB = Math.max(maxB, b);
            if (maxA - minA >= MAX_SIZE || maxB - minB >= MAX_SIZE) return single;
            for (Direction d : steps) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && same(level, n, facing, face)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        int width = maxA - minA + 1;
        int height = maxB - minB + 1;
        if (seen.size() != width * height) return single;
        return new Screen(pos.relative(right, minA).relative(up, minB), width, height, facing, face);
    }
}
