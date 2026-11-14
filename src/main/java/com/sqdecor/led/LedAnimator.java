package com.sqdecor.led;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.led.LedBlock;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class LedAnimator {
    private LedAnimator() {}

    public enum Type { RAINBOW, CHASE, FADE, PULSE, BLINK }

    private static final int MAX_ANIMATIONS = 64;
    private static final int INTERVAL = 2;
    private static final int RAINBOW_INTERVAL = 5;
    private static final int HUE_STEPS = 64;
    private static final Map<String, Anim> ANIMS = new HashMap<>();

    private static final class Anim {
        final ServerLevel level;
        final List<BlockPos> leds;
        final Type type;
        final double speed;
        final int rgb1;
        final int rgb2;
        final int baseLevel;
        final long duration;
        long ticks;
        int lastHead = -1;

        Anim(ServerLevel level, List<BlockPos> leds, Type type, double speed, int rgb1, int rgb2, int baseLevel, long duration) {
            this.level = level;
            this.leds = leds;
            this.type = type;
            this.speed = speed;
            this.rgb1 = rgb1;
            this.rgb2 = rgb2;
            this.baseLevel = baseLevel;
            this.duration = Math.max(1, duration);
        }
    }

    private static String key(Level level, List<BlockPos> leds) {
        return level.dimension().location() + "@" + leds.get(0).asLong();
    }

    public static void start(Level level, List<BlockPos> leds, Type type, double speed, int rgb1, int rgb2,
                             int baseLevel, int durationTicks) {
        if (!(level instanceof ServerLevel serverLevel) || leds.isEmpty()) return;
        String k = key(level, leds);
        if (!ANIMS.containsKey(k) && ANIMS.size() >= MAX_ANIMATIONS) return;
        ANIMS.put(k, new Anim(serverLevel, List.copyOf(leds), type, speed, rgb1 & 0xFFFFFF, rgb2 & 0xFFFFFF,
                Mth.clamp(baseLevel, 0, 15), durationTicks));
    }

    public static void stop(Level level, List<BlockPos> leds) {
        if (leds.isEmpty()) return;
        ANIMS.remove(key(level, leds));
    }

    public static void stopAt(Level level, BlockPos pos) {
        if (ANIMS.isEmpty()) return;
        ANIMS.values().removeIf(a -> a.level == level && a.leds.contains(pos));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ANIMS.isEmpty()) return;
        Iterator<Anim> it = ANIMS.values().iterator();
        while (it.hasNext()) {
            Anim a = it.next();
            a.ticks++;
            if (a.ticks % (a.type == Type.RAINBOW ? RAINBOW_INTERVAL : INTERVAL) != 0) continue;
            if (!step(a)) it.remove();
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ANIMS.clear();
    }

    private static boolean valid(Anim a, BlockPos pos) {
        return a.level.isLoaded(pos) && a.level.getBlockState(pos).getBlock() instanceof LedBlock;
    }

    private static void setRgb(Anim a, BlockPos pos, int rgb) {
        if (a.level.getBlockEntity(pos) instanceof LedBlockEntity be) be.setRgb(rgb);
    }

    private static int lerp(int c1, int c2, double t) {
        int r = (int) Math.round(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) Math.round((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static boolean step(Anim a) {
        int n = a.leds.size();
        boolean any = false;

        switch (a.type) {
            case RAINBOW -> {
                double shift = a.ticks / 20.0 * a.speed;
                for (int i = 0; i < n; i++) {
                    BlockPos p = a.leds.get(i);
                    if (!valid(a, p)) continue;
                    any = true;
                    float hue = (float) (((i / (double) n) + shift) % 1.0);
                    hue = Math.round(hue * HUE_STEPS) / (float) HUE_STEPS;
                    setRgb(a, p, Mth.hsvToRgb(hue, 1f, 1f));
                }
            }
            case CHASE -> {
                int head = (int) (Math.floor(a.ticks / 20.0 * a.speed) % n);
                if (head < 0) head = 0;
                if (a.lastHead == -1) {
                    for (BlockPos p : a.leds) {
                        if (!valid(a, p)) continue;
                        any = true;
                        setRgb(a, p, a.rgb1);
                        LedLogic.applyLevel(a.level, p, a.baseLevel, false);
                    }
                }
                if (head != a.lastHead) {
                    if (a.lastHead >= 0 && valid(a, a.leds.get(a.lastHead))) {
                        LedLogic.applyLevel(a.level, a.leds.get(a.lastHead), a.baseLevel, false);
                    }
                    BlockPos hp = a.leds.get(head);
                    if (valid(a, hp)) {
                        LedLogic.applyLevel(a.level, hp, 15, false);
                    }
                    a.lastHead = head;
                }
                for (BlockPos p : a.leds) {
                    if (valid(a, p)) { any = true; break; }
                }
            }
            case PULSE -> {
                double phase = (a.ticks / 20.0 * a.speed) % 1.0;
                int lvl = (int) Math.round(a.baseLevel * (1 - Math.abs(2 * phase - 1)));
                for (BlockPos p : a.leds) {
                    if (!valid(a, p)) continue;
                    any = true;
                    setRgb(a, p, a.rgb1);
                    LedLogic.applyLevel(a.level, p, lvl, false);
                }
            }
            case BLINK -> {
                boolean on = (int) Math.floor(a.ticks / 20.0 * a.speed * 2) % 2 == 0;
                int lvl = on ? a.baseLevel : 0;
                for (BlockPos p : a.leds) {
                    if (!valid(a, p)) continue;
                    any = true;
                    setRgb(a, p, a.rgb1);
                    LedLogic.applyLevel(a.level, p, lvl, false);
                }
            }
            case FADE -> {
                double t = Math.min(1.0, a.ticks / (double) a.duration);
                int color = lerp(a.rgb1, a.rgb2, t);
                for (BlockPos p : a.leds) {
                    if (!valid(a, p)) continue;
                    any = true;
                    setRgb(a, p, color);
                }
                if (t >= 1.0) return false;
            }
        }
        return any;
    }
}
