package com.sqdecor.compat.cc;

import com.sqdecor.block.entity.ImageMonitorBlockEntity;

final class MonitorRaster {
    private final byte[] px;
    private final int w;
    private final int h;

    MonitorRaster(ImageMonitorBlockEntity be) {
        byte[] pixels = be.rawPixels();
        this.px = pixels == null ? new byte[0] : pixels;
        this.w = pixels == null ? 0 : be.resWidth();
        this.h = pixels == null ? 0 : be.resHeight();
    }

    MonitorRaster(byte[] pixels, int width, int height) {
        this.px = pixels;
        this.w = width;
        this.h = height;
    }

    private void set(int x, int y, int rgb) {
        int i = (y * w + x) * 3;
        px[i] = (byte) (rgb >> 16);
        px[i + 1] = (byte) (rgb >> 8);
        px[i + 2] = (byte) rgb;
    }

    void put(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < w && y < h) set(x, y, rgb);
    }

    void hspan(int y, int x1, int x2, int rgb) {
        if (y < 0 || y >= h) return;
        x1 = Math.max(0, x1);
        x2 = Math.min(w - 1, x2);
        for (int x = x1; x <= x2; x++) set(x, y, rgb);
    }

    void rect(int x, int y, int rw, int rh, int rgb) {
        for (int yy = Math.max(0, y); yy < Math.min(h, y + rh); yy++) hspan(yy, x, x + rw - 1, rgb);
    }

    void line(double x1, double y1, double x2, double y2, int rgb, int thick) {
        int t = Math.max(1, Math.min(64, thick));
        double lo = -t, hix = w + t, hiy = h + t;
        double dxd = x2 - x1, dyd = y2 - y1, u0 = 0, u1 = 1;
        double[] p = {-dxd, dxd, -dyd, dyd};
        double[] q = {x1 - lo, hix - x1, y1 - lo, hiy - y1};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) return;
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0) u0 = Math.max(u0, r);
                else u1 = Math.min(u1, r);
            }
        }
        if (u0 > u1 || Double.isNaN(u0) || Double.isNaN(u1)) return;
        double cx1 = x1 + u0 * dxd, cy1 = y1 + u0 * dyd;
        x2 = x1 + u1 * dxd;
        y2 = y1 + u1 * dyd;
        x1 = cx1;
        y1 = cy1;
        int ax = (int) Math.floor(x1 + 0.5), ay = (int) Math.floor(y1 + 0.5);
        int bx = (int) Math.floor(x2 + 0.5), by = (int) Math.floor(y2 + 0.5);
        int dx = Math.abs(bx - ax), dy = Math.abs(by - ay);
        int sx = ax < bx ? 1 : -1, sy = ay < by ? 1 : -1;
        int err = dx - dy;
        int off = t / 2;
        int guard = dx + dy + 2;
        while (guard-- > 0) {
            if (t == 1) put(ax, ay, rgb);
            else rect(ax - off, ay - off, t, t, rgb);
            if (ax == bx && ay == by) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; ax += sx; }
            if (e2 < dx) { err += dx; ay += sy; }
        }
    }

    void tri(double x1, double y1, double x2, double y2, double x3, double y3, int rgb) {
        gtri(x1, y1, rgb, x2, y2, rgb, x3, y3, rgb);
    }

    void gtri(double x1, double y1, int c1, double x2, double y2, int c2, double x3, double y3, int c3) {
        double[] xs = {x1, x2, x3};
        double[] ys = {y1, y2, y3};
        int[] cs = {c1, c2, c3};
        double minY = Math.min(y1, Math.min(y2, y3));
        double maxY = Math.max(y1, Math.max(y2, y3));
        int yStart = Math.max(0, (int) Math.ceil(minY - 0.5));
        int yEnd = Math.min(h - 1, (int) Math.floor(maxY - 0.5));
        boolean flat = c1 == c2 && c2 == c3;
        double[] ex = new double[2];
        double[][] ec = new double[2][3];
        for (int y = yStart; y <= yEnd; y++) {
            double yy = y + 0.5;
            int n = 0;
            for (int e = 0; e < 3 && n < 2; e++) {
                int i = e, j = (e + 1) % 3;
                double ya = ys[i], yb = ys[j];
                if (ya == yb) continue;
                if (yy >= Math.min(ya, yb) && yy < Math.max(ya, yb)) {
                    double t = (yy - ya) / (yb - ya);
                    ex[n] = xs[i] + (xs[j] - xs[i]) * t;
                    ec[n][0] = lerp(cs[i] >> 16 & 0xFF, cs[j] >> 16 & 0xFF, t);
                    ec[n][1] = lerp(cs[i] >> 8 & 0xFF, cs[j] >> 8 & 0xFF, t);
                    ec[n][2] = lerp(cs[i] & 0xFF, cs[j] & 0xFF, t);
                    n++;
                }
            }
            if (n < 2) continue;
            int l = ex[0] <= ex[1] ? 0 : 1;
            int r = 1 - l;
            double xl = ex[l], xr = ex[r];
            int xa = Math.max(0, (int) Math.ceil(xl - 0.5));
            int xb = Math.min(w - 1, (int) Math.ceil(xr - 0.5) - 1);
            if (xb < xa) continue;
            if (flat) {
                hspan(y, xa, xb, c1);
                continue;
            }
            double span = Math.max(1e-6, xr - xl);
            for (int x = xa; x <= xb; x++) {
                double t = Math.max(0, Math.min(1, (x + 0.5 - xl) / span));
                int rr = (int) (ec[l][0] + (ec[r][0] - ec[l][0]) * t);
                int gg = (int) (ec[l][1] + (ec[r][1] - ec[l][1]) * t);
                int bb = (int) (ec[l][2] + (ec[r][2] - ec[l][2]) * t);
                set(x, y, rr << 16 | gg << 8 | bb);
            }
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    void disc(double cx, double cy, double r, int rgb) {
        int y0 = Math.max(0, (int) Math.ceil(cy - r - 0.5));
        int y1 = Math.min(h - 1, (int) Math.floor(cy + r - 0.5));
        for (int y = y0; y <= y1; y++) {
            double dy = y + 0.5 - cy;
            double q = r * r - dy * dy;
            if (q < 0) continue;
            double dx = Math.sqrt(q);
            hspan(y, (int) Math.ceil(cx - dx - 0.5), (int) Math.ceil(cx + dx - 0.5) - 1, rgb);
        }
    }

    void arc(double cx, double cy, double ro, double ri, double a0, double a1, int rgb) {
        double twoPi = Math.PI * 2;
        double span = a1 - a0;
        boolean full = span >= twoPi - 1e-6;
        if (span <= 0) return;
        double start = ((a0 % twoPi) + twoPi) % twoPi;
        double ro2 = ro * ro, ri2 = ri * ri;
        int y0 = Math.max(0, (int) Math.ceil(cy - ro - 0.5));
        int y1 = Math.min(h - 1, (int) Math.floor(cy + ro - 0.5));
        for (int y = y0; y <= y1; y++) {
            double dy = y + 0.5 - cy;
            double q = ro2 - dy * dy;
            if (q < 0) continue;
            double half = Math.sqrt(q);
            int xa = Math.max(0, (int) Math.ceil(cx - half - 0.5));
            int xb = Math.min(w - 1, (int) Math.ceil(cx + half - 0.5) - 1);
            for (int x = xa; x <= xb; x++) {
                double dx = x + 0.5 - cx;
                double d2 = dx * dx + dy * dy;
                if (d2 < ri2) continue;
                if (!full) {
                    double a = Math.atan2(dx, -dy);
                    if (a < 0) a += twoPi;
                    double rel = a - start;
                    if (rel < 0) rel += twoPi;
                    if (rel >= span) continue;
                }
                set(x, y, rgb);
            }
        }
    }

    void grad(int x, int y, int gw, int gh, int c1, int c2, boolean vertical) {
        for (int yy = Math.max(0, y); yy < Math.min(h, y + gh); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + gw); xx++) {
                double t = vertical ? (gh <= 1 ? 0 : (yy - y) / (double) (gh - 1)) : (gw <= 1 ? 0 : (xx - x) / (double) (gw - 1));
                int rr = (int) lerp(c1 >> 16 & 0xFF, c2 >> 16 & 0xFF, t);
                int gg = (int) lerp(c1 >> 8 & 0xFF, c2 >> 8 & 0xFF, t);
                int bb = (int) lerp(c1 & 0xFF, c2 & 0xFF, t);
                set(xx, yy, rr << 16 | gg << 8 | bb);
            }
        }
    }

    void glow(double cx, double cy, double r, int rgb, double intensity) {
        int y0 = Math.max(0, (int) Math.ceil(cy - r - 0.5));
        int y1 = Math.min(h - 1, (int) Math.floor(cy + r - 0.5));
        int x0 = Math.max(0, (int) Math.ceil(cx - r - 0.5));
        int x1 = Math.min(w - 1, (int) Math.floor(cx + r - 0.5));
        int cr = rgb >> 16 & 0xFF, cg = rgb >> 8 & 0xFF, cb = rgb & 0xFF;
        for (int y = y0; y <= y1; y++) {
            double dy = y + 0.5 - cy;
            for (int x = x0; x <= x1; x++) {
                double dx = x + 0.5 - cx;
                double d = Math.sqrt(dx * dx + dy * dy) / r;
                if (d >= 1) continue;
                double f = (1 - d) * (1 - d) * intensity;
                int i = (y * w + x) * 3;
                px[i] = (byte) Math.min(255, (px[i] & 0xFF) + (int) (cr * f));
                px[i + 1] = (byte) Math.min(255, (px[i + 1] & 0xFF) + (int) (cg * f));
                px[i + 2] = (byte) Math.min(255, (px[i + 2] & 0xFF) + (int) (cb * f));
            }
        }
    }

    void dim(double f) {
        int m = (int) Math.round(Math.max(0, Math.min(1, f)) * 256);
        for (int i = 0; i < px.length; i++) px[i] = (byte) (((px[i] & 0xFF) * m) >> 8);
    }

    void afill(int x, int y, int fw, int fh, int rgb, double alpha) {
        double a = Math.max(0, Math.min(1, alpha));
        int cr = rgb >> 16 & 0xFF, cg = rgb >> 8 & 0xFF, cb = rgb & 0xFF;
        for (int yy = Math.max(0, y); yy < Math.min(h, y + fh); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + fw); xx++) {
                int i = (yy * w + xx) * 3;
                px[i] = (byte) (int) lerp(px[i] & 0xFF, cr, a);
                px[i + 1] = (byte) (int) lerp(px[i + 1] & 0xFF, cg, a);
                px[i + 2] = (byte) (int) lerp(px[i + 2] & 0xFF, cb, a);
            }
        }
    }

    void image(String name, int x, int y, int iw, int ih, double alpha) {
        int[] data = MonitorImages.scaled(name, iw, ih);
        if (data == null) return;
        double a = Math.max(0, Math.min(1, alpha));
        int m = (int) Math.round(a * 256);
        for (int yy = Math.max(0, y); yy < Math.min(h, y + ih); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + iw); xx++) {
                int c = data[(yy - y) * iw + (xx - x)];
                if (m >= 256) {
                    set(xx, yy, c);
                } else {
                    int i = (yy * w + xx) * 3;
                    px[i] = (byte) (((px[i] & 0xFF) * (256 - m) + (c >> 16 & 0xFF) * m) >> 8);
                    px[i + 1] = (byte) (((px[i + 1] & 0xFF) * (256 - m) + (c >> 8 & 0xFF) * m) >> 8);
                    px[i + 2] = (byte) (((px[i + 2] & 0xFF) * (256 - m) + (c & 0xFF) * m) >> 8);
                }
            }
        }
    }
}
