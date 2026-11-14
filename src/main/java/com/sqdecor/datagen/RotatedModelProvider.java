package com.sqdecor.datagen;

import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sqdecor.SQDecor;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class RotatedModelProvider implements DataProvider {
    public static final List<String> MODELS = List.of(
            "led/led_cage_bulb", "cable/cable_1", "cable/cable_2", "cable/cable_3",
            "cable/cable_1_anchor", "cable/cable_2_anchor", "cable/cable_3_anchor");

    private static final String[] SIDES = {"north", "east", "south", "west"};

    private final PackOutput output;
    private final ExistingFileHelper files;

    public RotatedModelProvider(PackOutput output, ExistingFileHelper files) {
        this.output = output;
        this.files = files;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path root = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(SQDecor.MODID).resolve("models/block");
        for (String path : MODELS) {
            JsonObject model;
            try (Reader reader = files.getResource(SQDecor.id("block/" + path), PackType.CLIENT_RESOURCES, ".json", "models").openAsReader()) {
                model = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                throw new IllegalStateException("Missing model block/" + path, e);
            }
            futures.add(DataProvider.saveStable(cache, rotate(model), root.resolve(path + "_rot.json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "SQ Decor rotated models";
    }

    public static JsonObject rotate(JsonObject model) {
        JsonObject out = model.deepCopy();
        if (!out.has("elements")) return out;
        for (JsonElement e : out.getAsJsonArray("elements")) {
            JsonObject element = e.getAsJsonObject();
            double[] from = vec(element.getAsJsonArray("from"));
            double[] to = vec(element.getAsJsonArray("to"));

            if (element.has("faces")) {
                JsonObject faces = element.getAsJsonObject("faces");
                JsonObject turned = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                    String side = entry.getKey();
                    JsonObject face = entry.getValue().getAsJsonObject();
                    if (!face.has("uv")) face.add("uv", array(defaultUv(side, from, to)));
                    if (face.has("cullface")) face.addProperty("cullface", turn(face.get("cullface").getAsString()));
                    if (side.equals("up")) addRotation(face, 90);
                    if (side.equals("down")) addRotation(face, 270);
                    turned.add(turn(side), face);
                }
                element.add("faces", turned);
            }

            element.add("from", array(new double[]{16 - to[2], from[1], from[0]}));
            element.add("to", array(new double[]{16 - from[2], to[1], to[0]}));

            if (element.has("rotation")) {
                JsonObject rotation = element.getAsJsonObject("rotation");
                if (rotation.has("origin")) {
                    double[] o = vec(rotation.getAsJsonArray("origin"));
                    rotation.add("origin", array(new double[]{16 - o[2], o[1], o[0]}));
                }
                String axis = rotation.has("axis") ? rotation.get("axis").getAsString() : "y";
                if (axis.equals("x")) {
                    rotation.addProperty("axis", "z");
                } else if (axis.equals("z")) {
                    rotation.addProperty("axis", "x");
                    if (rotation.has("angle")) rotation.addProperty("angle", -rotation.get("angle").getAsDouble());
                }
            }
        }
        return out;
    }

    private static String turn(String side) {
        for (int i = 0; i < SIDES.length; i++) {
            if (SIDES[i].equals(side)) return SIDES[(i + 1) % SIDES.length];
        }
        return side;
    }

    private static void addRotation(JsonObject face, int degrees) {
        int current = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
        face.addProperty("rotation", (current + degrees) % 360);
    }

    private static double[] defaultUv(String side, double[] f, double[] t) {
        return switch (side) {
            case "down" -> new double[]{f[0], 16 - t[2], t[0], 16 - f[2]};
            case "up" -> new double[]{f[0], f[2], t[0], t[2]};
            case "north" -> new double[]{16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]};
            case "south" -> new double[]{f[0], 16 - t[1], t[0], 16 - f[1]};
            case "west" -> new double[]{f[2], 16 - t[1], t[2], 16 - f[1]};
            default -> new double[]{16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]};
        };
    }

    private static double[] vec(JsonArray array) {
        double[] out = new double[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsDouble();
        return out;
    }

    private static JsonArray array(double[] values) {
        JsonArray out = new JsonArray();
        for (double v : values) {
            if (v == Math.rint(v)) out.add((long) v);
            else out.add(v);
        }
        return out;
    }
}
