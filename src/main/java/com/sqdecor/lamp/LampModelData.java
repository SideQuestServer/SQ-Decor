package com.sqdecor.lamp;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class LampModelData {
    private LampModelData() {}

    public static final ModelProperty<BlockState> CASE = new ModelProperty<>();
    public static final ModelProperty<BlockState> CASE2 = new ModelProperty<>();

    public static ModelProperty<BlockState> of(LampPart part) {
        return part == LampPart.CASE2 ? CASE2 : CASE;
    }
}
