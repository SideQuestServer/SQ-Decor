package com.sqdecor.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

class DataFilesTest {
    private static final Set<String> IDS = new HashSet<>();
    private static final Set<String> TAGS = new HashSet<>();

    static {
        IDS.addAll(Res.files(Res.ASSETS + "models/item", ".json").keySet());
        IDS.addAll(Res.files(Res.ASSETS + "blockstates", ".json").keySet());
        for (String kind : List.of("block", "item")) TAGS.addAll(Res.files("data/sqdecor/tags/" + kind, ".json").keySet());
    }

    private static List<Path> data(String kind) {
        List<Path> out = new ArrayList<>();
        for (Path p : Res.all(".json")) {
            String s = p.toString().replace('\\', '/');
            if (s.contains("/data/sqdecor/" + kind + "/")) out.add(p);
        }
        return out;
    }

    @Test
    void allDataFilesAreJson() {
        List<String> bad = new ArrayList<>();
        for (Path root : Res.ROOTS) {
            Path data = root.resolve("data");
            if (!data.toFile().isDirectory()) continue;
            for (Path p : Res.walk(data)) {
                if (!p.toString().endsWith(".json")) {
                    bad.add(p + ": keine JSON-Datei");
                    continue;
                }
                try {
                    Res.json(p);
                } catch (RuntimeException e) {
                    bad.add(p + ": " + e.getMessage());
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    private static List<String> idsIn(String kind, String... keys) {
        List<Path> files = data(kind);
        assertFalse(files.isEmpty(), "keine Dateien unter " + kind);
        List<String> out = new ArrayList<>();
        for (Path p : files) {
            JsonElement json = Res.json(p);
            for (String key : keys) {
                List<String> refs = new ArrayList<>();
                Res.collect(json, key, refs);
                for (String ref : refs) {
                    if (ref.indexOf(':') > 0) out.add(p.getFileName() + " " + ref);
                }
            }
        }
        return out;
    }

    private static void checkIds(List<String> refs) {
        List<String> bad = new ArrayList<>();
        for (String entry : refs) {
            String ref = entry.substring(entry.indexOf(' ') + 1);
            String[] id = Res.split(ref);
            if (id[0].equals("sqdecor")) {
                if (!IDS.contains(id[1])) bad.add(entry);
            } else if (!id[0].equals("minecraft")) {
                bad.add(entry + " (fremder Namensraum)");
            }
        }
        assertTrue(bad.isEmpty(), "IDs: " + bad);
    }

    @Test
    void recipesUseKnownIds() {
        checkIds(idsIn("recipe", "item", "id"));
    }

    @Test
    void lootTablesUseKnownIds() {
        checkIds(idsIn("loot_table", "name"));
    }

    @Test
    void minecraftItemsExist() {
        Assumptions.assumeTrue(getClass().getClassLoader().getResource("assets/minecraft/models/item/stick.json") != null,
                "Minecraft-Ressourcen nicht im Klassenpfad");
        List<String> bad = new ArrayList<>();
        List<String> refs = new ArrayList<>(idsIn("recipe", "item", "id"));
        refs.addAll(idsIn("loot_table", "name"));
        for (String entry : refs) {
            String[] id = Res.split(entry.substring(entry.indexOf(' ') + 1));
            if (!id[0].equals("minecraft")) continue;
            if (getClass().getClassLoader().getResource("assets/minecraft/models/item/" + id[1] + ".json") == null) bad.add(entry);
        }
        assertTrue(bad.isEmpty(), "unbekannte Minecraft-IDs: " + bad);
    }

    @Test
    void tagsPointToKnownBlocksAndTags() {
        List<String> bad = new ArrayList<>();
        for (Path p : Res.all(".json")) {
            String s = p.toString().replace('\\', '/');
            if (!s.contains("/tags/")) continue;
            List<String> values = new ArrayList<>();
            for (JsonElement v : Res.object(p).getAsJsonArray("values")) {
                values.add(v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString());
            }
            for (String v : values) {
                boolean tag = v.startsWith("#");
                String[] id = Res.split(tag ? v.substring(1) : v);
                if (!id[0].equals("sqdecor")) continue;
                if (tag ? !TAGS.contains(id[1]) : !IDS.contains(id[1])) bad.add(p.getFileName() + " " + v);
            }
        }
        assertTrue(bad.isEmpty(), "Tags: " + bad);
    }
}
