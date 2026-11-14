package com.sqdecor.registry;

import com.sqdecor.CommonConfig;
import com.sqdecor.SQDecor;
import com.sqdecor.block.lamp.BeaconLampBlock;
import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SQDecor.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LED_CLICK = register("led_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_SWITCH = register("light_switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> REMOTE = register("remote");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_HORN = register("alarm_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEACON = register("beacon");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOTION_SENSOR = register("motion_sensor");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARQUEE = register("marquee");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONITOR_TOUCH = register("monitor_touch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONITOR_ON = register("monitor_on");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(SQDecor.id(name)));
    }

    public static void playSwitch(Level level, BlockPos pos, DeferredHolder<SoundEvent, SoundEvent> sound, boolean on) {
        level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, 0.6f, on ? 0.6f : 0.5f);
    }

    public static void play(Level level, BlockPos pos, DeferredHolder<SoundEvent, SoundEvent> sound, float volume) {
        level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, volume, 1f);
    }

    public static void playLoop(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        boolean horn = block == ModBlocks.ALARM_HORN.get();
        if (!horn && !(block instanceof BeaconLampBlock)) return;
        int period = horn ? 30 : 40;
        if (Math.floorMod(level.getGameTime() + pos.asLong(), period) != 0) return;
        if (state.getValue(LedLogic.LIGHT_LEVEL) == 0 || !CommonConfig.ALARM_SOUNDS.get()) return;
        float volume = (horn ? 1f : 0.35f) * CommonConfig.ALARM_VOLUME.get().floatValue();
        if (volume <= 0) return;
        level.playSound(null, pos, (horn ? ALARM_HORN : BEACON).get(), SoundSource.BLOCKS, volume, 1f);
    }

    public static void playLevel(Level level, BlockPos pos, int lightLevel) {
        float pitch = lightLevel == 0 ? 0.5f : 0.6f + lightLevel * 0.04f;
        level.playSound(null, pos, LED_CLICK.get(), SoundSource.BLOCKS, 0.4f, pitch);
    }
}
