package com.sqdecor.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sqdecor.SQDecor;
import com.sqdecor.lamp.ObjFile;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class LampGeometryLoader implements IGeometryLoader<LampGeometry> {
    public static final LampGeometryLoader INSTANCE = new LampGeometryLoader();
    public static final ResourceLocation ID = SQDecor.id("lamp");

    private LampGeometryLoader() {}

    @Override
    public LampGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
        ResourceLocation location = ResourceLocation.parse(GsonHelper.getAsString(json, "model"));
        ResourceManager resources = Minecraft.getInstance().getResourceManager();

        ObjFile obj;
        try (Reader reader = open(resources, location)) {
            obj = ObjFile.read(reader);
        } catch (IOException | RuntimeException e) {
            throw new JsonParseException("OBJ " + location + " konnte nicht gelesen werden", e);
        }

        Map<String, String> materials = obj.mtllib == null ? Map.of() : readMtl(resources, sibling(location, obj.mtllib));

        float[] offset = {8, 0, 8};
        if (json.has("offset")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "offset");
            for (int i = 0; i < 3; i++) offset[i] = array.get(i).getAsFloat();
        }
        return new LampGeometry(obj, materials, offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
    }

    private static Reader open(ResourceManager resources, ResourceLocation location) throws IOException {
        Optional<Resource> resource = resources.getResource(location);
        if (resource.isEmpty()) throw new IOException("nicht gefunden");
        return resource.get().openAsReader();
    }

    private static Map<String, String> readMtl(ResourceManager resources, ResourceLocation location) {
        Map<String, String> out = new HashMap<>();
        try (BufferedReader in = new BufferedReader(open(resources, location))) {
            String material = null;
            String line;
            while ((line = in.readLine()) != null) {
                line = line.strip();
                if (line.startsWith("newmtl ")) {
                    material = line.substring(7).strip();
                } else if (line.startsWith("map_Kd ") && material != null) {
                    String file = line.substring(7).strip().replace('\\', '/');
                    file = file.substring(file.lastIndexOf('/') + 1);
                    if (file.endsWith(".png")) file = file.substring(0, file.length() - 4);
                    out.put(material, file);
                }
            }
        } catch (IOException e) {
        }
        return out;
    }

    private static ResourceLocation sibling(ResourceLocation file, String name) {
        String path = file.getPath();
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path.substring(0, path.lastIndexOf('/') + 1) + name);
    }
}
