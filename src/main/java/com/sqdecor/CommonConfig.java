package com.sqdecor;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {
    private CommonConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MONITOR_MAX_RESOLUTION;
    public static final ModConfigSpec.IntValue MONITOR_SYNC_TICKS;
    public static final ModConfigSpec.IntValue MONITOR_VIEW_DISTANCE;
    public static final ModConfigSpec.IntValue AUTO_MAX_RANGE;
    public static final ModConfigSpec.IntValue SPOTLIGHT_RANGE;
    public static final ModConfigSpec.IntValue TAPE_MAX_LENGTH;
    public static final ModConfigSpec.IntValue CABLE_MAX_LENGTH;
    public static final ModConfigSpec.BooleanValue ALARM_SOUNDS;
    public static final ModConfigSpec.DoubleValue ALARM_VOLUME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Image monitor (CC: Tweaked)").translation("sqdecor.configuration.monitor").push("monitor");
        MONITOR_MAX_RESOLUTION = b.comment("Largest image monitor resolution per side in pixels")
                .translation("sqdecor.configuration.monitor_max_resolution")
                .defineInRange("max_resolution", 512, 16, 512);
        MONITOR_SYNC_TICKS = b.comment("Send a changed image to players at most every N ticks (2 = 10 frames per second)")
                .translation("sqdecor.configuration.monitor_sync_ticks")
                .defineInRange("sync_ticks", 2, 1, 40);
        MONITOR_VIEW_DISTANCE = b.comment("Only players within this many blocks receive the image (0 = everyone who can see the chunk)")
                .translation("sqdecor.configuration.monitor_view_distance")
                .defineInRange("view_distance", 0, 0, 512);
        b.pop();

        b.comment("Lights").translation("sqdecor.configuration.lights").push("lights");
        AUTO_MAX_RANGE = b.comment("Largest range for the automatic mode 'player nearby' in blocks")
                .translation("sqdecor.configuration.auto_max_range")
                .defineInRange("auto_max_range", 32, 2, 32);
        SPOTLIGHT_RANGE = b.comment("How far spotlights place light blocks (0 = no light blocks)")
                .translation("sqdecor.configuration.spotlight_range")
                .defineInRange("spotlight_range", 16, 0, 16);
        ALARM_SOUNDS = b.comment("Alarm horns and beacon lights make sound while they are on")
                .translation("sqdecor.configuration.alarm_sounds")
                .define("alarm_sounds", true);
        ALARM_VOLUME = b.comment("Volume of alarm horns and beacon lights (0 = silent, 1 = full)")
                .translation("sqdecor.configuration.alarm_volume")
                .defineInRange("alarm_volume", 1.0, 0.0, 1.0);
        b.pop();

        b.comment("Caution tape and thin cable").translation("sqdecor.configuration.tape").push("tape");
        TAPE_MAX_LENGTH = b.comment("Longest caution tape span in blocks")
                .translation("sqdecor.configuration.tape_max_length")
                .defineInRange("tape_max_length", 24, 2, 64);
        CABLE_MAX_LENGTH = b.comment("Longest thin cable span in blocks")
                .translation("sqdecor.configuration.cable_max_length")
                .defineInRange("cable_max_length", 32, 2, 64);
        b.pop();
        SPEC = b.build();
    }
}
