package com.sqdecor.client.render;

import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sqdecor.SQDecor;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class MarqueeFont implements ResourceManagerReloadListener {
    public static final MarqueeFont INSTANCE = new MarqueeFont();
    public static final int ROWS = 8;
    private static final int SPACE = 3;
    private static final int MAX_DEPTH = 4;

    private static Int2ObjectMap<byte[]> glyphs = new Int2ObjectOpenHashMap<>();
    private static int generation;

    private MarqueeFont() {}

    public static int generation() {
        return generation;
    }

    public static byte[] columns(String text) {
        ByteArrayList out = new ByteArrayList();
        text.codePoints().forEach(cp -> {
            if (Character.isWhitespace(cp)) {
                for (int i = 0; i < SPACE; i++) out.add((byte) 0);
                return;
            }
            byte[] glyph = glyphs.get(cp);
            if (glyph == null) glyph = glyphs.get('?');
            if (glyph == null) return;
            out.addElements(out.size(), glyph);
            out.add((byte) 0);
        });
        return out.toByteArray();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Int2ObjectMap<byte[]> map = new Int2ObjectOpenHashMap<>();
        try {
            loadFont(resources, ResourceLocation.withDefaultNamespace("default"), map, 0);
        } catch (Exception e) {
            SQDecor.LOGGER.warn("Could not read the font for the LED marquee", e);
        }
        glyphs = map;
        generation++;
    }

    private static void loadFont(ResourceManager resources, ResourceLocation id, Int2ObjectMap<byte[]> map, int depth) throws Exception {
        if (depth > MAX_DEPTH) return;
        ResourceLocation file = id.withPath(p -> "font/" + p + ".json");
        Optional<Resource> resource = resources.getResource(file);
        if (resource.isEmpty()) return;
        JsonObject json;
        try (Reader reader = resource.get().openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (JsonElement element : json.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            String type = provider.has("type") ? provider.get("type").getAsString() : "";
            if (type.equals("reference")) {
                ResourceLocation ref = ResourceLocation.tryParse(provider.get("id").getAsString());
                if (ref != null) loadFont(resources, ref, map, depth + 1);
            } else if (type.equals("bitmap")) {
                try {
                    loadBitmap(resources, provider, map);
                } catch (Exception e) {
                    SQDecor.LOGGER.debug("Skipping font bitmap {}", provider.get("file"), e);
                }
            }
        }
    }

    private static void loadBitmap(ResourceManager resources, JsonObject provider, Int2ObjectMap<byte[]> map) throws Exception {
        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
        int ascent = provider.has("ascent") ? provider.get("ascent").getAsInt() : 7;
        if (height > ROWS) return;
        ResourceLocation fileId = ResourceLocation.tryParse(provider.get("file").getAsString());
        if (fileId == null) return;
        JsonArray chars = provider.getAsJsonArray("chars");
        if (chars == null || chars.isEmpty()) return;

        Optional<Resource> resource = resources.getResource(fileId.withPrefix("textures/"));
        if (resource.isEmpty()) return;
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int rows = chars.size();
            int cols = chars.get(0).getAsString().codePointCount(0, chars.get(0).getAsString().length());
            if (rows == 0 || cols == 0) return;
            int cellW = image.getWidth() / cols;
            int cellH = image.getHeight() / rows;
            double scale = cellH / (double) height;
            int top = Math.max(0, (ROWS - 1) - ascent);

            for (int row = 0; row < rows; row++) {
                int[] codePoints = chars.get(row).getAsString().codePoints().toArray();
                for (int col = 0; col < codePoints.length && col < cols; col++) {
                    int cp = codePoints[col];
                    if (cp == 0 || cp == ' ' || map.containsKey(cp)) continue;
                    byte[] glyph = glyph(image, col * cellW, row * cellH, cellW, cellH, height, scale, top);
                    if (glyph != null) map.put(cp, glyph);
                }
            }
        }
    }

    private static byte[] glyph(NativeImage image, int x0, int y0, int cellW, int cellH, int height, double scale, int top) {
        int width = 0;
        for (int x = cellW - 1; x >= 0 && width == 0; x--) {
            for (int y = 0; y < cellH; y++) {
                if (alpha(image, x0 + x, y0 + y) > 0) {
                    width = x + 1;
                    break;
                }
            }
        }
        if (width == 0) return null;
        int dotsWide = Math.max(1, (int) Math.ceil(width / scale));
        byte[] out = new byte[dotsWide];
        for (int dx = 0; dx < dotsWide; dx++) {
            int px = x0 + Math.min(cellW - 1, (int) ((dx + 0.5) * scale));
            int bits = 0;
            for (int dy = 0; dy < height; dy++) {
                int row = top + dy;
                if (row >= ROWS) break;
                int py = y0 + Math.min(cellH - 1, (int) ((dy + 0.5) * scale));
                if (alpha(image, px, py) > 127) bits |= 1 << row;
            }
            out[dx] = (byte) bits;
        }
        return out;
    }

    private static int alpha(NativeImage image, int x, int y) {
        return (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
    }
}
