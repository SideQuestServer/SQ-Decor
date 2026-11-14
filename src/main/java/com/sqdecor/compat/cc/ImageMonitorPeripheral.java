package com.sqdecor.compat.cc;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sqdecor.block.entity.ImageMonitorBlockEntity;
import com.sqdecor.block.monitor.ImageMonitorBlock;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ImageMonitorPeripheral implements IPeripheral {
    private static final int MAX_FILE = 8 * 1024 * 1024;
    private static final int MAX_IMAGE = 4096;

    private final BlockEntity be;

    public ImageMonitorPeripheral(BlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "sqdecor_monitor";
    }

    @Override
    public void attach(IComputerAccess computer) {
        if (be instanceof ImageMonitorBlockEntity monitor) {
            monitor.touchListeners.put(computer, new ImageMonitorBlockEntity.TouchListener() {
                @Override
                public void touch(String player, int x, int y) {
                    computer.queueEvent("sqdecor_touch", computer.getAttachmentName(), x, y, player);
                }

                @Override
                public void resized(int width, int height) {
                    computer.queueEvent("sqdecor_resize", computer.getAttachmentName(), width, height);
                }
            });
        }
    }

    @Override
    public void detach(IComputerAccess computer) {
        if (be instanceof ImageMonitorBlockEntity monitor) monitor.touchListeners.remove(computer);
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof ImageMonitorPeripheral p && p.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    private ImageMonitorBlock.Screen screen() throws LuaException {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) throw new LuaException("not loaded");
        ImageMonitorBlock.Screen screen = ImageMonitorBlock.screen(level, be.getBlockPos());
        if (screen == null) throw new LuaException("not loaded");
        return screen;
    }

    private ImageMonitorBlockEntity main() throws LuaException {
        ImageMonitorBlock.Screen screen = screen();
        if (!(be.getLevel().getBlockEntity(screen.origin()) instanceof ImageMonitorBlockEntity main)) {
            throw new LuaException("not loaded");
        }
        main.ensure(screen);
        return main;
    }

    @LuaFunction(mainThread = true)
    public final Object[] getSize() throws LuaException {
        ImageMonitorBlock.Screen screen = screen();
        return new Object[]{screen.width(), screen.height()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getResolution() throws LuaException {
        ImageMonitorBlockEntity main = main();
        return new Object[]{main.resWidth(), main.resHeight()};
    }

    @LuaFunction(mainThread = true)
    public final void setResolution(int width, int height) throws LuaException {
        int max = ImageMonitorBlockEntity.maxResolution();
        if (width < 1 || height < 1 || width > max || height > max) throw new LuaException("resolution must be 1-" + max);
        main().setResolution(width, height);
    }

    @LuaFunction(mainThread = true)
    public final void clear(IArguments args) throws LuaException {
        int rgb = args.count() > 0 ? LedPeripheral.color(args, 0) : 0;
        main().clear(rgb);
    }

    @LuaFunction(mainThread = true)
    public final void setPixel(IArguments args) throws LuaException {
        ImageMonitorBlockEntity main = main();
        main.put(args.getInt(0) - 1, args.getInt(1) - 1, LedPeripheral.color(args, 2));
        main.changed();
    }

    @LuaFunction(mainThread = true)
    public final int getPixel(int x, int y) throws LuaException {
        return main().getPixel(x - 1, y - 1);
    }

    @LuaFunction(mainThread = true)
    public final void fill(IArguments args) throws LuaException {
        main().fill(args.getInt(0) - 1, args.getInt(1) - 1, args.getInt(2), args.getInt(3), LedPeripheral.color(args, 4));
    }

    @LuaFunction(mainThread = true)
    public final void drawPixels(IArguments args) throws LuaException {
        ImageMonitorBlockEntity main = main();
        int x = args.getInt(0) - 1;
        int y = args.getInt(1) - 1;
        Map<?, ?> rows = args.getTable(2);
        for (Map.Entry<?, ?> row : rows.entrySet()) {
            if (!(row.getKey() instanceof Number r) || !(row.getValue() instanceof Map<?, ?> cells)) continue;
            for (Map.Entry<?, ?> cell : cells.entrySet()) {
                if (cell.getKey() instanceof Number c && cell.getValue() instanceof Number color) {
                    main.put(x + c.intValue() - 1, y + r.intValue() - 1, color.intValue() & 0xFFFFFF);
                }
            }
        }
        main.changed();
    }

    @LuaFunction(mainThread = true)
    public final void drawImage(IArguments args) throws LuaException {
        ImageMonitorBlockEntity main = main();
        BufferedImage image = decode(args.getBytes(0));
        int x, y, w, h;
        if (args.count() > 1 && args.get(1) instanceof String mode) {
            if (!"fit".equals(mode)) throw new LuaException("unknown mode '" + mode + "', use \"fit\" or x, y, w, h");
            double scale = Math.min(main.resWidth() / (double) image.getWidth(), main.resHeight() / (double) image.getHeight());
            w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            x = (main.resWidth() - w) / 2;
            y = (main.resHeight() - h) / 2;
        } else if (args.count() > 1) {
            x = args.getInt(1) - 1;
            y = args.getInt(2) - 1;
            w = args.getInt(3);
            h = args.getInt(4);
            if (w < 1 || h < 1) throw new LuaException("size must be at least 1");
        } else {
            x = 0;
            y = 0;
            w = main.resWidth();
            h = main.resHeight();
        }
        w = Math.min(w, ImageMonitorBlockEntity.MAX_RESOLUTION * 2);
        h = Math.min(h, ImageMonitorBlockEntity.MAX_RESOLUTION * 2);

        int[] rgb = scale(image, w, h);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) main.put(x + px, y + py, rgb[py * w + px]);
        }
        main.changed();
    }

    @LuaFunction(mainThread = true)
    public final Object[] drawText(IArguments args) throws LuaException {
        ImageMonitorBlockEntity main = main();
        int size = args.optInt(4, 8);
        int[] wh = drawTextAt(main, args.getInt(0) - 1, args.getInt(1) - 1, args.getString(2), LedPeripheral.color(args, 3), size);
        main.changed();
        return new Object[]{wh[0], wh[1]};
    }

    private record Glyphs(int w, int h, int[] alpha) {}

    private static final java.util.Map<String, Glyphs> GLYPH_CACHE = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Glyphs> eldest) {
            return size() > 256;
        }
    };

    private static Glyphs glyphs(String text, int size) {
        String key = size + ":" + text;
        synchronized (GLYPH_CACHE) {
            Glyphs hit = GLYPH_CACHE.get(key);
            if (hit != null) return hit;
        }
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        FontMetrics metrics = pg.getFontMetrics();
        int w = Math.max(1, Math.min(4096, metrics.stringWidth(text)));
        int h = Math.max(1, metrics.getAscent() + metrics.getDescent());
        int ascent = metrics.getAscent();
        pg.dispose();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setFont(font);
        Object aa = size >= 16 ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
        g.setColor(Color.WHITE);
        g.drawString(text, 0, ascent);
        g.dispose();
        int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < argb.length; i++) argb[i] >>>= 24;
        Glyphs made = new Glyphs(w, h, argb);
        if (w * h <= 16384) {
            synchronized (GLYPH_CACHE) {
                GLYPH_CACHE.put(key, made);
            }
        }
        return made;
    }

    private static int[] drawTextAt(ImageMonitorBlockEntity main, int x, int y, String text, int rgb, int size) throws LuaException {
        if (size < 4 || size > 128) throw new LuaException("size must be 4-128");
        if (text.length() > 512) text = text.substring(0, 512);
        Glyphs gl = glyphs(text, size);
        int w = gl.w(), h = gl.h();
        if (main == null) return new int[]{w, h};

        int[] alpha = gl.alpha();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int a = alpha[py * w + px];
                if (a == 0) continue;
                int tx = x + px, ty = y + py;
                main.put(tx, ty, a == 255 ? rgb : blend(main.getPixel(tx, ty), rgb, a));
            }
        }
        return new int[]{w, h};
    }

    @LuaFunction(mainThread = true)
    public final Object[] measureText(String text, java.util.Optional<Integer> size) throws LuaException {
        int[] wh = drawTextAt(null, 0, 0, text, 0, size.orElse(8));
        return new Object[]{wh[0], wh[1]};
    }

    @LuaFunction(mainThread = true)
    public final void batch(Map<?, ?> commands) throws LuaException {
        ImageMonitorBlockEntity main = main();
        int count = commands.size();
        MonitorRaster r = new MonitorRaster(main);
        for (int n = 1; n <= count; n++) {
            Object entry = commands.get((double) n);
            if (!(entry instanceof Map<?, ?> c)) throw new LuaException("batch entry " + n + " is not a table");
            Object name = c.get(1.0);
            if (!(name instanceof String op)) throw new LuaException("batch entry " + n + ": command name missing");
            switch (op) {
                case "clear" -> main.clear(c.containsKey(2.0) ? LedPeripheral.color(c.get(2.0), 1) : 0);
                case "fill" -> r.rect(num(c, 2, n) - 1, num(c, 3, n) - 1, num(c, 4, n), num(c, 5, n), LedPeripheral.color(c.get(6.0), 5));
                case "pixel" -> r.put(num(c, 2, n) - 1, num(c, 3, n) - 1, LedPeripheral.color(c.get(4.0), 3));
                case "line" -> r.line(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, dbl(c, 4, n) - 0.5, dbl(c, 5, n) - 0.5,
                        LedPeripheral.color(c.get(6.0), 6), c.get(7.0) instanceof Number s ? s.intValue() : 1);
                case "tri" -> r.tri(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, dbl(c, 4, n) - 0.5, dbl(c, 5, n) - 0.5,
                        dbl(c, 6, n) - 0.5, dbl(c, 7, n) - 0.5, LedPeripheral.color(c.get(8.0), 8));
                case "gtri" -> r.gtri(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, LedPeripheral.color(c.get(4.0), 4),
                        dbl(c, 5, n) - 0.5, dbl(c, 6, n) - 0.5, LedPeripheral.color(c.get(7.0), 7),
                        dbl(c, 8, n) - 0.5, dbl(c, 9, n) - 0.5, LedPeripheral.color(c.get(10.0), 10));
                case "disc" -> r.disc(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, dbl(c, 4, n), LedPeripheral.color(c.get(5.0), 5));
                case "arc" -> r.arc(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, dbl(c, 4, n), dbl(c, 5, n), dbl(c, 6, n), dbl(c, 7, n),
                        LedPeripheral.color(c.get(8.0), 8));
                case "grad" -> r.grad(num(c, 2, n) - 1, num(c, 3, n) - 1, num(c, 4, n), num(c, 5, n),
                        LedPeripheral.color(c.get(6.0), 6), LedPeripheral.color(c.get(7.0), 7), !"h".equals(c.get(8.0)));
                case "glow" -> r.glow(dbl(c, 2, n) - 0.5, dbl(c, 3, n) - 0.5, dbl(c, 4, n), LedPeripheral.color(c.get(5.0), 5),
                        c.get(6.0) instanceof Number s ? s.doubleValue() : 1.0);
                case "image" -> r.image(imageName(c, n), num(c, 3, n) - 1, num(c, 4, n) - 1, num(c, 5, n), num(c, 6, n),
                        c.get(7.0) instanceof Number s ? s.doubleValue() : 1.0);
                case "dim" -> r.dim(dbl(c, 2, n));
                case "afill" -> r.afill(num(c, 2, n) - 1, num(c, 3, n) - 1, num(c, 4, n), num(c, 5, n),
                        LedPeripheral.color(c.get(6.0), 6), dbl(c, 7, n));
                case "text" -> drawTextAt(main, num(c, 2, n) - 1, num(c, 3, n) - 1, str(c, 4, n), LedPeripheral.color(c.get(5.0), 4),
                        c.get(6.0) instanceof Number s ? s.intValue() : 8);
                case "ctext" -> {
                    int size = c.get(8.0) instanceof Number s ? s.intValue() : 8;
                    String text = str(c, 6, n);
                    int[] wh = drawTextAt(null, 0, 0, text, 0, size);
                    int x = num(c, 2, n) - 1 + (num(c, 4, n) - wh[0]) / 2;
                    int y = num(c, 3, n) - 1 + (num(c, 5, n) - wh[1]) / 2;
                    drawTextAt(main, x, y, text, LedPeripheral.color(c.get(7.0), 6), size);
                }
                default -> throw new LuaException("batch entry " + n + ": unknown command '" + op + "'");
            }
        }
        main.changed();
    }

    private static int num(Map<?, ?> c, int i, int entry) throws LuaException {
        if (c.get((double) i) instanceof Number n) return n.intValue();
        throw new LuaException("batch entry " + entry + ": number expected at position " + i);
    }

    private static double dbl(Map<?, ?> c, int i, int entry) throws LuaException {
        if (c.get((double) i) instanceof Number n) return n.doubleValue();
        throw new LuaException("batch entry " + entry + ": number expected at position " + i);
    }

    private static String imageName(Map<?, ?> c, int entry) throws LuaException {
        String name = str(c, 2, entry);
        if (MonitorImages.load(name) == null) throw new LuaException("batch entry " + entry + ": image '" + name + "' not found in the mod");
        return name;
    }

    private static String str(Map<?, ?> c, int i, int entry) throws LuaException {
        if (c.get((double) i) instanceof String s) return s;
        throw new LuaException("batch entry " + entry + ": text expected at position " + i);
    }

    private static int blend(int under, int over, int a) {
        int r = ((over >> 16 & 0xFF) * a + (under >> 16 & 0xFF) * (255 - a)) / 255;
        int g = ((over >> 8 & 0xFF) * a + (under >> 8 & 0xFF) * (255 - a)) / 255;
        int b = ((over & 0xFF) * a + (under & 0xFF) * (255 - a)) / 255;
        return r << 16 | g << 8 | b;
    }

    private static int[] scale(BufferedImage image, int w, int h) {
        int sw = image.getWidth();
        int sh = image.getHeight();
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
                        int a = argb >>> 24;
                        r += ((argb >> 16) & 0xFF) * a / 255;
                        g += ((argb >> 8) & 0xFF) * a / 255;
                        b += (argb & 0xFF) * a / 255;
                        n++;
                    }
                }
                out[ty * w + tx] = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        }
        return out;
    }

    private static BufferedImage decode(ByteBuffer buffer) throws LuaException {
        if (buffer.remaining() > MAX_FILE) throw new LuaException("image file too big (max 8 MB)");
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) throw new LuaException("not an image (PNG, JPG, GIF or BMP)");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                if (reader.getWidth(0) > MAX_IMAGE || reader.getHeight(0) > MAX_IMAGE) {
                    throw new LuaException("image too big (max " + MAX_IMAGE + " x " + MAX_IMAGE + ")");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new LuaException("could not read image: " + e.getMessage());
        }
    }
}
