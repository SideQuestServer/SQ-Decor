package com.sqdecor.client.model;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.math.Transformation;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.lamp.ObjFile;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

record LampFace(LampPart part, TextureAtlasSprite sprite, Direction direction,
                float[] pos, float[] normal, float[] uv) {

    static LampFace of(ObjFile obj, ObjFile.Face face, TextureAtlasSprite sprite, Transformation transform,
                       float offsetX, float offsetY, float offsetZ, int turn) {
        float[] pos = new float[12];
        float[] normal = new float[12];
        float[] uv = new float[8];
        int corners = face.v().length;
        for (int i = 0; i < 4; i++) {
            int c = Math.min(i, corners - 1);
            float[] p = obj.positions.get(face.v()[c]);
            float x = p[0] + offsetX;
            float y = p[1] + offsetY;
            float z = p[2] + offsetZ;
            for (int t = 0; t < turn; t++) {
                float oldX = x;
                x = 1 - z;
                z = oldX;
            }
            Vector4f v = new Vector4f(x, y, z, 1);
            transform.getMatrix().transform(v);
            pos[i * 3] = v.x();
            pos[i * 3 + 1] = v.y();
            pos[i * 3 + 2] = v.z();

            if (face.vt()[c] >= 0) {
                float[] t = obj.uvs.get(face.vt()[c]);
                uv[i * 2] = t[0];
                uv[i * 2 + 1] = 1 - t[1];
            }
            if (face.vn()[c] >= 0) {
                float[] n = obj.normals.get(face.vn()[c]);
                Vector3f nv = new Vector3f(n[0], n[1], n[2]);
                for (int t = 0; t < turn; t++) nv.set(-nv.z(), nv.y(), nv.x());
                transform.getNormalMatrix().transform(nv);
                nv.normalize();
                normal[i * 3] = nv.x();
                normal[i * 3 + 1] = nv.y();
                normal[i * 3 + 2] = nv.z();
            }
        }

        Vector3f a = new Vector3f(pos[3] - pos[0], pos[4] - pos[1], pos[5] - pos[2]);
        Vector3f b = new Vector3f(pos[6] - pos[0], pos[7] - pos[1], pos[8] - pos[2]);
        Vector3f faceNormal = a.cross(b);
        if (faceNormal.lengthSquared() > 1e-12f) faceNormal.normalize();
        for (int i = 0; i < 4; i++) {
            if (face.vn()[Math.min(i, corners - 1)] < 0) {
                normal[i * 3] = faceNormal.x();
                normal[i * 3 + 1] = faceNormal.y();
                normal[i * 3 + 2] = faceNormal.z();
            }
        }
        Direction direction = Direction.getNearest(faceNormal.x(), faceNormal.y(), faceNormal.z());
        return new LampFace(face.part(), sprite, direction, pos, normal, uv);
    }

    @org.jetbrains.annotations.Nullable
    Direction cullSide() {
        int axis = direction.getAxis().ordinal();
        float plane = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        for (int i = 0; i < 4; i++) {
            for (int a = 0; a < 3; a++) {
                float p = pos[i * 3 + a];
                if (a == axis ? Math.abs(p - plane) > EPS : p < -EPS || p > 1 + EPS) return null;
            }
        }
        return direction;
    }

    private static final float EPS = 1e-4f;

    BakedQuad bake() {
        return bake(sprite, uv, false);
    }

    BakedQuad bakeGlowing() {
        return bake(sprite, uv, part == LampPart.BULB);
    }

    BakedQuad bakeWith(TextureAtlasSprite blockSprite) {
        return bake(blockSprite, projectedUv(), false);
    }

    private BakedQuad bake(TextureAtlasSprite target, float[] uv01, boolean glow) {
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(target);
        builder.setDirection(direction);
        builder.setTintIndex(part.tintIndex);
        builder.setShade(!glow);
        builder.setHasAmbientOcclusion(!glow);
        int light = glow ? FULL_BRIGHT : 0;
        for (int i = 0; i < 4; i++) {
            builder.addVertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(target.getU(uv01[i * 2]), target.getV(uv01[i * 2 + 1]))
                    .setUv2(light, light)
                    .setNormal(normal[i * 3], normal[i * 3 + 1], normal[i * 3 + 2]);
        }
        return builder.bakeQuad();
    }

    private static final int FULL_BRIGHT = 240;

    private float[] projectedUv() {
        float[] out = new float[8];
        for (int i = 0; i < 4; i++) {
            float x = pos[i * 3];
            float y = pos[i * 3 + 1];
            float z = pos[i * 3 + 2];
            float u;
            float v;
            switch (direction) {
                case UP -> { u = x; v = z; }
                case DOWN -> { u = x; v = 1 - z; }
                case NORTH -> { u = 1 - x; v = 1 - y; }
                case SOUTH -> { u = x; v = 1 - y; }
                case WEST -> { u = z; v = 1 - y; }
                default -> { u = 1 - z; v = 1 - y; }
            }
            out[i * 2] = u;
            out[i * 2 + 1] = v;
        }
        fit(out, 0);
        fit(out, 1);
        return out;
    }

    private static void fit(float[] uv, int axis) {
        float min = Float.MAX_VALUE;
        for (int i = axis; i < 8; i += 2) min = Math.min(min, uv[i]);
        float shift = (float) Math.floor(min + 1e-4f);
        float max = 0;
        for (int i = axis; i < 8; i += 2) {
            uv[i] -= shift;
            max = Math.max(max, uv[i]);
        }
        if (max > 1) {
            for (int i = axis; i < 8; i += 2) uv[i] /= max;
        }
        for (int i = axis; i < 8; i += 2) uv[i] = Math.max(0, uv[i]);
    }
}
