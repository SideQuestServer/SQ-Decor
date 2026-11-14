package com.sqdecor.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModelReferencesTest {
    private static final Map<String, Path> MODELS = Res.files(Res.ASSETS + "models", ".json");
    private static final Map<String, Path> OBJS = Res.files(Res.ASSETS + "models", ".obj");
    private static final Map<String, Path> TEXTURES = Res.files(Res.ASSETS + "textures", ".png");
    private static final Map<String, Path> STATES = Res.files(Res.ASSETS + "blockstates", ".json");

    @Test
    void blockstatesPointToModels() {
        assertFalse(STATES.isEmpty());
        List<String> bad = new ArrayList<>();
        STATES.forEach((name, path) -> {
            List<String> refs = new ArrayList<>();
            Res.collect(Res.json(path), "model", refs);
            if (refs.isEmpty()) bad.add(name + ": kein Modell");
            for (String ref : refs) {
                String[] id = Res.split(ref);
                if (id[0].equals("sqdecor") && !MODELS.containsKey(id[1])) bad.add(name + " -> " + ref);
            }
        });
        assertTrue(bad.isEmpty(), "Blockstates: " + bad);
    }

    @Test
    void modelsPointToTexturesParentsAndObj() {
        assertFalse(MODELS.isEmpty());
        List<String> bad = new ArrayList<>();
        MODELS.forEach((name, path) -> {
            JsonObject model = Res.object(path);
            if (model.has("parent")) {
                String[] id = Res.split(model.get("parent").getAsString());
                if (id[0].equals("sqdecor") && !MODELS.containsKey(id[1])) bad.add(name + " parent " + model.get("parent"));
            }
            if (model.has("textures")) {
                for (Map.Entry<String, JsonElement> e : model.getAsJsonObject("textures").entrySet()) {
                    String value = e.getValue().getAsString();
                    if (value.startsWith("#")) continue;
                    String[] id = Res.split(value);
                    if (id[0].equals("sqdecor") && !TEXTURES.containsKey(id[1])) bad.add(name + " " + e.getKey() + " -> " + value);
                }
            }
            if (model.has("model") && model.get("model").isJsonPrimitive()) {
                String value = model.get("model").getAsString();
                if (value.endsWith(".obj")) {
                    String[] id = Res.split(value);
                    String rel = id[1].startsWith("models/") ? id[1].substring(7, id[1].length() - 4) : "";
                    if (!id[0].equals("sqdecor") || !OBJS.containsKey(rel)) bad.add(name + " obj " + value);
                }
            }
        });
        assertTrue(bad.isEmpty(), "Modelle: " + bad);
    }

    @Test
    void everyBlockWithItemHasItemModel() {
        Map<String, Path> items = Res.files(Res.ASSETS + "models/item", ".json");
        List<String> withoutItem = List.of("cable_clip", "shelf_top", "tape_anchor");
        List<String> bad = new ArrayList<>();
        for (String state : STATES.keySet()) {
            if (!items.containsKey(state) && !withoutItem.contains(state)) bad.add(state);
        }
        assertTrue(bad.isEmpty(), "ohne Item-Modell: " + bad);
    }
}
