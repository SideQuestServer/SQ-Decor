package com.sqdecor.client.model;

import com.sqdecor.block.entity.LedBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class HotBulbModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> HOT = new ModelProperty<>();

    public HotBulbModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData data = super.getModelData(level, pos, state, modelData);
        if (level.getBlockEntity(pos) instanceof LedBlockEntity be && be.hot) return data.derive().with(HOT, true).build();
        return data;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                                    @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (!Boolean.TRUE.equals(data.get(HOT))) return quads;
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) if (quad.getTintIndex() != 0) out.add(quad);
        return out;
    }
}
