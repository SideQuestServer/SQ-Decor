package com.sqdecor.block.entity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.CommonConfig;
import com.sqdecor.block.monitor.ImageMonitorBlock;
import com.sqdecor.network.ImageMonitorPayload;
import com.sqdecor.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ImageMonitorBlockEntity extends BlockEntity {
    public static final int PIXELS_PER_BLOCK = 64;
    public static final int MAX_RESOLUTION = 512;
    private static final int VIEWER_CHECK_TICKS = 20;
    private static final int ROW_GAP = 8;

    @FunctionalInterface
    public interface TouchListener {
        void touch(String player, int x, int y);

        default void resized(int width, int height) {}
    }

    public static final int[] PPB_STEPS = {16, 32, 64, 128, 256};

    public final java.util.Map<Object, TouchListener> touchListeners = new java.util.concurrent.ConcurrentHashMap<>();

    private int ppb = PIXELS_PER_BLOCK;
    private int shapeW;
    private int shapeH;
    private int resWidth;
    private int resHeight;
    @Nullable
    private byte[] pixels;
    private boolean dirty;
    private long lastSync;
    @Nullable
    private byte[] sent;
    private final Set<UUID> viewers = new HashSet<>();

    public int version;
    public int dirtyFrom;
    public int dirtyTo = Integer.MAX_VALUE;
    public Object texture;
    public Runnable onClientRemoved;
    public ImageMonitorBlock.Screen screen;
    public long screenCheckedAt = -1;

    public ImageMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAGE_MONITOR.get(), pos, state);
    }

    @Nullable
    public byte[] pixels() {
        return pixels;
    }

    public byte[] rawPixels() {
        return pixels;
    }

    public int resWidth() {
        return resWidth;
    }

    public int resHeight() {
        return resHeight;
    }

    public int[] resolution(ImageMonitorBlock.Screen screen) {
        if (pixels != null) return new int[]{resWidth, resHeight};
        return autoResolution(screen);
    }

    public static int maxResolution() {
        return Math.min(MAX_RESOLUTION, CommonConfig.MONITOR_MAX_RESOLUTION.get());
    }

    public int[] autoResolution(ImageMonitorBlock.Screen screen) {
        int max = maxResolution();
        return new int[]{Mth.clamp(screen.width() * ppb, 1, max), Mth.clamp(screen.height() * ppb, 1, max)};
    }

    public int ppb() {
        return ppb;
    }

    public void ensure(ImageMonitorBlock.Screen screen) {
        if (pixels == null && level != null) {
            for (int i = 0; i < screen.width(); i++) {
                for (int j = 0; j < screen.height(); j++) {
                    BlockPos p = screen.origin().relative(screen.right(), i).relative(screen.up(), j);
                    if (!p.equals(worldPosition) && level.getBlockEntity(p) instanceof ImageMonitorBlockEntity other && other.pixels != null) {
                        ppb = other.ppb;
                    }
                }
            }
        }
        if (pixels == null || shapeW != screen.width() || shapeH != screen.height()) adapt(screen);
    }

    public void adapt(ImageMonitorBlock.Screen screen) {
        int[] res = autoResolution(screen);
        shapeW = screen.width();
        shapeH = screen.height();
        setResolution(res[0], res[1]);
        if (level != null && !level.isClientSide) ImageMonitorBlock.notifyResized(level, screen, res[0], res[1]);
    }

    public void cyclePpb(ImageMonitorBlock.Screen screen) {
        int next = PPB_STEPS[0];
        for (int i = 0; i < PPB_STEPS.length; i++) {
            if (PPB_STEPS[i] == ppb) {
                next = PPB_STEPS[(i + 1) % PPB_STEPS.length];
                break;
            }
        }
        ppb = next;
        adapt(screen);
    }

    public void setPpb(int ppb) {
        this.ppb = ppb;
    }

    public void setResolution(int width, int height) {
        resWidth = Mth.clamp(width, 1, maxResolution());
        resHeight = Mth.clamp(height, 1, maxResolution());
        pixels = new byte[resWidth * resHeight * 3];
        changed();
    }

    public void clear(int rgb) {
        if (pixels == null) return;
        fill(0, 0, resWidth, resHeight, rgb);
    }

    public int getPixel(int x, int y) {
        if (pixels == null || x < 0 || y < 0 || x >= resWidth || y >= resHeight) return 0;
        int i = (y * resWidth + x) * 3;
        return (pixels[i] & 0xFF) << 16 | (pixels[i + 1] & 0xFF) << 8 | (pixels[i + 2] & 0xFF);
    }

    public void put(int x, int y, int rgb) {
        if (pixels == null || x < 0 || y < 0 || x >= resWidth || y >= resHeight) return;
        int i = (y * resWidth + x) * 3;
        pixels[i] = (byte) (rgb >> 16);
        pixels[i + 1] = (byte) (rgb >> 8);
        pixels[i + 2] = (byte) rgb;
    }

    public void fill(int x, int y, int width, int height, int rgb) {
        if (pixels == null) return;
        int x1 = Math.max(0, x), y1 = Math.max(0, y);
        int x2 = Math.min(resWidth, x + width), y2 = Math.min(resHeight, y + height);
        for (int py = y1; py < y2; py++) {
            for (int px = x1; px < x2; px++) put(px, py, rgb);
        }
        changed();
    }

    public void changed() {
        dirty = true;
        setChanged();
    }

    public void serverTick(ServerLevel level) {
        if (pixels != null && level.getGameTime() % 10 == 0) {
            ImageMonitorBlock.Screen screen = ImageMonitorBlock.screen(level, worldPosition);
            if (screen != null && !screen.origin().equals(worldPosition)
                    && level.getBlockEntity(screen.origin()) instanceof ImageMonitorBlockEntity main && main.pixels == null) {
                main.ppb = ppb;
                main.adapt(screen);
                pixels = null;
                sent = null;
                viewers.clear();
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            } else if (screen != null && !screen.origin().equals(worldPosition)) {
                pixels = null;
                sent = null;
                viewers.clear();
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            } else if (screen != null && screen.origin().equals(worldPosition)) {
                if (shapeW == 0) {
                    shapeW = screen.width();
                    shapeH = screen.height();
                } else if (shapeW != screen.width() || shapeH != screen.height()
                        || resWidth > maxResolution() || resHeight > maxResolution()) {
                    adapt(screen);
                }
            }
        }
        if (!dirty && pixels != null && sent != null && Math.floorMod(level.getGameTime() + worldPosition.asLong(), VIEWER_CHECK_TICKS) == 0) {
            sendToNewViewers(level);
        }
        if (!dirty || level.getGameTime() - lastSync < CommonConfig.MONITOR_SYNC_TICKS.get()) return;
        dirty = false;
        lastSync = level.getGameTime();
        sync(level);
    }

    private List<ServerPlayer> audience(ServerLevel level) {
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false);
        int distance = CommonConfig.MONITOR_VIEW_DISTANCE.get();
        if (distance <= 0) return players;
        List<ServerPlayer> near = new ArrayList<>();
        double max = (double) distance * distance;
        for (ServerPlayer player : players) {
            if (player.distanceToSqr(worldPosition.getCenter()) <= max) near.add(player);
        }
        return near;
    }

    public boolean inView(ServerPlayer player) {
        int distance = CommonConfig.MONITOR_VIEW_DISTANCE.get();
        return distance <= 0 || player.distanceToSqr(worldPosition.getCenter()) <= (double) distance * distance;
    }

    public byte[] sentPixels() {
        return sent;
    }

    public void addViewer(ServerPlayer player) {
        if (sent != null) viewers.add(player.getUUID());
    }

    private void sendToNewViewers(ServerLevel level) {
        List<ServerPlayer> audience = audience(level);
        Set<UUID> now = new HashSet<>();
        List<ServerPlayer> fresh = new ArrayList<>();
        for (ServerPlayer player : audience) {
            now.add(player.getUUID());
            if (!viewers.contains(player.getUUID())) fresh.add(player);
        }
        viewers.retainAll(now);
        if (fresh.isEmpty()) return;
        ImageMonitorPayload.send(worldPosition, resWidth, resHeight, sent, 0, resHeight, fresh);
        for (ServerPlayer player : fresh) viewers.add(player.getUUID());
    }

    private void sync(ServerLevel level) {
        if (pixels == null) {
            sent = null;
            viewers.clear();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        List<ServerPlayer> audience = audience(level);
        if (sent == null || sent.length != pixels.length) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            ImageMonitorPayload.send(worldPosition, resWidth, resHeight, pixels, 0, resHeight, audience);
            sent = pixels.clone();
            viewers.clear();
            for (ServerPlayer player : audience) viewers.add(player.getUUID());
            return;
        }
        List<ServerPlayer> known = new ArrayList<>();
        List<ServerPlayer> fresh = new ArrayList<>();
        Set<UUID> now = new HashSet<>();
        for (ServerPlayer player : audience) {
            now.add(player.getUUID());
            (viewers.contains(player.getUUID()) ? known : fresh).add(player);
        }
        viewers.retainAll(now);
        int row = resWidth * 3;
        int y = 0;
        while (y < resHeight) {
            if (sameRow(y, row)) {
                y++;
                continue;
            }
            int start = y;
            int last = y;
            while (++y < resHeight && y - last <= ROW_GAP) {
                if (!sameRow(y, row)) last = y;
            }
            int end = last + 1;
            ImageMonitorPayload.send(worldPosition, resWidth, resHeight, pixels, start, end, known);
            System.arraycopy(pixels, start * row, sent, start * row, (end - start) * row);
            y = end;
        }
        if (!fresh.isEmpty()) {
            ImageMonitorPayload.send(worldPosition, resWidth, resHeight, pixels, 0, resHeight, fresh);
            for (ServerPlayer player : fresh) viewers.add(player.getUUID());
        }
    }

    private boolean sameRow(int y, int row) {
        int from = y * row;
        return Arrays.equals(pixels, from, from + row, sent, from, from + row);
    }

    public void receive(int width, int height, int y, byte[] data) {
        if (width < 1 || height < 1 || width > MAX_RESOLUTION || height > MAX_RESOLUTION || y < 0 || y >= height) return;
        if (pixels == null || resWidth != width || resHeight != height) {
            resWidth = width;
            resHeight = height;
            pixels = new byte[width * height * 3];
            markDirty(0, height);
        }
        int from = y * width * 3;
        int bytes = inflate(data, pixels, from, pixels.length - from);
        markDirty(y, y + (bytes + width * 3 - 1) / (width * 3));
        version++;
    }

    private void markDirty(int from, int to) {
        if (dirtyFrom >= dirtyTo) {
            dirtyFrom = from;
            dirtyTo = to;
        } else {
            dirtyFrom = Math.min(dirtyFrom, from);
            dirtyTo = Math.max(dirtyTo, to);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide && onClientRemoved != null) {
            onClientRemoved.run();
            onClientRemoved = null;
            texture = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (pixels == null) {
            tag.putInt("ppb", ppb);
            return;
        }
        tag.putInt("ppb", ppb);
        tag.putInt("shape_w", shapeW);
        tag.putInt("shape_h", shapeH);
        tag.putInt("res_w", resWidth);
        tag.putInt("res_h", resHeight);
        tag.putByteArray("pixels", deflate(pixels, 0, pixels.length));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        version++;
        markDirty(0, Integer.MAX_VALUE);
        if (tag.contains("ppb")) ppb = Mth.clamp(tag.getInt("ppb"), 1, MAX_RESOLUTION);
        shapeW = tag.getInt("shape_w");
        shapeH = tag.getInt("shape_h");
        int w = tag.getInt("res_w");
        int h = tag.getInt("res_h");
        if (tag.getBoolean("sync")) {
            if (w < 1 || h < 1 || w > MAX_RESOLUTION || h > MAX_RESOLUTION) {
                pixels = null;
            } else if (pixels == null || resWidth != w || resHeight != h) {
                resWidth = w;
                resHeight = h;
                pixels = new byte[w * h * 3];
            }
            return;
        }
        if (!tag.contains("pixels") || w < 1 || h < 1 || w > MAX_RESOLUTION || h > MAX_RESOLUTION) {
            pixels = null;
            return;
        }
        byte[] data = new byte[w * h * 3];
        inflate(tag.getByteArray("pixels"), data, 0, data.length);
        resWidth = w;
        resHeight = h;
        pixels = data;
    }

    public static byte[] deflate(byte[] data, int offset, int length) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(data, offset, length);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, length / 4));
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer));
        deflater.end();
        return out.toByteArray();
    }

    private static int inflate(byte[] data, byte[] out, int offset, int size) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        int n = 0;
        try {
            while (n < size && !inflater.finished()) {
                int read = inflater.inflate(out, offset + n, size - n);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
                n += read;
            }
        } catch (DataFormatException e) {
            Arrays.fill(out, offset, offset + size, (byte) 0);
            n = size;
        } finally {
            inflater.end();
        }
        return n;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("sync", true);
        tag.putInt("ppb", ppb);
        tag.putInt("shape_w", shapeW);
        tag.putInt("shape_h", shapeH);
        if (pixels != null) {
            tag.putInt("res_w", resWidth);
            tag.putInt("res_h", resHeight);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
