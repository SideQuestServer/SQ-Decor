package com.sqdecor.block.entity;

import com.sqdecor.block.marquee.MarqueeBlock;
import com.sqdecor.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MarqueeBlockEntity extends BlockEntity {
    public static final int MAX_TEXT = 256;
    public static final int MAX_SPEED = 20;
    public static final int DEFAULT_RGB = 0xFF8020;

    private String text = "";
    private int rgb = DEFAULT_RGB;
    private int speed = 8;
    private boolean on = true;
    private int lastSignal = -1;

    public byte[] columns;
    public String columnsText;
    public int columnsGeneration = -1;
    public int rowIndex;
    public int rowLength = 1;
    public long rowCheckedAt = -1;

    public MarqueeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARQUEE.get(), pos, state);
    }

    public String getText() {
        return text;
    }

    public int getRgb() {
        return rgb;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isOn() {
        return on;
    }

    public int getLastSignal() {
        return lastSignal;
    }

    public void setLastSignal(int signal) {
        if (lastSignal == signal) return;
        lastSignal = signal;
        setChanged();
    }

    public void set(String newText, int newRgb, int newSpeed, boolean newOn) {
        text = clean(newText);
        rgb = newRgb & 0xFFFFFF;
        speed = Mth.clamp(newSpeed, 0, MAX_SPEED);
        on = newOn;
        setChanged();
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MarqueeBlock && state.getValue(MarqueeBlock.LIT) != on) {
            level.setBlock(worldPosition, state.setValue(MarqueeBlock.LIT, on), Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public static String clean(String value) {
        String filtered = StringUtil.filterText(value == null ? "" : value);
        return filtered.length() > MAX_TEXT ? filtered.substring(0, MAX_TEXT) : filtered;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("text", text);
        tag.putInt("rgb", rgb);
        tag.putInt("speed", speed);
        tag.putBoolean("on", on);
        tag.putInt("last_signal", lastSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        text = clean(tag.getString("text"));
        rgb = tag.contains("rgb") ? tag.getInt("rgb") & 0xFFFFFF : DEFAULT_RGB;
        speed = tag.contains("speed") ? Mth.clamp(tag.getInt("speed"), 0, MAX_SPEED) : 8;
        on = !tag.contains("on") || tag.getBoolean("on");
        lastSignal = tag.contains("last_signal") ? tag.getInt("last_signal") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
