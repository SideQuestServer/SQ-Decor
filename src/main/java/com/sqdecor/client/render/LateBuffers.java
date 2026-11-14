package com.sqdecor.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class LateBuffers {
    private LateBuffers() {}

    public static final RenderStateShard.TransparencyStateShard LIGHT_OVER = new RenderStateShard.TransparencyStateShard(
            "sqdecor_light_over", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    public static int cover(float light) {
        return Math.round(Mth.clamp(light, 0f, 1f) * 255);
    }

    private static final MultiBufferSource.BufferSource SOURCE = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 18));

    public static VertexConsumer get(RenderType type, MultiBufferSource normal) {
        return shaderPackActive() ? normal.getBuffer(type) : SOURCE.getBuffer(type);
    }

    private static final java.lang.invoke.MethodHandle IRIS_ACTIVE = findIris("isShaderPackInUse");
    private static final java.lang.invoke.MethodHandle IRIS_SHADOW = findIris("isRenderingShadowPass");

    private static java.lang.invoke.MethodHandle findIris(String method) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return java.lang.invoke.MethodHandles.lookup().unreflect(api.getMethod(method)).bindTo(instance);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static boolean ask(java.lang.invoke.MethodHandle handle) {
        if (handle == null) return false;
        try {
            return (boolean) handle.invoke();
        } catch (Throwable e) {
            return false;
        }
    }

    private static long frame, frameAsked = -1;
    private static boolean packCached;

    private static boolean shaderPackActive() {
        if (IRIS_ACTIVE == null) return false;
        if (frameAsked != frame) {
            frameAsked = frame;
            packCached = ask(IRIS_ACTIVE);
        }
        return packCached;
    }

    public static boolean shadowPass() {
        return shaderPackActive() && ask(IRIS_SHADOW);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        GlowRenderer.endFrame();
        Occlusion.endFrame();
        frame++;
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        SOURCE.endBatch();
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
