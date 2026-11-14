package com.sqdecor.block.switches;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.Attached;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.client.DeviceClient;
import com.sqdecor.item.ChannelItem;
import com.sqdecor.led.DeviceSetting;
import com.sqdecor.led.LightChannels;
import com.sqdecor.led.LightLinks;
import com.sqdecor.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightSwitchBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock, Attached {
    public static final MapCodec<LightSwitchBlock> CODEC = simpleCodec(LightSwitchBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape FLOOR_NS = Block.box(5, 0, 4, 11, 2, 12);
    private static final VoxelShape FLOOR_EW = Block.box(4, 0, 5, 12, 2, 11);
    private static final VoxelShape CEILING_NS = Block.box(5, 14, 4, 11, 16, 12);
    private static final VoxelShape CEILING_EW = Block.box(4, 14, 5, 12, 16, 11);
    private static final VoxelShape WALL_NORTH = Block.box(5, 4, 14, 11, 12, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(5, 4, 0, 11, 12, 2);
    private static final VoxelShape WALL_EAST = Block.box(0, 4, 5, 2, 12, 11);
    private static final VoxelShape WALL_WEST = Block.box(14, 4, 5, 16, 12, 11);

    public LightSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightSwitchBlockEntity(pos, state);
    }

    public List<DeviceSetting> settings() {
        return List.of();
    }

    protected boolean ticks() {
        return false;
    }

    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state, LightSwitchBlockEntity be) {
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !ticks()) return null;
        return (lvl, pos, st, be) -> {
            if (lvl instanceof ServerLevel server && be instanceof LightSwitchBlockEntity sw) serverTick(server, pos, st, sw);
        };
    }

    protected static void switchChannel(ServerLevel level, BlockPos pos, LightSwitchBlockEntity be, boolean on) {
        if (be.getChannel() == 0) return;
        LightChannels channels = LightChannels.get(level.getServer());
        if (channels.isOn(be.getChannel()) == on) {
            be.channelChanged(on);
            return;
        }
        channels.set(level.getServer(), be.getChannel(), on);
        be.channelChanged(on);
        ModSounds.playSwitch(level, pos, ModSounds.LIGHT_SWITCH, on);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this) && oldState.getValue(POWERED) != state.getValue(POWERED)) updateNeighbours(state, level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(POWERED)) updateNeighbours(state, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
    }

    @Nullable
    @Override
    public Direction supportDirection(BlockState state) {
        return getConnectedDirection(state).getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        return switch (state.getValue(FACE)) {
            case FLOOR -> ns ? FLOOR_NS : FLOOR_EW;
            case CEILING -> ns ? CEILING_NS : CEILING_EW;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> WALL_SOUTH;
                case EAST -> WALL_EAST;
                case WEST -> WALL_WEST;
                default -> WALL_NORTH;
            };
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be)) return;
        if (be.getChannel() == 0) be.setChannel(LightChannels.newChannel(level.random));
        LightChannels channels = LightChannels.get(level);
        if (channels != null) be.channelChanged(channels.isOn(be.getChannel()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be) {
                if (be.getChannel() == 0) be.setChannel(LightChannels.newChannel(level.random));
                LightLinks.toggle(player, level, pos, be.getChannel());
            }
        } else if (!settings().isEmpty()) {
            if (level.isClientSide) DeviceClient.open(pos, settings());
        } else if (!level.isClientSide) {
            flip(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ChannelItem && player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be && be.getChannel() != 0) {
                ChannelItem.setChannel(stack, be.getChannel());
                player.displayClientMessage(Component.translatable("message.sqdecor.channel_copied",
                        LightChannels.label(be.getChannel())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected static void flip(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null || !(level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be)) return;
        if (be.getChannel() == 0) be.setChannel(LightChannels.newChannel(level.random));
        boolean on = LightChannels.get(server).toggle(server, be.getChannel());
        be.channelChanged(on);
        ModSounds.playSwitch(level, pos, ModSounds.LIGHT_SWITCH, on);
    }
}
