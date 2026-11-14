package com.sqdecor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.lamp.BeaconLampBlock;
import com.sqdecor.block.lamp.HangLampBlock;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.block.lamp.ShelfBlock;
import com.sqdecor.block.lamp.TallShelfBlock;
import com.sqdecor.client.ClientConfig;
import com.sqdecor.client.PerfProbe;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LedRenderer<T extends LedBlockEntity> implements BlockEntityRenderer<T> {
    private static final int CHAIN_CHECK_TICKS = 10;

    private final BlockRenderDispatcher blocks;

    public LedRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();
        LampBlockEntity lamp = be instanceof LampBlockEntity l ? l : null;
        boolean bulb = HotBulbs.active(be);
        boolean chain = lamp != null && state.getBlock() instanceof HangLampBlock && !ChunkExtras.chainInMesh(lamp);
        boolean strip = lamp != null && state.getBlock() instanceof com.sqdecor.block.lamp.AnimatedStripBlock;
        boolean shelf = lamp != null && state.getBlock() instanceof ShelfBlock && lamp.hasShelfItems();
        boolean lit = !(state.getBlock() instanceof LampBlock lampBlock) || lampBlock.hasLight();
        int mode = be.effectiveLightMode();
        boolean on = lit && state.hasProperty(LedLogic.LIGHT_LEVEL) && state.getValue(LedLogic.LIGHT_LEVEL) > 0
                && !LateBuffers.shadowPass();
        boolean beacon = on && lamp != null && state.getBlock() instanceof BeaconLampBlock && ClientConfig.LIGHT_CONES.get();
        boolean glow = on && ClientConfig.HALO.get() && mode >= 1;
        boolean cone = on && ClientConfig.LIGHT_CONES.get() && mode >= 2 && lamp != null;
        if (!bulb && !chain && !strip && !shelf && !beacon && !glow && !cone) {
            if (PerfProbe.on) PerfProbe.idle++;
            return;
        }
        if (Occlusion.hidden(be, getRenderBoundingBox(be))) {
            if (PerfProbe.on) PerfProbe.hidden++;
            return;
        }
        if (PerfProbe.on) PerfProbe.drawn++;
        if (bulb) HotBulbs.render(be, state, level, pose, buffers, overlay);
        if (chain) renderChain(lamp, level, pose, buffers);
        if (strip) StripAnimRenderer.render(lamp, state, level, partialTick, pose, buffers);
        if (beacon) BeaconRenderer.render(lamp, state, level, partialTick, pose, buffers);
        if (glow) {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            GlowRenderer.render(be, state, level, camera, pose, buffers);
        }
        if (cone) SpotConeRenderer.render(lamp, state, pose, buffers);
        if (shelf) ShelfItems.render(lamp, (ShelfBlock) state.getBlock(), state, level, pose, buffers, overlay);
    }

    @Override
    public boolean shouldRender(T be, Vec3 camera) {
        return be.hot || be.coolingUntil != 0 || BlockEntityRenderer.super.shouldRender(be, camera);
    }

    private static int chainLength(LampBlockEntity lamp, Level level) {
        long now = level.getGameTime();
        if (lamp.chainLength < 0 || now - lamp.chainCheckedAt >= CHAIN_CHECK_TICKS || now < lamp.chainCheckedAt) {
            lamp.chainLength = HangLampBlock.chainLength(level, lamp.getBlockPos());
            lamp.chainCheckedAt = now;
        }
        return lamp.chainLength;
    }

    private void renderChain(LampBlockEntity lamp, Level level, PoseStack pose, MultiBufferSource buffers) {
        int length = chainLength(lamp, level);
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockPos pos = lamp.getBlockPos();
        for (int i = 1; i <= length; i++) {
            pose.pushPose();
            pose.translate(0, i, 0);
            int light = LevelRenderer.getLightColor(level, pos.above(i));
            blocks.renderSingleBlock(chain, pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        BlockState state = be.getBlockState();
        int mode = be.effectiveLightMode();
        int chain = be instanceof LampBlockEntity lamp ? lamp.chainLength : 0;
        AABB box = be.renderBox;
        if (box != null && be.renderBoxState == state && be.renderBoxMode == mode && be.renderBoxChain == chain) return box;
        box = new AABB(be.getBlockPos()).minmax(GlowRenderer.bounds(state).move(be.getBlockPos()));
        if (state.getBlock() instanceof BeaconLampBlock) box = box.inflate(BeaconRenderer.LENGTH);
        if (mode >= 2) box = box.inflate(SpotConeRenderer.reach());
        if (state.getBlock() instanceof TallShelfBlock) box = box.expandTowards(0, 1, 0);
        if (chain > 0) box = box.expandTowards(0, chain, 0);
        be.renderBox = box;
        be.renderBoxState = state;
        be.renderBoxMode = mode;
        be.renderBoxChain = chain;
        return box;
    }

    @Override
    public int getViewDistance() {
        return ClientConfig.EFFECT_VIEW_DISTANCE.get();
    }
}
