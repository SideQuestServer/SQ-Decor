package com.sqdecor.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.mojang.math.Transformation;
import com.sqdecor.lamp.ObjFile;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

public class LampGeometry implements IUnbakedGeometry<LampGeometry> {
    private final ObjFile obj;
    private final Map<String, String> materials;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private boolean[] hidden;

    LampGeometry(ObjFile obj, Map<String, String> materials, float offsetX, float offsetY, float offsetZ) {
        this.obj = obj;
        this.materials = materials;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                           ModelState state, ItemOverrides overrides) {
        Transformation transform = state.getRotation();
        if (!transform.isIdentity()) transform = transform.blockCenterToCorner();

        Map<String, TextureAtlasSprite> byMaterial = new HashMap<>();
        for (ObjFile.Face face : obj.faces) {
            String material = material(face);
            byMaterial.computeIfAbsent(material, m -> sprites.apply(material(context, m)));
        }
        Transformation rotation = transform;
        IntFunction<List<LampFace>> faces = turn -> {
            boolean[] hidden = hidden(byMaterial);
            List<LampFace> list = new ArrayList<>(obj.faces.size());
            for (int i = 0; i < obj.faces.size(); i++) {
                if (hidden[i]) continue;
                ObjFile.Face face = obj.faces.get(i);
                list.add(LampFace.of(obj, face, byMaterial.get(material(face)), rotation, offsetX, offsetY, offsetZ, turn));
            }
            return list;
        };

        TextureAtlasSprite particle = sprites.apply(context.hasMaterial("particle")
                ? context.getMaterial("particle") : context.getMaterial("texture"));

        ResourceLocation hint = context.getRenderTypeHint();
        RenderTypeGroup group = hint != null ? context.getRenderType(hint) : RenderTypeGroup.EMPTY;
        RenderType renderType = group.isEmpty() ? RenderType.cutout() : group.block();

        return new LampBakedModel(faces, particle, context.getTransforms(), context.useAmbientOcclusion(),
                context.isGui3d(), context.useBlockLight(), renderType, overrides);
    }

    private synchronized boolean[] hidden(Map<String, TextureAtlasSprite> byMaterial) {
        if (hidden == null) {
            List<LampFace> flat = new ArrayList<>(obj.faces.size());
            for (ObjFile.Face face : obj.faces) {
                flat.add(LampFace.of(obj, face, byMaterial.get(material(face)), Transformation.identity(), offsetX, offsetY, offsetZ, 0));
            }
            hidden = FaceCleanup.hidden(flat, obj.faces.stream().map(ObjFile.Face::object).toList());
        }
        return hidden;
    }

    private static String material(ObjFile.Face face) {
        return face.material() == null ? "" : face.material();
    }

    private Material material(IGeometryBakingContext context, String material) {
        String png = materials.get(material);
        if (png != null && context.hasMaterial(png)) return context.getMaterial(png);
        if (!material.isEmpty() && context.hasMaterial(material)) return context.getMaterial(material);
        return context.getMaterial("texture");
    }
}
