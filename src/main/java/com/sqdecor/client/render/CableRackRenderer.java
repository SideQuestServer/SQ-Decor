package com.sqdecor.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sqdecor.SQDecor;
import com.sqdecor.block.cable.CableClipBlock;
import com.sqdecor.block.cable.CableRackBlock;
import com.sqdecor.block.entity.CableRackBlockEntity;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.block.tape.TapeAnchorBlock;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CableRackRenderer implements BlockEntityRenderer<CableRackBlockEntity> {
    private static final ResourceLocation CABLE_TEXTURE = SQDecor.id("textures/misc/thin_cable.png");
    private static final java.util.Map<CableRackBlockEntity, WireMesh> MESHES = new java.util.WeakHashMap<>();

    public CableRackRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(CableRackBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CableRackBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        for (CableRackBlockEntity.Link l : be.links()) box = box.minmax(new AABB(l.other()));
        return box.inflate(2);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(CableRackBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || be.links().isEmpty()) return;
        BlockState self = be.getBlockState();
        if (!(self.getBlock() instanceof CableRackBlock)) return;
        if (LateBuffers.shadowPass()) return;
        AABB box = getRenderBoundingBox(be);
        if (Occlusion.outsideView(box) || Occlusion.hidden(be, box)) return;

        BlockPos pos = be.getBlockPos();
        int signature = self.hashCode() * 31 + be.links().hashCode();
        for (CableRackBlockEntity.Link l : be.links()) signature = signature * 31 + be.color(l.slot()).ordinal();
        long now = level.getGameTime();
        WireMesh mesh = MESHES.get(be);
        if (mesh == null || mesh.stale(now, signature, pos)) {
            mesh = new WireMesh(now, signature);
            Matrix4f identity = new Matrix4f();
            Vec3 origin = Vec3.atLowerCornerOf(pos);
            for (CableRackBlockEntity.Link l : be.links()) {
                Vec3 to = otherEnd(level, pos, l);
                if (to == null) continue;
                Vec3 a = CableRackBlock.tip(pos, self, l.slot()).subtract(origin);
                Vec3 c = to.subtract(origin);
                float sag = TapeRenderer.fitSag(level, pos, a, c, true);
                int rgb = be.color(l.slot()).getTextureDiffuseColor();
                TapeRenderer.emit(mesh, identity, level, pos, true, a, c, sag,
                        FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb));
                mesh.endPart();
            }
            MESHES.put(be, mesh);
            if (com.sqdecor.client.PerfProbe.on) com.sqdecor.client.PerfProbe.wireBuilds++;
        }
        if (mesh.isEmpty()) return;
        mesh.draw(buffers.getBuffer(RenderType.entityCutoutNoCull(CABLE_TEXTURE)), pose.last(), pos);
    }

    private static Vec3 otherEnd(Level level, BlockPos self, CableRackBlockEntity.Link l) {
        BlockState state = level.getBlockState(l.other());
        if (l.otherSlot() < 0) {
            if (!(state.getBlock() instanceof CableClipBlock)) return null;
            if (!(level.getBlockEntity(l.other()) instanceof TapeAnchorBlockEntity clip) || !clip.links().contains(self)) return null;
            return TapeAnchorBlock.tip(l.other(), state);
        }
        if (!(state.getBlock() instanceof CableRackBlock)) return null;
        long a = self.asLong(), b = l.other().asLong();
        if (b < a || (b == a && l.otherSlot() < l.slot())) return null;
        if (!(level.getBlockEntity(l.other()) instanceof CableRackBlockEntity ob) || !ob.hasLink(l.otherSlot(), self, l.slot())) return null;
        return CableRackBlock.tip(l.other(), state, l.otherSlot());
    }
}
