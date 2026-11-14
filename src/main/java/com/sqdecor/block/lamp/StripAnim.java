package com.sqdecor.block.lamp;

import net.minecraft.util.Mth;

public enum StripAnim {
    STATIC, CHASE, BOUNCE, PULSE, ALTERNATE, FILL, TWINKLE, WAVE, RAINBOW;

    private static final double RATE = 8;

    public StripAnim next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static StripAnim byIndex(int index) {
        return values()[Mth.clamp(index, 0, values().length - 1)];
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static double speed(int step) {
        return switch (step) {
            case 0 -> 0.5;
            case 2 -> 2;
            case 3 -> 4;
            default -> 1;
        };
    }

    public float level(int i, int n, double t, long seed) {
        switch (this) {
            case CHASE: {
                double head = (t * RATE) % n;
                double d = ((head - i) % n + n) % n;
                return (float) Math.max(0, 1 - d / Math.min(4, n));
            }
            case BOUNCE: {
                double span = Math.max(1, n - 1);
                double p = (t * RATE) % (2 * span);
                double head = p <= span ? p : 2 * span - p;
                return (float) Math.max(0, 1 - Math.abs(head - i) / 2.5);
            }
            case PULSE:
                return (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 1.2));
            case ALTERNATE:
                return (i + (long) Math.floor(t * 2)) % 2 == 0 ? 1f : 0f;
            case FILL: {
                double total = n + 4;
                double p = (t * RATE * 0.6) % total;
                return i < p ? 1f : 0f;
            }
            case TWINKLE: {
                long k = (long) Math.floor(t * 6);
                long h = (i * 0x9E3779B97F4A7C15L) ^ (k * 0xC2B2AE3D27D4EB4FL) ^ seed;
                h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
                return (h & 7) < 3 ? 1f : 0.05f;
            }
            case WAVE:
                return (float) (0.5 + 0.5 * Math.sin(t * 6 - i * 2 * Math.PI / Math.min(n, 8)));
            default:
                return 1f;
        }
    }

    public float hue(int i, int n, double t) {
        if (this != RAINBOW) return -1f;
        return (float) (((t * 0.35 - (double) i / n) % 1 + 1) % 1);
    }
}
