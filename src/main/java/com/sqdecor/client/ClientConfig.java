package com.sqdecor.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private ClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HALO;
    public static final ModConfigSpec.DoubleValue HALO_STRENGTH;
    public static final ModConfigSpec.BooleanValue LIGHT_CONES;
    public static final ModConfigSpec.DoubleValue CONE_STRENGTH;
    public static final ModConfigSpec.IntValue EFFECT_VIEW_DISTANCE;
    public static final ModConfigSpec.BooleanValue OCCLUSION_CULLING;
    public static final ModConfigSpec.BooleanValue CHUNK_MESHES;
    public static final ModConfigSpec.IntValue SCREEN_VIEW_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HALO = builder.comment("Soft glow around LEDs that are on")
                .translation("sqdecor.configuration.halo")
                .define("halo", true);
        HALO_STRENGTH = builder.comment("How strong the glow is (0 = invisible, 1 = strong)")
                .translation("sqdecor.configuration.halo_strength")
                .defineInRange("halo_strength", 0.5, 0.0, 1.0);
        LIGHT_CONES = builder.comment("Light cones of spotlights and the turning beams of beacon lights")
                .translation("sqdecor.configuration.light_cones")
                .define("light_cones", true);
        CONE_STRENGTH = builder.comment("How strong the light cones are (0 = invisible, 1 = strong)")
                .translation("sqdecor.configuration.cone_strength")
                .defineInRange("cone_strength", 0.5, 0.0, 1.0);
        EFFECT_VIEW_DISTANCE = builder.comment("Draw lamp effects (glow, light cones, chains, shelf items) up to this many blocks away")
                .translation("sqdecor.configuration.effect_view_distance")
                .defineInRange("effect_view_distance", 96, 16, 256);
        OCCLUSION_CULLING = builder.comment("Skip glow, light cones, marquees and other effects that are hidden behind solid blocks")
                .translation("sqdecor.configuration.occlusion_culling")
                .define("occlusion_culling", true);
        CHUNK_MESHES = builder.comment("Build hanging lamp chains, caution tape and thin cables into the chunk meshes instead of drawing them every frame,",
                        "and let running LED programs change colors without rebuilding the chunk. Turn off if something looks wrong")
                .translation("sqdecor.configuration.chunk_meshes")
                .define("chunk_meshes", true);
        SCREEN_VIEW_DISTANCE = builder.comment("Draw marquees up to this many blocks away; image monitors a bit further the larger they are")
                .translation("sqdecor.configuration.screen_view_distance")
                .defineInRange("screen_view_distance", 64, 16, 256);
        SPEC = builder.build();
    }
}
