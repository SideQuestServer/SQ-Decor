package com.sqdecor.resources;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class Res {
    static final List<Path> ROOTS = List.of(Path.of("src/main/resources"), Path.of("src/generated/resources"));
    static final String ASSETS = "assets/sqdecor/";

    private Res() {}

    static Map<String, Path> files(String sub, String ext) {
        Map<String, Path> out = new TreeMap<>();
        for (Path root : ROOTS) {
            Path base = root.resolve(sub);
            if (!Files.isDirectory(base)) continue;
            for (Path p : walk(base)) {
                String rel = base.relativize(p).toString().replace('\\', '/');
                if (rel.endsWith(ext)) out.put(rel.substring(0, rel.length() - ext.length()), p);
            }
        }
        return out;
    }

    static List<Path> all(String ext) {
        List<Path> out = new ArrayList<>();
        for (Path root : ROOTS) {
            if (!Files.isDirectory(root)) continue;
            for (Path p : walk(root)) {
                if (p.toString().endsWith(ext)) out.add(p);
            }
        }
        return out;
    }

    static List<Path> walk(Path base) {
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonElement json(Path p) {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void collect(JsonElement node, String key, List<String> out) {
        if (node.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : node.getAsJsonObject().entrySet()) {
                JsonElement v = e.getValue();
                if (e.getKey().equals(key) && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) out.add(v.getAsString());
                else collect(v, key, out);
            }
        } else if (node.isJsonArray()) {
            for (JsonElement v : node.getAsJsonArray()) collect(v, key, out);
        }
    }

    static String[] split(String id) {
        int i = id.indexOf(':');
        return i < 0 ? new String[]{"minecraft", id} : new String[]{id.substring(0, i), id.substring(i + 1)};
    }

    static JsonObject object(Path p) {
        return json(p).getAsJsonObject();
    }
}
