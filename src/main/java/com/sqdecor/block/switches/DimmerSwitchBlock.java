package com.sqdecor.block.switches;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.LedLogic;
import com.sqdecor.led.LightChannels;
import com.sqdecor.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class DimmerSwitchBlock extends LightSwitchBlock {
    public static final MapCodec<DimmerSwitchBlock> CODEC = simpleCodec(DimmerSwitchBlock::new);
    private static final int[] STEPS = {0, 1, 3, 5, 8, 11, 15};

    public DimmerSwitchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide) press(state, level, pos, player, upperHalf(state, pos, hit.getLocation()));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean upperHalf(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atCenterOf(pos));
        if (state.getValue(FACE) == AttachFace.WALL) return local.y >= 0;
        Direction facing = state.getValue(FACING);
        return local.x * facing.getStepX() + local.z * facing.getStepZ() <= 0;
    }

    private static void press(BlockState state, Level level, BlockPos pos, Player player, boolean up) {
        MinecraftServer server = level.getServer();
        if (server == null || !(level.getBlockEntity(pos) instanceof LightSwitchBlockEntity be)) return;
        if (be.getChannel() == 0) be.setChannel(LightChannels.newChannel(level.random));
        LightChannels channels = LightChannels.get(server);
        int current = channels.isOn(be.getChannel()) ? channels.level(be.getChannel(), LedLogic.DEFAULT_LEVEL) : 0;
        int next = up ? stepUp(current) : stepDown(current);
        channels.dim(server, be.getChannel(), next);
        be.channelChanged(next > 0);
        ModSounds.playSwitch(level, pos, ModSounds.LIGHT_SWITCH, up);
        player.displayClientMessage(next == 0
                ? Component.translatable("message.sqdecor.dimmer_off")
                : Component.translatable("message.sqdecor.dimmer", next), true);
    }

    private static int stepUp(int level) {
        for (int step : STEPS) if (step > level) return step;
        return 15;
    }

    private static int stepDown(int level) {
        for (int i = STEPS.length - 1; i >= 0; i--) if (STEPS[i] < level) return STEPS[i];
        return 0;
    }
}
