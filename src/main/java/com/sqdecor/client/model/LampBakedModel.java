package com.sqdecor.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.lamp.LampModelData;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class LampBakedModel implements IDynamicBakedModel {
    private record Key(int turn, boolean glow, @Nullable BlockState caseTexture, @Nullable BlockState case2Texture, int solid) {}

    private record Turn(List<LampFace> faces, List<BakedQuad> quads, List<BakedQuad> glowQuads, Direction[] cullSides,
                        int sideMask) {
        static Turn of(List<LampFace> faces) {
            List<BakedQuad> quads = faces.stream().map(LampFace::bake).toList();
            List<BakedQuad> glow = new ArrayList<>(faces.size());
            for (int i = 0; i < faces.size(); i++) {
                glow.add(faces.get(i).part() == LampPart.BULB ? faces.get(i).bakeGlowing() : quads.get(i));
            }
            Direction[] sides = faces.stream().map(LampFace::cullSide).toArray(Direction[]::new);
            int mask = 0;
            for (Direction side : sides) if (side != null) mask |= 1 << side.get3DDataValue();
            return new Turn(faces, quads, List.copyOf(glow), sides, mask);
        }
    }

    private final IntFunction<List<LampFace>> source;
    private final AtomicReferenceArray<Turn> turns = new AtomicReferenceArray<>(4);
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean blockLight;
    private final RenderType renderType;
    private final ChunkRenderTypeSet renderTypes;
    private final ItemOverrides overrides;
    private final Map<Key, List<BakedQuad>> cache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Key, List<BakedQuad>> eldest) {
                    return size() > CACHE_SIZE;
                }
            });
    private static final int CACHE_SIZE = 256;

    private static final ModelProperty<Integer> SOLID_SIDES = new ModelProperty<>();

    LampBakedModel(IntFunction<List<LampFace>> source, TextureAtlasSprite particle, ItemTransforms transforms,
                   boolean ambientOcclusion, boolean gui3d, boolean blockLight, RenderType renderType,
                   ItemOverrides overrides) {
        this.source = source;
        this.particle = particle;
        this.transforms = transforms;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.blockLight = blockLight;
        this.renderType = renderType;
        this.renderTypes = ChunkRenderTypeSet.of(renderType);
        this.overrides = overrides;
    }

    private Turn turn(int turn) {
        Turn baked = turns.get(turn);
        if (baked != null) return baked;
        Turn fresh = Turn.of(source.apply(turn));
        return turns.compareAndSet(turn, null, fresh) ? fresh : turns.get(turn);
    }

    private static int turnOf(@Nullable BlockState state) {
        return state != null && state.hasProperty(LampBlock.TURN) ? state.getValue(LampBlock.TURN) : 0;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        int mask = turn(turnOf(state)).sideMask();
        if (mask == 0) return modelData;
        int solid = 0;
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            if ((mask & 1 << dir.get3DDataValue()) == 0) continue;
            neighbor.setWithOffset(pos, dir);
            if (level.getBlockState(neighbor).isSolidRender(level, neighbor)) solid |= 1 << dir.get3DDataValue();
        }
        return solid == 0 ? modelData : modelData.derive().with(SOLID_SIDES, solid).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType type) {
        if (side != null || (type != null && type != renderType)) return List.of();
        int turn = turnOf(state);
        Turn baked = turn(turn);
        boolean glow = state != null && state.hasProperty(LedLogic.LIGHT_LEVEL) && state.getValue(LedLogic.LIGHT_LEVEL) > 0;
        List<BakedQuad> plain = glow ? baked.glowQuads() : baked.quads();
        BlockState caseTexture = data.get(LampModelData.CASE);
        BlockState case2Texture = data.get(LampModelData.CASE2);
        Integer solidSides = data.get(SOLID_SIDES);
        int solid = solidSides == null ? 0 : solidSides & baked.sideMask();
        if (caseTexture == null && case2Texture == null && solid == 0) return plain;

        Key key = new Key(turn, glow, caseTexture, case2Texture, solid);
        List<BakedQuad> cached = cache.get(key);
        if (cached != null) return cached;
        List<BakedQuad> base = caseTexture == null && case2Texture == null ? plain
                : retexture(baked.faces(), plain, caseTexture, case2Texture);
        List<BakedQuad> built = List.copyOf(cull(base, baked.cullSides(), solid));
        cache.put(key, built);
        return built;
    }

    private static List<BakedQuad> cull(List<BakedQuad> all, Direction[] sides, int solid) {
        if (solid == 0) return all;
        List<BakedQuad> out = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            Direction side = sides[i];
            if (side == null || (solid & 1 << side.get3DDataValue()) == 0) out.add(all.get(i));
        }
        return out;
    }

    private static List<BakedQuad> retexture(List<LampFace> faces, List<BakedQuad> plain,
                                             @Nullable BlockState caseTexture, @Nullable BlockState case2Texture) {
        Map<Direction, TextureAtlasSprite> caseSprites = new EnumMap<>(Direction.class);
        Map<Direction, TextureAtlasSprite> case2Sprites = new EnumMap<>(Direction.class);
        List<BakedQuad> out = new ArrayList<>(faces.size());
        for (int i = 0; i < faces.size(); i++) {
            LampFace face = faces.get(i);
            BlockState texture = face.part() == LampPart.CASE ? caseTexture
                    : face.part() == LampPart.CASE2 ? case2Texture : null;
            if (texture == null) {
                out.add(plain.get(i));
                continue;
            }
            Map<Direction, TextureAtlasSprite> sprites = face.part() == LampPart.CASE ? caseSprites : case2Sprites;
            out.add(face.bakeWith(sprites.computeIfAbsent(face.direction(), dir -> sprite(texture, dir))));
        }
        return out;
    }

    static TextureAtlasSprite sprite(BlockState state, Direction dir) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> quads = model.getQuads(state, dir, rand, ModelData.EMPTY, null);
        if (!quads.isEmpty()) return quads.get(0).getSprite();
        for (BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, null)) {
            if (quad.getDirection() == dir) return quad.getSprite();
        }
        return model.getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return renderTypes;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState caseTexture = data.get(LampModelData.CASE);
        return caseTexture != null ? sprite(caseTexture, Direction.UP) : particle;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
