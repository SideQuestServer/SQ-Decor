package com.sqdecor.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.sqdecor.block.entity.PaintBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

public class TemplateModel extends BakedModelWrapper<BakedModel> {
    private record Key(@Nullable BlockState state, BlockState template, @Nullable Direction side) {}

    private static final int CACHE_SIZE = 512;
    private static final int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
    private static final int UV = 4;

    private final Map<Key, List<BakedQuad>> cache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Key, List<BakedQuad>> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public TemplateModel(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType type) {
        BlockState template = data.get(PaintBlockEntity.TEMPLATE);
        if (template == null) return super.getQuads(state, side, rand, data, type);
        if (type != null && !templateTypes(template, rand).contains(type)) return List.of();
        Key key = new Key(state, template, side);
        List<BakedQuad> cached = cache.get(key);
        if (cached != null) return cached;
        List<BakedQuad> built = retexture(super.getQuads(state, side, rand, ModelData.EMPTY, null), template);
        cache.put(key, built);
        return built;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState template = data.get(PaintBlockEntity.TEMPLATE);
        return template == null ? super.getRenderTypes(state, rand, data) : templateTypes(template, rand);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState template = data.get(PaintBlockEntity.TEMPLATE);
        return template == null ? super.getParticleIcon(data) : LampBakedModel.sprite(template, Direction.UP);
    }

    private static ChunkRenderTypeSet templateTypes(BlockState template, RandomSource rand) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(template)
                .getRenderTypes(template, rand, ModelData.EMPTY);
    }

    private static List<BakedQuad> retexture(List<BakedQuad> quads, BlockState template) {
        Map<Direction, TextureAtlasSprite> sprites = new EnumMap<>(Direction.class);
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite from = quad.getSprite();
            TextureAtlasSprite to = sprites.computeIfAbsent(quad.getDirection(), dir -> LampBakedModel.sprite(template, dir));
            int[] vertices = quad.getVertices().clone();
            float du = from.getU1() - from.getU0();
            float dv = from.getV1() - from.getV0();
            for (int i = 0; i < 4; i++) {
                int at = i * STRIDE + UV;
                float u = (Float.intBitsToFloat(vertices[at]) - from.getU0()) / du;
                float v = (Float.intBitsToFloat(vertices[at + 1]) - from.getV0()) / dv;
                vertices[at] = Float.floatToRawIntBits(to.getU(u));
                vertices[at + 1] = Float.floatToRawIntBits(to.getV(v));
            }
            out.add(new BakedQuad(vertices, -1, quad.getDirection(), to, quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return List.copyOf(out);
    }
}
