package com.sqdecor.block.lamp;

import java.util.ArrayList;
import java.util.List;

public enum StripLayout {
    STRIP {
        @Override
        List<double[]> build() {
            List<double[]> list = new ArrayList<>();
            for (int i = 0; i < 8; i++) list.add(seg(-8 + 0.3 + 2 * i, -1.8, 1.4, 3.6, 3));
            return list;
        }
    },
    THIN {
        @Override
        List<double[]> build() {
            List<double[]> list = new ArrayList<>();
            for (int i = 0; i < 16; i++) list.add(seg(-8 + 0.15 + i, -0.8, 0.7, 1.6, 2));
            return list;
        }
    },
    DOUBLE {
        @Override
        List<double[]> build() {
            List<double[]> list = new ArrayList<>();
            for (int i = 0; i < 8; i++) list.add(seg(-8 + 0.3 + 2 * i, -3.9, 1.4, 2.4, 3));
            for (int i = 7; i >= 0; i--) list.add(seg(-8 + 0.3 + 2 * i, 1.5, 1.4, 2.4, 3));
            return list;
        }
    },
    CORNER {
        @Override
        List<double[]> build() {
            List<double[]> list = new ArrayList<>();
            for (int i = 0; i < 6; i++) list.add(seg(-8 + 0.3 + 2 * i, 4.3, 1.4, 3.4, 3));
            list.add(seg(4.3, 4.3, 3.4, 3.4, 3));
            for (int j = 0; j < 6; j++) {
                double z1 = 3.7 - 2 * j;
                list.add(new double[]{4.3, 2.6, z1 - 1.4, 7.7, 3.25, z1});
            }
            return list;
        }
    },
    RING {
        @Override
        List<double[]> build() {
            List<double[]> list = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30 - 90);
                double cx = 6 * Math.cos(a), cz = 6 * Math.sin(a);
                list.add(new double[]{cx - 0.9, 2.6, cz - 0.9, cx + 0.9, 3.25, cz + 0.9});
            }
            return list;
        }
    };

    private List<double[]> segments;

    abstract List<double[]> build();

    public List<double[]> segments() {
        if (segments == null) segments = List.copyOf(build());
        return segments;
    }

    private static double[] seg(double x0, double z0, double w, double d, double top) {
        return new double[]{x0, top - 0.4, z0, x0 + w, top + 0.25, z0 + d};
    }

    public static StripLayout byName(String name) {
        return valueOf(name.toUpperCase(java.util.Locale.ROOT));
    }
}
