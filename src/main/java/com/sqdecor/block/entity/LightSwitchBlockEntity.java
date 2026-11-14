package com.sqdecor.block.entity;

import com.sqdecor.block.switches.LightSwitchBlock;
import com.sqdecor.led.ChannelMember;
import com.sqdecor.led.DeviceSetting;
import com.sqdecor.led.LightChannels;
import com.sqdecor.registry.ModBlockEntities;
import com.sqdecor.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LightSwitchBlockEntity extends BlockEntity implements ChannelMember {
    private int channel;
    private final int[] settings = new int[DeviceSetting.values().length];

    public boolean active;
    public long lastSeen;
    public int lastWanted = -1;

    public LightSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHT_SWITCH.get(), pos, state);
        for (DeviceSetting setting : DeviceSetting.values()) settings[setting.ordinal()] = setting.defaultValue;
    }

    public int get(DeviceSetting setting) {
        return settings[setting.ordinal()];
    }

    public void set(DeviceSetting setting, int value) {
        settings[setting.ordinal()] = setting.clamp(value);
        lastWanted = -1;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public int getChannel() {
        return channel;
    }

    public void setChannel(int value) {
        if (channel == value) return;
        if (level != null) LightChannels.untrack(level, worldPosition, channel);
        channel = value;
        if (level != null) LightChannels.track(level, worldPosition, channel);
        setChanged();
    }

    @Override
    public void channelChanged(boolean on) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof LightSwitchBlock && state.getValue(LightSwitchBlock.POWERED) != on) {
            level.setBlock(worldPosition, state.setValue(LightSwitchBlock.POWERED, on), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) LightChannels.track(level, worldPosition, channel);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) LightChannels.untrack(level, worldPosition, channel);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (channel != 0) components.set(ModDataComponents.CHANNEL.get(), channel);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        Integer value = input.get(ModDataComponents.CHANNEL.get());
        if (value != null) setChannel(value);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("channel");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("channel", channel);
        tag.putIntArray("settings", settings);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int oldChannel = channel;
        channel = tag.getInt("channel");
        int[] saved = tag.getIntArray("settings");
        DeviceSetting[] all = DeviceSetting.values();
        for (int i = 0; i < all.length; i++) settings[i] = i < saved.length ? all[i].clamp(saved[i]) : all[i].defaultValue;
        if (level != null && oldChannel != channel) {
            LightChannels.untrack(level, worldPosition, oldChannel);
            LightChannels.track(level, worldPosition, channel);
        }
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
