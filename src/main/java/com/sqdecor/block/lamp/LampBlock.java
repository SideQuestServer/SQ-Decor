package com.sqdecor.block.lamp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.item.ChannelItem;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.lamp.ObjFile;
import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LampBlock extends LedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);

    private final String model;
    private final VoxelShape[][] shapes = new VoxelShape[4][6];
    private final VoxelShape[][] collisions = new VoxelShape[4][6];
    private volatile List<LampPart> clickOrder;

    public LampBlock(Properties properties, String model, double[] base) {
        super(properties);
        this.model = model;
        for (int turn = 0; turn < 4; turn++) {
            for (Direction dir : Direction.values()) {
                VoxelShape shape = shape(base, dir, turn);
                shapes[turn][dir.get3DDataValue()] = shape;
                collisions[turn][dir.get3DDataValue()] = Shapes.join(shape, Shapes.block(), BooleanOp.AND);
            }
        }
        registerDefaultState(stateDefinition.any()
                .setValue(LedLogic.LIGHT_LEVEL, LedLogic.DEFAULT_LEVEL)
                .setValue(FACING, Direction.UP)
                .setValue(TURN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, TURN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        int turn = face.getAxis().isVertical() ? turnFor(context.getHorizontalDirection()) : 0;
        return placementState(context).setValue(FACING, face).setValue(TURN, turn);
    }

    public static int turnFor(Direction horizontal) {
        return (horizontal.get2DDataValue() + 2) % 4;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
    }

    public String model() {
        return model;
    }

    public boolean hasLight() {
        return true;
    }

    @Override
    public Direction supportDirection(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(TURN)][state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hasCollision ? collisions[state.getValue(TURN)][state.getValue(FACING).get3DDataValue()] : Shapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis().isHorizontal()) return state.setValue(FACING, rotation.rotate(facing));
        int quarters = switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
        if (facing == Direction.DOWN) quarters = 4 - quarters;
        return state.setValue(TURN, (state.getValue(TURN) + quarters) % 4);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis().isHorizontal()) return state.setValue(FACING, mirror.mirror(facing));
        int turn = state.getValue(TURN);
        return switch (mirror) {
            case NONE -> state;
            case LEFT_RIGHT -> state.setValue(TURN, Math.floorMod(2 - turn, 4));
            case FRONT_BACK -> state.setValue(TURN, Math.floorMod(-turn, 4));
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ChannelItem) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LampBlockEntity be) {
                LampPart part = be.paintNext(clickOrder(), dye.getDyeColor().getTextureDiffuseColor());
                show(player, part, Component.translatable("color.minecraft." + dye.getDyeColor().getName()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem item && LampBlockEntity.canUseAsTexture(item.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LampBlockEntity be) {
                LampPart part = be.textureNext(clickOrder(), item.getBlock());
                if (part == null) {
                    player.displayClientMessage(Component.translatable("message.sqdecor.lamp_no_case"), true);
                } else {
                    show(player, part, item.getBlock().getName());
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide) level.setBlock(pos, state.cycle(TURN), Block.UPDATE_ALL);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void show(Player player, LampPart part, Component value) {
        player.displayClientMessage(Component.translatable("message.sqdecor.lamp_part", part.displayName(), value), true);
    }

    public List<LampPart> clickOrder() {
        List<LampPart> order = clickOrder;
        if (order == null) clickOrder = order = readClickOrder();
        return order;
    }

    private List<LampPart> readClickOrder() {
        String path = "/assets/" + SQDecor.MODID + "/models/block/" + model + ".obj";
        try (InputStream in = LampBlock.class.getResourceAsStream(path)) {
            if (in != null) {
                Set<LampPart> found = ObjFile.read(new InputStreamReader(in, StandardCharsets.UTF_8)).parts();
                List<LampPart> order = Arrays.stream(LampPart.PAINTABLE).filter(found::contains).toList();
                if (!order.isEmpty()) return order;
            }
        } catch (IOException | RuntimeException e) {
            SQDecor.LOGGER.warn("Konnte {} nicht lesen", path, e);
        }
        SQDecor.LOGGER.warn("Keine Teile bulb/case/case2 in {}, nehme alle drei", path);
        return List.of(LampPart.PAINTABLE);
    }

    private static VoxelShape shape(double[] base, Direction facing, int turn) {
        double[] b = base;
        for (int i = 0; i < turn; i++) b = rotY(b);
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
        for (int i = 0; i < xTurns; i++) b = rotX(b);
        for (int i = 0; i < yTurns; i++) b = rotY(b);
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    private static double[] rotY(double[] b) {
        return new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
    }

    private static double[] rotX(double[] b) {
        return new double[]{b[0], b[2], 16 - b[4], b[3], b[5], 16 - b[1]};
    }
}
