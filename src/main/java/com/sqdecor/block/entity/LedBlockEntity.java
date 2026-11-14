package com.sqdecor.block.entity;

import com.sqdecor.CommonConfig;
import com.sqdecor.led.AutoMode;
import com.sqdecor.led.ChannelMember;
import com.sqdecor.led.LedAnimator;
import com.sqdecor.led.LedLogic;
import com.sqdecor.led.LedLook;
import com.sqdecor.led.LightChannels;
import com.sqdecor.led.RedstoneMode;
import com.sqdecor.registry.ModBlockEntities;
import com.sqdecor.registry.ModDataComponents;
import com.sqdecor.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LedBlockEntity extends BlockEntity implements ChannelMember {
    public static final int DEFAULT_RGB = DyeColor.WHITE.getTextureDiffuseColor() & 0xFFFFFF;

    private int rgb = DEFAULT_RGB;
    private RedstoneMode redstoneMode = RedstoneMode.SWITCH;
    private int lastSignal = -1;
    private int onLevel = 8;
    private AutoMode autoMode = AutoMode.OFF;
    private int autoRange = AutoMode.DEFAULT_RANGE;
    private int autoState = -1;
    private int fadeTarget = -1;
    private int lightMode = -1;
    private boolean lightTinted = true;
    private int channel;
    private int channelState = -1;

    public LedBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LED.get(), pos, state);
    }

    protected LedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getRgb() {
        return rgb;
    }

    public void setRgb(int value) {
        value &= 0xFFFFFF;
        if (rgb == value) return;
        rgb = value;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (redstoneMode == mode) return;
        redstoneMode = mode;
        lastSignal = -1;
        setChanged();
    }

    public int getLastSignal() {
        return lastSignal;
    }

    public void setLastSignal(int signal) {
        if (lastSignal == signal) return;
        lastSignal = signal;
        setChanged();
    }

    public int getOnLevel() {
        return onLevel;
    }

    public void setOnLevel(int value) {
        value = Mth.clamp(value, 1, 15);
        if (onLevel == value) return;
        onLevel = value;
        setChanged();
    }

    public AutoMode getAutoMode() {
        return autoMode;
    }

    public void setAutoMode(AutoMode mode) {
        autoMode = mode;
        autoState = -1;
        setChanged();
    }

    public int getAutoRange() {
        return autoRange;
    }

    public void setAutoRange(int value) {
        value = Mth.clamp(value, AutoMode.MIN_RANGE, AutoMode.MAX_RANGE);
        if (autoRange == value) return;
        autoRange = value;
        autoState = -1;
        setChanged();
    }

    public int getLightMode() {
        return lightMode;
    }

    public void setLightMode(int mode) {
        mode = Mth.clamp(mode, -1, 2);
        if (lightMode == mode) return;
        lightMode = mode;
        changed();
    }

    public volatile boolean hot;
    public long lastColorNanos;
    public long coolingUntil;
    public static final long HOT_GAP_NANOS = 1_500_000_000L;
    public static java.util.function.Consumer<LedBlockEntity> clientLoaded = be -> {};
    public static java.util.function.Predicate<LedBlockEntity> hotAllowed = be -> false;

    public net.minecraft.world.phys.AABB renderBox;
    public BlockState renderBoxState;
    public int renderBoxMode;
    public int renderBoxChain;

    public int effectiveLightMode() {
        if (lightMode >= 0) return lightMode;
        if (getBlockState().getBlock() instanceof com.sqdecor.block.lamp.GlowBlock) return 0;
        return getBlockState().getBlock() instanceof com.sqdecor.block.lamp.SpotlightBlock ? 2 : 1;
    }

    public boolean isLightTinted() {
        return lightTinted;
    }

    public void setLightTinted(boolean tinted) {
        if (lightTinted == tinted) return;
        lightTinted = tinted;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void fadeTo(int target) {
        fadeTarget = Mth.clamp(target, 0, 15);
        setChanged();
    }

    public void stopFade() {
        fadeTarget = -1;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ModSounds.playLoop(level, pos, state);
        if (fadeTarget >= 0) {
            int current = state.getValue(LedLogic.LIGHT_LEVEL);
            int next = current + Integer.signum(fadeTarget - current);
            if (next == fadeTarget) fadeTarget = -1;
            LedLogic.applyLevel(level, pos, next, false);
        }
        if (autoMode == AutoMode.OFF && channel == 0) return;
        if (Math.floorMod(level.getGameTime() + pos.asLong(), 20L) != 0) return;
        if (autoMode != AutoMode.OFF) {
            int wanted = autoMode.wantsLight(level, pos, Math.min(autoRange, CommonConfig.AUTO_MAX_RANGE.get())) ? 1 : 0;
            if (wanted != autoState) {
                autoState = wanted;
                fadeTo(wanted == 1 ? onLevel : 0);
            }
        }
        LightChannels channels = channel == 0 ? null : LightChannels.get(level);
        if (channels != null && (channels.isOn(channel) ? 1 : 0) != channelState) channelChanged(channels.isOn(channel));
    }

    @Override
    public int getChannel() {
        return channel;
    }

    public void setChannel(int value) {
        if (channel == value) return;
        if (level != null) LightChannels.untrack(level, worldPosition, channel);
        channel = value;
        LightChannels channels = level == null ? null : LightChannels.get(level);
        channelState = channels != null && channels.isOn(value) ? 1 : 0;
        if (level != null) LightChannels.track(level, worldPosition, channel);
        setChanged();
    }

    @Override
    public void channelChanged(boolean on) {
        channelState = on ? 1 : 0;
        if (!on && level != null) LedAnimator.stopAt(level, worldPosition);
        LightChannels channels = level == null ? null : LightChannels.get(level);
        fadeTo(on ? (channels == null ? onLevel : channels.level(channel, onLevel)) : 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) LightChannels.track(level, worldPosition, channel);
        if (level != null && level.isClientSide) clientLoaded.accept(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) LightChannels.untrack(level, worldPosition, channel);
    }

    public LedLook look() {
        return LedLook.light(rgb);
    }

    protected void applyLook(LedLook look) {
        rgb = look.rgb() & 0xFFFFFF;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        LedLook look = look();
        if (!look.equals(LedLook.light(DEFAULT_RGB))) components.set(ModDataComponents.LED_LOOK.get(), look);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        LedLook look = input.get(ModDataComponents.LED_LOOK.get());
        if (look != null) applyLook(look);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("rgb");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("rgb", rgb);
        tag.putInt("redstone", redstoneMode.ordinal());
        tag.putInt("last_signal", lastSignal);
        tag.putInt("on_level", onLevel);
        tag.putInt("auto", autoMode.ordinal());
        tag.putInt("auto_range", autoRange);
        if (lightMode >= 0) tag.putInt("light_mode", lightMode);
        if (!lightTinted) tag.putBoolean("light_tint_off", true);
        if (fadeTarget >= 0) tag.putInt("fade_target", fadeTarget);
        if (channel != 0) {
            tag.putInt("channel", channel);
            tag.putInt("channel_state", channelState);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        int oldRgb = rgb;
        int oldLook = modelLook();
        readData(tag, registries);
        if (level != null && level.isClientSide) refreshClient(oldRgb != rgb, oldLook != modelLook());
    }

    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rgb = tag.contains("rgb") ? tag.getInt("rgb") & 0xFFFFFF : DEFAULT_RGB;
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone"));
        lastSignal = tag.contains("last_signal") ? tag.getInt("last_signal") : -1;
        onLevel = tag.contains("on_level") ? Mth.clamp(tag.getInt("on_level"), 1, 15) : 8;
        autoMode = AutoMode.byId(tag.getInt("auto"));
        autoRange = tag.contains("auto_range")
                ? Mth.clamp(tag.getInt("auto_range"), AutoMode.MIN_RANGE, AutoMode.MAX_RANGE) : AutoMode.DEFAULT_RANGE;
        lightMode = tag.contains("light_mode") ? Mth.clamp(tag.getInt("light_mode"), -1, 2) : -1;
        lightTinted = !tag.getBoolean("light_tint_off");
        fadeTarget = tag.contains("fade_target") ? Mth.clamp(tag.getInt("fade_target"), 0, 15) : -1;
        int oldChannel = channel;
        channel = tag.getInt("channel");
        channelState = tag.contains("channel_state") ? tag.getInt("channel_state") : -1;
        if (level != null && oldChannel != channel) {
            LightChannels.untrack(level, worldPosition, oldChannel);
            LightChannels.track(level, worldPosition, channel);
        }
    }

    protected int modelLook() {
        return 0;
    }

    private void refreshClient(boolean rgbChanged, boolean lookChanged) {
        if (lookChanged) {
            requestModelDataUpdate();
            rebuildClient();
            return;
        }
        if (!rgbChanged) return;
        long now = System.nanoTime();
        boolean quick = lastColorNanos != 0 && now - lastColorNanos < HOT_GAP_NANOS;
        lastColorNanos = now;
        if (hot) return;
        if (quick && hotAllowed.test(this)) hot = true;
        rebuildClient();
    }

    public static int clientRebuilds;

    public void rebuildClient() {
        if (level == null) return;
        clientRebuilds++;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
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
