package com.sqdecor.compat.create;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.sqdecor.block.Attached;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CreateCompat {
    private CreateCompat() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CreateCompat::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
                Direction support = support(state);
                return support == null ? CheckResult.PASS : CheckResult.of(direction == support);
            });
            BlockMovementChecks.registerNotSupportiveCheck((state, direction) -> {
                Direction support = support(state);
                return support == null ? CheckResult.PASS : CheckResult.of(direction != support);
            });
        });
    }

    @Nullable
    private static Direction support(BlockState state) {
        return state.getBlock() instanceof Attached attached ? attached.supportDirection(state) : null;
    }
}
