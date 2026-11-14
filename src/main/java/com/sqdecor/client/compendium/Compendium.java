package com.sqdecor.client.compendium;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class Compendium {
    private Compendium() {}

    public record Entry(String id, List<ItemStack> pictures, List<ItemStack> recipes) {
        public Component title() {
            return Component.translatable("compendium.sqdecor." + id + ".title");
        }

        public Component text() {
            return Component.translatable("compendium.sqdecor." + id + ".text");
        }
    }

    public record Chapter(String id, ItemStack icon, List<Entry> entries) {
        public Component title() {
            return Component.translatable("compendium.sqdecor.chapter." + id);
        }
    }

    private static List<Chapter> chapters;

    public static List<Chapter> chapters() {
        if (chapters == null) chapters = build();
        return chapters;
    }

    private static List<Chapter> build() {
        List<Chapter> list = new ArrayList<>();
        list.add(chapter("start", ModItems.HANDBOOK,
                entry("welcome", ModItems.HANDBOOK),
                entry("basics", ModItems.LED_BULB).noRecipes()));
        list.add(chapter("lamps", ModItems.LED_BULB,
                entry("bulbs", ModItems.LED_BULB, ModItems.LED_CAGE_BULB),
                entry("strips", ModBlocks.LED_STRIP, ModBlocks.LED_CORNER, ModBlocks.LED_STRIP_CORNER_L,
                        ModBlocks.LED_STRIP_CROSS, ModBlocks.LED_MIDDLE, ModBlocks.LED_MIDDLE_HIGH),
                entry("tubes", ModBlocks.LED_TUBE, ModBlocks.LED_STRIP_1, ModBlocks.LED_STRIP_1_MIDDLE,
                        ModBlocks.LED_STRIP_1_LONG, ModBlocks.LED_STRIP_2, ModBlocks.LED_STRIP_2_MIDDLE,
                        ModBlocks.LED_STRIP_2_LONG),
                entry("panels", ModBlocks.LED_PANEL, ModBlocks.LED_PANEL_BEVEL, ModBlocks.LED_PANEL_LAMELLA,
                        ModBlocks.LED_PANEL_BEVEL_LAMELLA, ModBlocks.LED_PANEL_MIDDLE, ModBlocks.LED_PANEL_LONG,
                        ModBlocks.LED_PANEL_SQUARE, ModBlocks.LED_PANEL_SQUARE_MIDDLE, ModBlocks.LED_PANEL_SQUARE_BIG),
                entry("hang_lamp", ModBlocks.HANG_LAMP),
                entry("wall_lamp", ModBlocks.WALL_LAMP),
                entry("ceiling", ModBlocks.CEILING_ROUND, ModBlocks.CEILING_SQUARE, ModBlocks.CEILING_RING,
                        ModBlocks.LED_PANEL_ROUND, ModBlocks.STRIP_LIGHT, ModBlocks.GLOBE_LAMP),
                entry("spotlight", ModBlocks.SPOTLIGHT),
                entry("spots", ModBlocks.DOWNLIGHT, ModBlocks.FLOODLIGHT, ModBlocks.TRACK_SPOTS, ModBlocks.TRIPLE_SPOTS),
                entry("beacon", ModBlocks.BEACON_BLUE, ModBlocks.BEACON_RED, ModBlocks.BEACON_ORANGE),
                entry("industrial", ModBlocks.INDUSTRIAL_PENDANT, ModBlocks.INDUSTRIAL_CAGE, ModBlocks.HIGH_BAY,
                        ModBlocks.BULKHEAD, ModBlocks.STACK_LIGHT),
                entry("work_lights", ModBlocks.WORK_LIGHT, ModBlocks.WORK_LIGHT_TALL, ModBlocks.WORK_LIGHT_SMALL),
                entry("lab", ModBlocks.LAB_LAMP, ModBlocks.LAB_TUBE),
                entry("shelves", ModBlocks.SHELF, ModBlocks.SHELF_PLAIN, ModBlocks.SHELF_TALL, ModBlocks.SHELF_TALL_PLAIN, ModBlocks.WALL_SHELF),
                entry("signs", ModBlocks.EXIT_SIGN_LEFT, ModBlocks.EXIT_SIGN_RIGHT, ModBlocks.EXIT_SIGN_DOWN, ModBlocks.ASSEMBLY_POINT_SIGN, ModBlocks.SIGN_EXIT, ModBlocks.SIGN_WARNING, ModBlocks.SIGN_ARROW, ModBlocks.SIGN_NO_ENTRY,
                        ModBlocks.SIGN_STOP, ModBlocks.SIGN_EXCLAMATION, ModBlocks.SIGN_PARKING, ModBlocks.SIGN_INFO,
                        ModBlocks.SIGN_FIRST_AID, ModBlocks.SIGN_ELECTRIC, ModBlocks.SIGN_OK, ModBlocks.SIGN_CROSS),
                entry("computers_decor", ModBlocks.DESKTOP_MONITOR, ModBlocks.CRT_MONITOR, ModBlocks.LAPTOP, ModBlocks.TOWER_PC, ModBlocks.SERVER_RACK, ModBlocks.KEYBOARD, ModBlocks.FRAMED_GRAPH, ModBlocks.FRAMED_MAP, ModBlocks.FRAMED_STATUS, ModBlocks.WALL_SCREEN, ModBlocks.WALL_TERMINAL, ModBlocks.RADAR_SCREEN, ModBlocks.BLINKEN_PANEL),
                entry("signals", ModBlocks.STATUS_LIGHT, ModBlocks.SEVEN_SEGMENT, ModBlocks.DOT_DISPLAY, ModBlocks.ARROW_SIGNAL,
                        ModBlocks.CROSS_SIGNAL, ModBlocks.BAR_DISPLAY, ModBlocks.SIGNAL_POST),
                entry("more_lamps", ModBlocks.EMERGENCY_LIGHT, ModBlocks.WALL_CAGE, ModBlocks.TUBE_PENDANT,
                        ModBlocks.TRAFFIC_LIGHT, ModBlocks.FLOOR_LAMP, ModBlocks.NEON_RING, ModBlocks.FLASK_LAMP)));
        list.add(chapter("design", Items.RED_DYE,
                entry("colors", Items.RED_DYE, Items.LIME_DYE, Items.LIGHT_BLUE_DYE, Items.YELLOW_DYE).noRecipes(),
                entry("cases", Items.OAK_PLANKS, Items.COPPER_BLOCK, Items.QUARTZ_BLOCK, Items.BLACK_CONCRETE).noRecipes(),
                entry("glow", Items.GLOWSTONE_DUST).noRecipes()));
        list.add(chapter("tools", ModItems.LED_TUNER,
                entry("tuner", ModItems.LED_TUNER),
                entry("temperature", ModItems.LED_TUNER).noRecipes(),
                entry("scenes", ModItems.LED_TUNER).noRecipes(),
                entry("remote", ModItems.LED_REMOTE),
                entry("programs", ModItems.LED_REMOTE).noRecipes(),
                entry("tape", ModItems.CAUTION_TAPE),
                entry("thin_cable", ModItems.THIN_CABLE),
                entry("cables", com.sqdecor.registry.DecorBlocks.CABLE_1, com.sqdecor.registry.DecorBlocks.CABLE_2, com.sqdecor.registry.DecorBlocks.CABLE_3,
                        com.sqdecor.registry.DecorBlocks.CABLE_1_ANCHOR, com.sqdecor.registry.DecorBlocks.CABLE_2_ANCHOR, com.sqdecor.registry.DecorBlocks.CABLE_3_ANCHOR),
                entry("cable_rack", ModBlocks.CABLE_RACK_2, ModBlocks.CABLE_RACK_3, ModBlocks.CABLE_RACK_5, ModBlocks.CABLE_RACK_10)));
        list.add(chapter("switching", ModBlocks.LIGHT_SWITCH,
                entry("light_switch", ModBlocks.LIGHT_SWITCH),
                entry("dimmer", ModBlocks.DIMMER_SWITCH),
                entry("motion", ModBlocks.MOTION_SENSOR),
                entry("dusk", ModBlocks.DUSK_SENSOR),
                entry("timer", ModBlocks.TIMER_SWITCH),
                entry("automatic", Items.CLOCK, Items.REDSTONE).noRecipes()));
        list.add(chapter("industrial", ModBlocks.industrial("diamond_plate"),
                entry("industrial_props", ModBlocks.SKIRTING, ModBlocks.WALL_TRIM, ModBlocks.FLOOR_GRATE, ModBlocks.FLOOR_HATCH, ModBlocks.WALL_VENT, ModBlocks.TRAFFIC_CONE, ModBlocks.PYLON_TALL, ModBlocks.BARRIER_POST, ModBlocks.BARRIER_POST_CHAIN, ModBlocks.BARRIER_GATE, ModBlocks.BARRIER_FENCE, ModBlocks.JERSEY_BARRIER, ModBlocks.BARREL, ModBlocks.RAILING, ModBlocks.LADDER, ModBlocks.HAZARD_STRIP, ModBlocks.BOLLARD, ModBlocks.PALLET, ModBlocks.CRATE, ModBlocks.CONSTRUCTION_BLINKER),
                entry("industrial_blocks", industrialBlocks()),
                entry("frames", com.sqdecor.registry.DecorBlocks.FRAMES.toArray(ItemLike[]::new)),
                entry("walkway", com.sqdecor.registry.DecorBlocks.WALKWAY),
                entry("industrial_controls", com.sqdecor.registry.DecorBlocks.INDUSTRIAL_LEVER, com.sqdecor.registry.DecorBlocks.ROTARY_SWITCH, com.sqdecor.registry.DecorBlocks.EMERGENCY_STOP, com.sqdecor.registry.DecorBlocks.SWITCH_CABINET, com.sqdecor.registry.DecorBlocks.FUSE_BOX),
                entry("workshop", ModBlocks.WORKBENCH, ModBlocks.TOOL_WALL, ModBlocks.TOOL_CABINET, ModBlocks.OFFICE_DESK,
                        ModBlocks.OFFICE_CHAIR, ModBlocks.FILING_CABINET)));
        list.add(chapter("bunker", ModBlocks.ANIM_STRIP,
                entry("anim_strips", ModBlocks.ANIM_STRIP, ModBlocks.ANIM_STRIP_THIN, ModBlocks.ANIM_STRIP_DOUBLE, ModBlocks.ANIM_STRIP_CORNER, ModBlocks.ANIM_RING),
                entry("controls", ModBlocks.BUTTON_ROUND, ModBlocks.BUTTON_BIG_RED, ModBlocks.BUTTON_PAIR, ModBlocks.BUTTON_GRID, ModBlocks.TOGGLE_PANEL, ModBlocks.KEY_SWITCH, ModBlocks.KEYPAD, ModBlocks.CARD_READER, ModBlocks.INTERCOM, ModBlocks.FINGERPRINT_SCANNER, ModBlocks.DIAL_PANEL, ModBlocks.SLIDER_PANEL, ModBlocks.STATUS_BOARD, ModBlocks.ALARM_HORN, ModBlocks.DOOR_STATUS_LIGHT, ModBlocks.HAZARD_LIGHT_BAR),
                entry("it_gear", ModBlocks.OSCILLOSCOPE, ModBlocks.SERVER_RACK_BLADES, ModBlocks.NETWORK_SWITCH, ModBlocks.PATCH_PANEL, ModBlocks.UPS_UNIT, ModBlocks.LAPTOP_CLOSED, ModBlocks.TABLET_STAND, ModBlocks.DESK_TERMINAL, ModBlocks.MONITOR_WALL, ModBlocks.CLOCK_DIGITAL, ModBlocks.HOLOGRAM_PROJECTOR)));
        list.add(chapter("styles", ModBlocks.SCIFI_RING_LIGHT,
                entry("style_models", ModBlocks.STYLE_MODELS.values().stream().flatMap(List::stream)
                        .map(b -> (ItemLike) b.get()).toArray(ItemLike[]::new)),
                entry("style_fences", ModBlocks.FENCES.stream().map(f -> (ItemLike) f.get()).toArray(ItemLike[]::new))));
        list.add(chapter("marquee", ModItems.LED_MARQUEE,
                entry("marquee", ModItems.LED_MARQUEE, ModItems.MARQUEE_FRAMED, ModItems.MARQUEE_HAZARD, ModItems.MARQUEE_SLIM,
                        ModItems.MARQUEE_BEZEL, ModItems.MARQUEE_EXIT)));
        list.add(chapter("compat", Items.PISTON,
                entry("computers", optional("computercraft:computer_normal", Items.REDSTONE)).noRecipes(),
                entry("monitor", ModBlocks.IMAGE_MONITOR),
                entry("create", optional("create:wrench", Items.PISTON)).noRecipes()));
        return List.copyOf(list);
    }

    private static Chapter chapter(String id, ItemLike icon, Builder... entries) {
        return new Chapter(id, new ItemStack(icon), Arrays.stream(entries).map(Builder::build).toList());
    }

    private static ItemLike[] industrialBlocks() {
        return Arrays.stream(ModBlocks.INDUSTRIAL_TEXTURES).map(t -> (ItemLike) ModBlocks.industrial(t).get())
                .toArray(ItemLike[]::new);
    }

    private static Builder entry(String id, ItemLike... items) {
        return new Builder(id, items);
    }

    private static ItemLike optional(String id, ItemLike fallback) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).<ItemLike>map(item -> item).orElse(fallback);
    }

    private static final class Builder {
        private final String id;
        private final List<ItemStack> items;
        private boolean recipes = true;

        Builder(String id, ItemLike... items) {
            this.id = id;
            this.items = Arrays.stream(items).map(ItemStack::new).toList();
        }

        Builder noRecipes() {
            recipes = false;
            return this;
        }

        Entry build() {
            return new Entry(id, items, recipes ? items : List.of());
        }
    }
}
