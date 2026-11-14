package com.sqdecor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.client.ClientConfig;
import com.sqdecor.client.PerfProbe;
import com.sqdecor.client.model.HotBulbModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class HotBulbs {
    private HotBulbs() {}

    private static final long COOL_AFTER_NANOS = 2_000_000_000L;
    private static final long OVERLAP_NANOS = 700_000_000L;

    private record Part(RenderType type, BakedQuad quad) {}

    private static final Map<BlockState, List<Part>> PARTS = new ConcurrentHashMap<>();

    public static void clear() {
        PARTS.clear();
    }

    public static boolean allowed(LedBlockEntity be) {
        return ClientConfig.CHUNK_MESHES.get()
                && Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState()) instanceof HotBulbModel;
    }

    public static boolean active(LedBlockEntity be) {
        if (!be.hot && be.coolingUntil == 0) return false;
        long now = System.nanoTime();
        if (be.hot && (now - be.lastColorNanos > COOL_AFTER_NANOS || !ClientConfig.CHUNK_MESHES.get())) {
            be.hot = false;
            be.coolingUntil = now + OVERLAP_NANOS;
            be.rebuildClient();
        }
        if (be.hot) return true;
        if (now < be.coolingUntil) return true;
        be.coolingUntil = 0;
        return false;
    }

    public static void render(LedBlockEntity be, BlockState state, Level level, PoseStack pose, MultiBufferSource buffers,
                              int overlay) {
        List<Part> parts = PARTS.computeIfAbsent(state, HotBulbs::collect);
        if (parts.isEmpty()) return;
        if (PerfProbe.on) PerfProbe.hot++;
        BlockPos pos = be.getBlockPos();
        int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        if (color == -1) color = 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        int light = LevelRenderer.getLightColor(level, pos);
        PoseStack.Pose last = pose.last();
        for (Part part : parts) {
            VertexConsumer vc = buffers.getBuffer(part.type());
            if (!part.quad().isShade()) vc = new UpNormal(vc);
            vc.putBulkData(last, part.quad(), r, g, b, 1f, light, overlay, false);
        }
    }

    private static List<Part> collect(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create();
        List<Part> out = new ArrayList<>();
        for (RenderType chunkType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            RenderType type = RenderTypeHelper.getEntityRenderType(chunkType, false);
            for (int i = -1; i < 6; i++) {
                Direction side = i < 0 ? null : Direction.from3DDataValue(i);
                random.setSeed(42L);
                for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, chunkType)) {
                    if (quad.getTintIndex() == 0) out.add(new Part(type, quad));
                }
            }
        }
        return List.copyOf(out);
    }

    private record UpNormal(VertexConsumer inner) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            inner.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            inner.setNormal(0f, 1f, 0f);
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight,
                              float normalX, float normalY, float normalZ) {
            inner.addVertex(x, y, z, color, u, v, packedOverlay, packedLight, 0f, 1f, 0f);
        }
    }
}
