package com.sqdecor.registry;

import com.sqdecor.block.lamp.LampBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class TabGroups {
    private TabGroups() {}

    public record Group(String id, int color) {}

    public static final List<Group> GROUPS = List.of(
            new Group("leds", 0x3FA9F5),
            new Group("ceiling", 0x5AC8D8),
            new Group("spots", 0xE8C547),
            new Group("warning", 0xE8573F),
            new Group("signs", 0x4CB86A),
            new Group("screens", 0x6A7BE0),
            new Group("controls", 0xD8803C),
            new Group("cables", 0x8C8C8C),
            new Group("building", 0x7FB2C9),
            new Group("industrial", 0xC9A227),
            new Group("style_blocks", 0xA0785A),
            new Group("style_sets", 0x9A6FD0),
            new Group("props", 0xB0B0B0),
            new Group("tools", 0xD05A8C),
            new Group("misc", 0x707070));

    public static final DeferredItem<Item> SPACER = ModItems.ITEMS.registerSimpleItem("tab_spacer");
    public static final Map<String, DeferredItem<Item>> HEADERS = new LinkedHashMap<>();

    static {
        for (Group group : GROUPS) HEADERS.put(group.id(), ModItems.ITEMS.registerSimpleItem("header_" + group.id()));
    }

    static void init() {}

    public static boolean isLayoutItem(Item item) {
        if (item == SPACER.get()) return true;
        for (DeferredItem<Item> header : HEADERS.values()) if (item == header.get()) return true;
        return false;
    }

    public static Group headerGroup(Item item) {
        for (Group group : GROUPS) if (HEADERS.get(group.id()).get() == item) return group;
        return null;
    }

    private static final Set<String> TOOLS = Set.of("led_tuner", "led_remote", "handbook");
    private static final Set<String> CABLES = Set.of("caution_tape", "thin_cable", "cable_1", "cable_2", "cable_3", "cable_1_anchor", "cable_2_anchor", "cable_3_anchor", "cable_rack_2", "cable_rack_3",
            "cable_rack_5", "cable_rack_10");
    private static final Set<String> BUILDING = Set.of("grating_walkway");
    private static final Set<String> CONTROLS = Set.of("button_round", "button_big_red", "button_pair", "button_grid",
            "toggle_panel", "key_switch", "keypad", "card_reader", "intercom", "fingerprint_scanner", "dial_panel",
            "slider_panel", "industrial_lever", "rotary_switch", "emergency_stop", "switch_cabinet", "fuse_box");
    private static final Set<String> SCREENS = Set.of("image_monitor", "desktop_monitor", "crt_monitor", "laptop",
            "laptop_closed", "tower_pc", "server_rack", "server_rack_blades", "keyboard", "framed_graph", "framed_map", "framed_status",
            "wall_screen",
            "wall_terminal", "radar_screen", "blinken_panel", "status_board", "oscilloscope", "network_switch",
            "patch_panel", "ups_unit", "tablet_stand", "desk_terminal", "monitor_wall", "clock_digital",
            "hologram_projector");
    private static final Set<String> SIGNS = Set.of("exit_sign_left", "exit_sign_right", "exit_sign_down",
            "assembly_point_sign", "status_light", "seven_segment", "dot_display", "arrow_signal", "cross_signal",
            "bar_display", "signal_post", "traffic_light");
    private static final Set<String> WARNING = Set.of("beacon_blue", "beacon_red", "beacon_orange", "stack_light",
            "construction_blinker", "alarm_horn", "hazard_light_bar", "door_status_light", "emergency_light");
    private static final Set<String> SPOTS = Set.of("spotlight", "downlight", "floodlight", "track_spots",
            "triple_spots", "work_light", "work_light_tall", "work_light_small", "industrial_pendant", "industrial_cage",
            "high_bay", "bulkhead", "wall_cage", "lab_lamp", "lab_tube", "flask_lamp");
    private static final Set<String> CEILING = Set.of("ceiling_round", "ceiling_square", "ceiling_ring", "strip_light",
            "globe_lamp", "hang_lamp", "wall_lamp", "tube_pendant", "neon_ring", "floor_lamp");

    private static Map<Item, String> byList;

    private static Map<Item, String> listGroups() {
        if (byList != null) return byList;
        Map<Item, String> map = new java.util.HashMap<>();
        put(map, ModBlocks.INDUSTRIAL, "industrial");
        put(map, ModBlocks.FENCES, "style_blocks");
        ModBlocks.STYLE_MODELS.values().forEach(list -> put(map, list, "style_sets"));
        put(map, ModBlocks.SWITCHES, "controls");
        put(map, ModBlocks.MARQUEES, "screens");
        put(map, DecorBlocks.FRAMES, "building");
        byList = map;
        return map;
    }

    private static void put(Map<Item, String> map, List<? extends DeferredBlock<? extends Block>> blocks, String group) {
        for (DeferredBlock<? extends Block> block : blocks) {
            Item item = block.get().asItem();
            if (item != net.minecraft.world.item.Items.AIR) map.put(item, group);
        }
    }

    public static String groupOf(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        String listed = listGroups().get(item);
        if (listed != null) return listed;
        if (TOOLS.contains(id)) return "tools";
        if (CABLES.contains(id)) return "cables";
        if (BUILDING.contains(id)) return "building";
        if (CONTROLS.contains(id)) return "controls";
        if (SCREENS.contains(id)) return "screens";
        if (SIGNS.contains(id) || id.startsWith("sign_")) return "signs";
        if (WARNING.contains(id)) return "warning";
        if (SPOTS.contains(id)) return "spots";
        if (CEILING.contains(id) || id.startsWith("led_panel")) return "ceiling";
        if (id.startsWith("led_") || id.startsWith("anim_")) return "leds";
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof LampBlock) return "props";
        return "misc";
    }

    public static void fill(CreativeModeTab.Output output, List<Item> items) {
        Map<String, List<Item>> grouped = new LinkedHashMap<>();
        for (Group group : GROUPS) grouped.put(group.id(), new ArrayList<>());
        for (Item item : items) grouped.get(groupOf(item)).add(item);

        int[] count = {0};
        int[] spacer = {0};
        Set<Item> seen = new HashSet<>();
        for (Group group : GROUPS) {
            List<Item> list = grouped.get(group.id());
            if (list.isEmpty()) continue;
            padRow(output, count, spacer);
            output.accept(new ItemStack(HEADERS.get(group.id()).get()), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            count[0]++;
            padRow(output, count, spacer);
            for (Item item : list) {
                if (!seen.add(item)) continue;
                output.accept(item);
                count[0]++;
            }
        }
    }

    private static void padRow(CreativeModeTab.Output output, int[] count, int[] spacer) {
        while (count[0] % 9 != 0) {
            ItemStack stack = new ItemStack(SPACER.get());
            int n = spacer[0]++;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("n", n));
            output.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            count[0]++;
        }
    }
}
