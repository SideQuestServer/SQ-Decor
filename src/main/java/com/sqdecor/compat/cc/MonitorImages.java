package com.sqdecor.compat.cc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import com.sqdecor.SQDecor;
import net.neoforged.fml.ModList;

final class MonitorImages {
    private static final Map<String, String> PATHS = Map.of("logo", "/assets/sqdecor/textures/misc/sqdecor_logo.png");
    private static final Map<String, BufferedImage> LOADED = new HashMap<>();
    private static final Map<String, int[]> SCALED = new HashMap<>();

    private MonitorImages() {}

    static synchronized BufferedImage load(String name) {
        if (LOADED.containsKey(name)) return LOADED.get(name);
        BufferedImage image = null;
        String path = PATHS.get(name);
        if (path != null) {
            image = read(() -> MonitorImages.class.getResourceAsStream(path));
            if (image == null) image = read(() -> MonitorImages.class.getClassLoader().getResourceAsStream(path.substring(1)));
            if (image == null) {
                image = read(() -> {
                    var container = ModList.get().getModContainerById(SQDecor.MODID);
                    if (container.isEmpty()) return null;
                    Path file = container.get().getModInfo().getOwningFile().getFile().findResource(path.substring(1));
                    return Files.exists(file) ? Files.newInputStream(file) : null;
                });
            }
            if (image == null) SQDecor.LOGGER.warn("SQ Decor: Bild '{}' ({}) nicht gefunden, der Monitor zeichnet es nicht", name, path);
        }
        LOADED.put(name, image);
        return image;
    }

    private interface Source {
        InputStream open() throws IOException;
    }

    private static BufferedImage read(Source source) {
        try (InputStream in = source.open()) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static synchronized int[] scaled(String name, int w, int h) {
        BufferedImage image = load(name);
        if (image == null || w < 1 || h < 1 || w > 2048 || h > 2048) return null;
        String key = name + ":" + w + "x" + h;
        int[] hit = SCALED.get(key);
        if (hit != null) return hit;
        SCALED.keySet().removeIf(k -> k.startsWith(name + ":"));
        int sw = image.getWidth(), sh = image.getHeight();
        int[] src = image.getRGB(0, 0, sw, sh, null, 0, sw);
        int[] out = new int[w * h];
        for (int ty = 0; ty < h; ty++) {
            int y0 = (int) ((long) ty * sh / h);
            int y1 = Math.max(y0 + 1, (int) ((long) (ty + 1) * sh / h));
            for (int tx = 0; tx < w; tx++) {
                int x0 = (int) ((long) tx * sw / w);
                int x1 = Math.max(x0 + 1, (int) ((long) (tx + 1) * sw / w));
                long r = 0, g = 0, b = 0;
                int n = 0;
                for (int sy = y0; sy < y1; sy++) {
                    for (int sx = x0; sx < x1; sx++) {
                        int argb = src[sy * sw + sx];
                        r += argb >> 16 & 0xFF;
                        g += argb >> 8 & 0xFF;
                        b += argb & 0xFF;
                        n++;
                    }
                }
                out[ty * w + tx] = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        }
        SCALED.put(key, out);
        return out;
    }
}
