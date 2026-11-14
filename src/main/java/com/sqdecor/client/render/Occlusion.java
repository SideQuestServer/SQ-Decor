package com.sqdecor.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.sqdecor.client.ClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Occlusion {
    private Occlusion() {}

    private static final int VISIBLE_TICKS = 4;
    private static final double SHRINK = 0.15;
    private static final int MAX_STEPS = 400;

    private static final class Entry {
        long checkedAt = Long.MIN_VALUE;
        boolean hidden;
    }

    private static final long BUDGET_NANOS = 2_000_000L;
    private static final int STARVED_MIN_AGE = 20;

    private static final Map<BlockEntity, Entry> CACHE = new WeakHashMap<>();
    private static long spent;
    private static boolean starvedLast, starvedNow;
    private static int tests, skipped, outside;
    public static int lastTests, lastSkipped, lastOutside;

    public static void endFrame() {
        lastTests = tests;
        lastSkipped = skipped;
        lastOutside = outside;
        tests = skipped = outside = 0;
        spent = 0;
        starvedLast = starvedNow;
        starvedNow = false;
    }

    public static boolean outsideView(AABB box) {
        boolean visible;
        try {
            visible = Minecraft.getInstance().levelRenderer.getFrustum().isVisible(box);
        } catch (RuntimeException e) {
            visible = true;
        }
        if (!visible) outside++;
        return !visible;
    }

    public static boolean hidden(BlockEntity be, AABB box) {
        if (!ClientConfig.OCCLUSION_CULLING.get()) return false;
        Level level = be.getLevel();
        if (level == null) return false;
        long now = level.getGameTime();
        Entry entry = CACHE.computeIfAbsent(be, key -> new Entry());
        boolean phase = Math.floorMod(now + be.getBlockPos().hashCode(), VISIBLE_TICKS) == 0;
        boolean due = entry.hidden || phase || now - entry.checkedAt >= VISIBLE_TICKS || now < entry.checkedAt;
        boolean tested = entry.checkedAt != Long.MIN_VALUE;
        if (due && starvedLast && tested && now >= entry.checkedAt && now - entry.checkedAt < STARVED_MIN_AGE) due = false;
        if (due && entry.checkedAt != now) {
            if (spent >= BUDGET_NANOS) {
                starvedNow = true;
                skipped++;
                return entry.hidden;
            }
            long t0 = System.nanoTime();
            entry.hidden = test(level, box);
            entry.checkedAt = now;
            spent += System.nanoTime() - t0;
            tests++;
        }
        return entry.hidden;
    }

    private static boolean test(Level level, AABB box) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos cameraPos = BlockPos.containing(camera);
        if (level.getBlockState(cameraPos).isSolidRender(level, cameraPos)) return false;
        if (box.contains(camera)) return false;

        Vec3 center = box.getCenter();
        if (!blocked(level, camera, center)) return false;
        double sx = box.getXsize() * (0.5 - SHRINK);
        double sy = box.getYsize() * (0.5 - SHRINK);
        double sz = box.getZsize() * (0.5 - SHRINK);
        for (int corner = 0; corner < 8; corner++) {
            Vec3 point = center.add((corner & 1) == 0 ? -sx : sx, (corner & 2) == 0 ? -sy : sy, (corner & 4) == 0 ? -sz : sz);
            if (!blocked(level, camera, point)) return false;
        }
        return true;
    }

    private static boolean blocked(Level level, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        int x = Mth.floor(from.x);
        int y = Mth.floor(from.y);
        int z = Mth.floor(from.z);
        int endX = Mth.floor(to.x);
        int endY = Mth.floor(to.y);
        int endZ = Mth.floor(to.z);
        int stepX = dx > 0 ? 1 : dx < 0 ? -1 : 0;
        int stepY = dy > 0 ? 1 : dy < 0 ? -1 : 0;
        int stepZ = dz > 0 ? 1 : dz < 0 ? -1 : 0;
        double deltaX = stepX != 0 ? 1 / Math.abs(dx) : Double.MAX_VALUE;
        double deltaY = stepY != 0 ? 1 / Math.abs(dy) : Double.MAX_VALUE;
        double deltaZ = stepZ != 0 ? 1 / Math.abs(dz) : Double.MAX_VALUE;
        double nextX = stepX > 0 ? (x + 1 - from.x) * deltaX : stepX < 0 ? (from.x - x) * deltaX : Double.MAX_VALUE;
        double nextY = stepY > 0 ? (y + 1 - from.y) * deltaY : stepY < 0 ? (from.y - y) * deltaY : Double.MAX_VALUE;
        double nextZ = stepZ > 0 ? (z + 1 - from.z) * deltaZ : stepZ < 0 ? (from.z - z) * deltaZ : Double.MAX_VALUE;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < MAX_STEPS; i++) {
            if (nextX <= nextY && nextX <= nextZ) {
                if (nextX > 1) return false;
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                if (nextY > 1) return false;
                y += stepY;
                nextY += deltaY;
            } else {
                if (nextZ > 1) return false;
                z += stepZ;
                nextZ += deltaZ;
            }
            if (x == endX && y == endY && z == endZ) return false;
            pos.set(x, y, z);
            if (level.getBlockState(pos).isSolidRender(level, pos)) return true;
        }
        return false;
    }
}
