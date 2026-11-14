package com.sqdecor.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.sqdecor.SQDecor;
import com.sqdecor.block.industrial.IndustrialBlock;
import net.minecraft.world.level.block.Block;
import com.sqdecor.block.industrial.StyleFenceBlock;
import com.sqdecor.block.lamp.AnimatedStripBlock;
import com.sqdecor.block.lamp.BeaconLampBlock;
import com.sqdecor.block.lamp.GlowBlock;
import com.sqdecor.block.lamp.StripLayout;
import com.sqdecor.block.lamp.HangLampBlock;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.block.lamp.PropBlock;
import com.sqdecor.block.lamp.ShelfBlock;
import com.sqdecor.block.lamp.ShelfTopBlock;
import com.sqdecor.block.lamp.SpotlightBlock;
import com.sqdecor.block.lamp.TallShelfBlock;
import com.sqdecor.block.lamp.WallLampBlock;
import com.sqdecor.block.led.LedBulbBlock;
import com.sqdecor.block.led.LedCageBulbBlock;
import com.sqdecor.block.marquee.MarqueeBlock;
import com.sqdecor.block.monitor.ImageMonitorBlock;
import com.sqdecor.block.tape.TapeAnchorBlock;
import com.sqdecor.block.switches.DimmerSwitchBlock;
import com.sqdecor.block.switches.DuskSensorBlock;
import com.sqdecor.block.switches.LightSwitchBlock;
import com.sqdecor.block.switches.MotionSensorBlock;
import com.sqdecor.block.switches.TimerSwitchBlock;
import com.sqdecor.led.LedLogic;
import com.sqdecor.led.LedLook;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SQDecor.MODID);

    public static final DeferredBlock<LedBulbBlock> LED_BULB = BLOCKS.register("led_bulb",
            () -> new LedBulbBlock(ledProperties()));
    public static final DeferredBlock<LedCageBulbBlock> LED_CAGE_BULB = BLOCKS.register("led_cage_bulb",
            () -> new LedCageBulbBlock(ledProperties()));

    public static final List<DeferredBlock<? extends LampBlock>> LAMPS = new ArrayList<>();
    public static final Map<DeferredBlock<? extends LampBlock>, LedLook> LAMP_LOOKS = new HashMap<>();

    public static final DeferredBlock<LampBlock> LED_STRIP = lamp("led_strip", 6.25, 0, 0, 9.75, 1, 16);
    public static final DeferredBlock<LampBlock> LED_CORNER = lamp("led_corner", 12.5, 0, 0, 16, 1, 16);
    public static final DeferredBlock<LampBlock> LED_STRIP_CORNER_L = lamp("led_strip_corner_l", 0, 0, 0, 16, 1, 16);
    public static final DeferredBlock<LampBlock> LED_STRIP_CROSS = lamp("led_strip_cross", -0.05, 0, 0, 16, 1, 16);
    public static final DeferredBlock<LampBlock> LED_MIDDLE = lamp("led_middle", 6.25, 0, 6.25, 9.75, 1, 9.75);
    public static final DeferredBlock<LampBlock> LED_MIDDLE_HIGH = lamp("led_middle_high", 6.25, 0, 6.25, 9.75, 5, 9.75);

    public static final DeferredBlock<LampBlock> LED_TUBE = lamp("led_tube", 7, 0, 0, 9, 2, 16);
    public static final DeferredBlock<LampBlock> LED_STRIP_1 = lamp("led_strip_1", 6.99, 0, 0, 9.01, 2, 16);
    public static final DeferredBlock<LampBlock> LED_STRIP_1_MIDDLE = lamp("led_strip_1_middle", 6.99, 0, 1, 9.01, 2, 31);
    public static final DeferredBlock<LampBlock> LED_STRIP_1_LONG = lamp("led_strip_1_long", 6.99, 0, -16, 9.01, 2, 32);
    public static final DeferredBlock<LampBlock> LED_STRIP_2 = lamp("led_strip_2", 5.71, 0, 0, 10.29, 2, 16);
    public static final DeferredBlock<LampBlock> LED_STRIP_2_MIDDLE = lamp("led_strip_2_middle", 5.71, 0, 2, 10.29, 2, 30);
    public static final DeferredBlock<LampBlock> LED_STRIP_2_LONG = lamp("led_strip_2_long", 5.71, 0, -16, 10.29, 2, 32);

    public static final DeferredBlock<LampBlock> LED_PANEL = lamp("led_panel", 0, 0, 0, 16, 2, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_BEVEL = lamp("led_panel_bevel", 0, 0, 0, 16, 2, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_LAMELLA = lamp("led_panel_lamella", 0, 0, 0, 16, 2, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_BEVEL_LAMELLA = lamp("led_panel_bevel_lamella", 0, 0, 0, 16, 2, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_MIDDLE = lamp("led_panel_middle", 0.1, 0, 0, 16, 2, 32);
    public static final DeferredBlock<LampBlock> LED_PANEL_LONG = lamp("led_panel_long", -16, 0, 0, 32, 2, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_SQUARE = lamp("led_panel_square", 0, 0, 0, 16, 16, 16);
    public static final DeferredBlock<LampBlock> LED_PANEL_SQUARE_MIDDLE = lamp("led_panel_square_middle", 0, 0, 0, 32, 16, 32);
    public static final DeferredBlock<LampBlock> LED_PANEL_SQUARE_BIG = lamp("led_panel_square_big", -16, 0, -16, 32, 16, 32);

    public static final DeferredBlock<HangLampBlock> HANG_LAMP = register(BLOCKS.register("hang_lamp",
            () -> new HangLampBlock(ledProperties(), "lamp/hang_lamp", new double[]{4.5, 0, 4.5, 11.5, 9.3, 11.5})));
    public static final DeferredBlock<WallLampBlock> WALL_LAMP = register(BLOCKS.register("wall_lamp",
            () -> new WallLampBlock(ledProperties(), "lamp/wall_lamp", new double[]{6.75, 0, 0, 9.25, 3.25, 6.25})));

    public static final DeferredBlock<SpotlightBlock> SPOTLIGHT = register(BLOCKS.register("spotlight",
            () -> new SpotlightBlock(ledProperties(), "lamp/spotlight", new double[]{4.5, 0, 4.5, 11.5, 7, 11.5})));

    public static final DeferredBlock<LampBlock> CEILING_ROUND = lamp("ceiling_round", 1.62, 0, 1.62, 14.38, 3.5, 14.38);
    public static final DeferredBlock<LampBlock> CEILING_SQUARE = lamp("ceiling_square", 1, 0, 1, 15, 2.2, 15);
    public static final DeferredBlock<LampBlock> CEILING_RING = lamp("ceiling_ring", 1.26, 0, 1.26, 14.74, 2.4, 14.74);
    public static final DeferredBlock<LampBlock> LED_PANEL_ROUND = lamp("led_panel_round", 0.56, 0, 0.56, 15.44, 1.1, 15.44);
    public static final DeferredBlock<LampBlock> STRIP_LIGHT = lamp("strip_light", 6.5, 0, 0.5, 9.5, 1.6, 15.5);
    public static final DeferredBlock<LampBlock> GLOBE_LAMP = lamp("globe_lamp", 4.47, 0, 4.47, 11.53, 7.5, 11.53);

    public static final DeferredBlock<SpotlightBlock> DOWNLIGHT = spot("downlight", 4.83, 0, 4.83, 11.17, 0.9, 11.17);
    public static final DeferredBlock<SpotlightBlock> FLOODLIGHT = spot("floodlight", 3, 0, 4.5, 13, 6.8, 11.5);
    public static final DeferredBlock<SpotlightBlock> TRACK_SPOTS = spot("track_spots", 6.04, 0, 0, 9.96, 5.8, 16);
    public static final DeferredBlock<SpotlightBlock> TRIPLE_SPOTS = spot("triple_spots", 1.5, 0, 6.23, 14.5, 4.9, 9.77);

    public static final DeferredBlock<LampBlock> INDUSTRIAL_PENDANT = lamp("industrial_pendant", 1.62, 0, 1.62, 14.38, 9, 14.38);
    public static final DeferredBlock<LampBlock> INDUSTRIAL_CAGE = lamp("industrial_cage", 4.9, 0, 4.96, 11.1, 7.5, 11.04);
    public static final DeferredBlock<LampBlock> HIGH_BAY = lamp("high_bay", 0.64, 0, 0.64, 15.36, 7, 15.36);
    public static final DeferredBlock<LampBlock> BULKHEAD = lamp("bulkhead", 3.59, 0, 3.59, 12.41, 3.3, 12.41);
    public static final DeferredBlock<LampBlock> STACK_LIGHT = lamp("stack_light", 6.43, 0, 6.43, 9.57, 9, 9.57);
    public static final DeferredBlock<LampBlock> WORK_LIGHT = lamp("work_light", 2, 0, 2, 14, 16, 14);
    public static final DeferredBlock<LampBlock> WORK_LIGHT_TALL = lamp("work_light_tall", 2, 0, 2, 14, 16, 14);
    public static final DeferredBlock<LampBlock> WORK_LIGHT_SMALL = lamp("work_light_small", 3.4, 0, 5, 12.6, 7.6, 11);
    public static final DeferredBlock<LampBlock> LAB_LAMP = lamp("lab_lamp", 5.06, 0, 1.56, 10.94, 10.2, 14.44);
    public static final DeferredBlock<LampBlock> LAB_TUBE = lamp("lab_tube", 5.3, 0, 0.5, 10.7, 3.4, 15.5);
    public static final DeferredBlock<ShelfBlock> SHELF = shelf("shelf", true);
    public static final DeferredBlock<ShelfBlock> SHELF_PLAIN = shelf("shelf_plain", false);
    public static final DeferredBlock<TallShelfBlock> SHELF_TALL = tallShelf("shelf_tall", true);
    public static final DeferredBlock<TallShelfBlock> SHELF_TALL_PLAIN = tallShelf("shelf_tall_plain", false);
    public static final DeferredBlock<ShelfTopBlock> SHELF_TOP = BLOCKS.register("shelf_top",
            () -> new ShelfTopBlock(shelfProperties(false).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<LampBlock> WALL_SHELF = lamp("wall_shelf", 1, 0, 11.4, 15, 7, 16);

    public static final DeferredBlock<LampBlock> SIGN_EXIT = lamp("sign_exit",
            new LedLook(0x19C84A, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 4, 15.5, 1.45, 12);
    public static final DeferredBlock<LampBlock> SIGN_WARNING = lamp("sign_warning",
            new LedLook(0xFFC400, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 0.79, 0, 1.5, 15.21, 1.45, 14.5);
    public static final DeferredBlock<LampBlock> SIGN_ARROW = lamp("sign_arrow",
            new LedLook(0x19C84A, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 5, 15.5, 1.45, 11);

    public static final DeferredBlock<LampBlock> EXIT_SIGN_LEFT = lamp("exit_sign_left",
            new LedLook(0xFFFFFF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 4, 15.5, 1.3, 12);
    public static final DeferredBlock<LampBlock> EXIT_SIGN_RIGHT = lamp("exit_sign_right",
            new LedLook(0xFFFFFF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 4, 15.5, 1.3, 12);
    public static final DeferredBlock<LampBlock> EXIT_SIGN_DOWN = lamp("exit_sign_down",
            new LedLook(0xFFFFFF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 4, 15.5, 1.3, 12);
    public static final DeferredBlock<LampBlock> ASSEMBLY_POINT_SIGN = lamp("assembly_point_sign",
            new LedLook(0xFFFFFF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 4, 15.5, 1.3, 12);
    public static final DeferredBlock<LampBlock> WORKBENCH = prop("workbench",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 0, 0, 2, 16, 16, 14);
    public static final DeferredBlock<LampBlock> TOOL_WALL = prop("tool_wall",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 0, 0, 0, 16, 3.5, 16);
    public static final DeferredBlock<LampBlock> OFFICE_CHAIR = prop("office_chair",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 2.25, 0, 2.25, 13.75, 16, 13.25);
    public static final DeferredBlock<LampBlock> OFFICE_DESK = prop("office_desk",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 0, 0, 0, 16, 15.5, 14.3);
    public static final DeferredBlock<LampBlock> FILING_CABINET = prop("filing_cabinet",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 2, 0, 2, 14, 16, 14.9);
    public static final DeferredBlock<LampBlock> TOOL_CABINET = prop("tool_cabinet",
            new LedLook(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.5, 0, 1.5, 15.5, 15, 14.8);

    public static final DeferredBlock<LampBlock> SIGN_NO_ENTRY = lamp("sign_no_entry",
            new LedLook(0xFF2020, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 0.89, 0, 0.89, 15.11, 1.5, 15.11);
    public static final DeferredBlock<LampBlock> SIGN_STOP = lamp("sign_stop",
            new LedLook(0xFF2020, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 1.16, 0, 1.16, 14.84, 1.5, 14.84);
    public static final DeferredBlock<LampBlock> SIGN_EXCLAMATION = lamp("sign_exclamation",
            new LedLook(0xFFC400, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 2, 0, 1, 14, 1.45, 15);
    public static final DeferredBlock<LampBlock> EMERGENCY_LIGHT = lamp("emergency_light", 1, 0, 4.5, 15, 4.6, 11.5);
    public static final DeferredBlock<LampBlock> WALL_CAGE = lamp("wall_cage", 3.1, 0, 3.1, 12.9, 6.8, 12.9);
    public static final DeferredBlock<LampBlock> TUBE_PENDANT = lamp("tube_pendant", 0.1, 0, 6, 15.9, 9.4, 10);
    public static final DeferredBlock<LampBlock> TRAFFIC_LIGHT = lamp("traffic_light",
            new LedLook(0x19C84A, 0x1E1E1E, 0x2A2A2A, Optional.empty(), Optional.empty()), 5.1, 0, 5.1, 10.9, 15.8, 10.9);
    public static final DeferredBlock<LampBlock> FLOOR_LAMP = lamp("floor_lamp", 2.9, 0, 2.9, 13.1, 15.5, 13.1);
    public static final DeferredBlock<LampBlock> NEON_RING = lamp("neon_ring",
            new LedLook(0xFF2DD0, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 1.6, 0, 1.6, 14.4, 1.8, 14.4);
    public static final DeferredBlock<LampBlock> FLASK_LAMP = lamp("flask_lamp", 3.88, 0, 3.88, 12.12, 11.8, 12.12);

    public static final DeferredBlock<LampBlock> SIGN_PARKING = lamp("sign_parking",
            new LedLook(0x1E50FF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 2, 0, 1, 14, 1.45, 15);
    public static final DeferredBlock<LampBlock> SIGN_INFO = lamp("sign_info",
            new LedLook(0x1E50FF, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 2, 0, 1, 14, 1.45, 15);
    public static final DeferredBlock<LampBlock> SIGN_FIRST_AID = lamp("sign_first_aid",
            new LedLook(0x19C84A, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 1, 0, 1, 15, 1.45, 15);
    public static final DeferredBlock<LampBlock> SIGN_ELECTRIC = lamp("sign_electric",
            new LedLook(0xFFC400, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 0.79, 0, 1.5, 15.21, 1.45, 14.5);
    public static final DeferredBlock<LampBlock> SIGN_OK = lamp("sign_ok",
            new LedLook(0x19C84A, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 1.09, 0, 1.09, 14.91, 1.45, 14.91);
    public static final DeferredBlock<LampBlock> SIGN_CROSS = lamp("sign_cross",
            new LedLook(0xFF2020, 0xF0F0F0, 0xFFFFFF, Optional.empty(), Optional.empty()), 1.09, 0, 1.09, 14.91, 1.45, 14.91);
    public static final DeferredBlock<LampBlock> STATUS_LIGHT = lamp("status_light",
            new LedLook(0x19C84A, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 3.06, 0, 3.06, 12.94, 2.6, 12.94);
    public static final DeferredBlock<LampBlock> SEVEN_SEGMENT = lamp("seven_segment",
            new LedLook(0xFF2020, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 3, 0, 0, 13, 1.3, 16);
    public static final DeferredBlock<LampBlock> DOT_DISPLAY = lamp("dot_display",
            new LedLook(0xFFA000, 0x1E1E1E, 0x3A3A3A, Optional.empty(), Optional.empty()), 2.5, 0, 0, 13.5, 1.3, 16);
    public static final DeferredBlock<LampBlock> ARROW_SIGNAL = lamp("arrow_signal",
            new LedLook(0x19C84A, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 0.5, 0, 3, 15.5, 1.3, 13);
    public static final DeferredBlock<LampBlock> CROSS_SIGNAL = lamp("cross_signal",
            new LedLook(0xFF2020, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 2, 0, 2, 14, 1.3, 14);
    public static final DeferredBlock<LampBlock> BAR_DISPLAY = lamp("bar_display",
            new LedLook(0x19C84A, 0x1E1E1E, 0x1E1E1E, Optional.empty(), Optional.empty()), 4.5, 0, 0, 11.5, 1.3, 16);
    public static final DeferredBlock<LampBlock> SIGNAL_POST = lamp("signal_post",
            new LedLook(0xFF2020, 0x2A2A2A, 0x3A3A3A, Optional.empty(), Optional.empty()), 5.1, 0, 5.1, 10.9, 15.8, 10.9);

    public static final DeferredBlock<LampBlock> DESKTOP_MONITOR = glow("desktop_monitor",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 1, 0, 5, 15, 13, 11);
    public static final DeferredBlock<LampBlock> CRT_MONITOR = glow("crt_monitor",
            new LedLook(0xFFFFFF, 0x8A8A82, 0x5A5A55, Optional.empty(), Optional.empty()), 2.5, 0, 1, 13.5, 9, 12.9);
    public static final DeferredBlock<LampBlock> LAPTOP = glow("laptop",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 2, 0, 3.4, 14, 9, 12);
    public static final DeferredBlock<LampBlock> TOWER_PC = glow("tower_pc",
            new LedLook(0x7FD8FF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 4.5, 0, 3, 11.5, 13, 13.7);
    public static final DeferredBlock<LampBlock> SERVER_RACK = glow("server_rack",
            new LedLook(0x19C84A, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 1, 0, 3, 15, 16, 13.7);
    public static final DeferredBlock<LampBlock> KEYBOARD = glow("keyboard",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 1, 0, 5.5, 15, 1.2, 10.5);
    public static final DeferredBlock<LampBlock> FRAMED_GRAPH = glow("framed_graph",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 2, 0, 0.5, 14, 1.5, 15.5);
    public static final DeferredBlock<LampBlock> FRAMED_MAP = glow("framed_map",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 2, 0, 0.5, 14, 1.5, 15.5);
    public static final DeferredBlock<LampBlock> FRAMED_STATUS = glow("framed_status",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 2, 0, 0.5, 14, 1.5, 15.5);
    public static final DeferredBlock<LampBlock> WALL_SCREEN = glow("wall_screen",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 0.4, 0, 3.4, 15.6, 1.3, 12.6);
    public static final DeferredBlock<LampBlock> WALL_TERMINAL = glow("wall_terminal",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 3.5, 0, 2, 12.5, 2.8, 14);
    public static final DeferredBlock<LampBlock> RADAR_SCREEN = glow("radar_screen",
            new LedLook(0xFFFFFF, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 2, 0, 2, 14, 3.5, 14);
    public static final DeferredBlock<LampBlock> BLINKEN_PANEL = glow("blinken_panel",
            new LedLook(0x19C84A, 0x2A2A2A, 0x4A4A4A, Optional.empty(), Optional.empty()), 1, 0, 3, 15, 1.4, 13);

    public static final DeferredBlock<LampBlock> SKIRTING = prop("skirting",
            new LedLook(0xFFFFFF, 0xE8E8E8, 0xB0B0B0, Optional.empty(), Optional.empty()), 0, 0, 6.5, 16, 1.2, 9.9);
    public static final DeferredBlock<LampBlock> WALL_TRIM = prop("wall_trim",
            new LedLook(0xFFFFFF, 0xE8E8E8, 0xB0B0B0, Optional.empty(), Optional.empty()), 0, 0, 6.8, 16, 1.6, 9.6);
    public static final DeferredBlock<LampBlock> FLOOR_GRATE = prop("floor_grate",
            new LedLook(0xFFFFFF, 0x6A6D72, 0x3A3C40, Optional.empty(), Optional.empty()), 0.4, 0, 0.4, 15.6, 0.8, 15.6);
    public static final DeferredBlock<LampBlock> FLOOR_HATCH = prop("floor_hatch",
            new LedLook(0xFFFFFF, 0x7A7D82, 0x5A5D62, Optional.empty(), Optional.empty()), 0.5, 0, 0.5, 15.5, 1.9, 15.5);
    public static final DeferredBlock<LampBlock> WALL_VENT = prop("wall_vent",
            new LedLook(0xFFFFFF, 0xD0D3D8, 0x3A3C40, Optional.empty(), Optional.empty()), 2, 0, 2, 14, 1.5, 14);
    public static final DeferredBlock<LampBlock> TRAFFIC_CONE = prop("traffic_cone",
            new LedLook(0xFFFFFF, 0xFF6A00, 0xF0F0F0, Optional.empty(), Optional.empty()), 4, 0, 4, 12, 11, 12);
    public static final DeferredBlock<LampBlock> PYLON_TALL = prop("pylon_tall",
            new LedLook(0xFFFFFF, 0xFF6A00, 0xF0F0F0, Optional.empty(), Optional.empty()), 3, 0, 3, 13, 16, 13);
    public static final DeferredBlock<LampBlock> BARRIER_POST = prop("barrier_post",
            new LedLook(0xFFFFFF, 0xE8B000, 0x2A2A2A, Optional.empty(), Optional.empty()), 4.88, 0, 4.8, 11.12, 13.2, 11.2);
    public static final DeferredBlock<LampBlock> BARRIER_POST_CHAIN = prop("barrier_post_chain",
            new LedLook(0xFFFFFF, 0xE8B000, 0x2A2A2A, Optional.empty(), Optional.empty()), 0, 0, 4.8, 16, 13.2, 11.2);
    public static final DeferredBlock<LampBlock> BARRIER_GATE = prop("barrier_gate",
            new LedLook(0xFFFFFF, 0xF0F0F0, 0xE02020, Optional.empty(), Optional.empty()), 0, 0, 6, 16, 12, 10);
    public static final DeferredBlock<LampBlock> BARRIER_FENCE = prop("barrier_fence",
            new LedLook(0xFFFFFF, 0x9AA0A6, 0x2A2A2A, Optional.empty(), Optional.empty()), 0, 0, 5.6, 16, 13.2, 10.4);
    public static final DeferredBlock<LampBlock> JERSEY_BARRIER = prop("jersey_barrier",
            new LedLook(0xFFFFFF, 0xBFBFBF, 0xFF6A00, Optional.empty(), Optional.empty()), 0, 0, 4.5, 16, 10, 11.5);
    public static final DeferredBlock<LampBlock> BARREL = prop("barrel",
            new LedLook(0xFFFFFF, 0xE03030, 0x2A2A2A, Optional.empty(), Optional.empty()), 2.8, 0, 2.8, 13.2, 12.4, 13.2);
    public static final DeferredBlock<LampBlock> RAILING = prop("railing",
            new LedLook(0xFFFFFF, 0x8A8F98, 0xFFC400, Optional.empty(), Optional.empty()), 0, 0, 7.4, 16, 12.6, 8.6);
    public static final DeferredBlock<LampBlock> LADDER = prop("ladder",
            new LedLook(0xFFFFFF, 0x8A8F98, 0xFFC400, Optional.empty(), Optional.empty()), 4.5, 0, 0, 11.5, 1.6, 16);
    public static final DeferredBlock<LampBlock> HAZARD_STRIP = prop("hazard_strip",
            new LedLook(0xFFFFFF, 0x1E1E1E, 0xFFC400, Optional.empty(), Optional.empty()), 0, 0, 5.5, 16, 0.5, 10.5);
    public static final DeferredBlock<LampBlock> BOLLARD = prop("bollard",
            new LedLook(0xFFFFFF, 0xFFC400, 0x2A2A2A, Optional.empty(), Optional.empty()), 4.98, 0, 4.9, 11.02, 10.6, 11.1);
    public static final DeferredBlock<LampBlock> PALLET = prop("pallet",
            new LedLook(0xFFFFFF, 0xB08850, 0x8A6A3A, Optional.empty(), Optional.empty()), 0.5, 0, 0.5, 15.5, 4.2, 15.5);
    public static final DeferredBlock<LampBlock> CRATE = prop("crate",
            new LedLook(0xFFFFFF, 0xA07A45, 0x6E5230, Optional.empty(), Optional.empty()), 0.6, 0, 0.6, 15.4, 14, 15.4);
    public static final DeferredBlock<LampBlock> CONSTRUCTION_BLINKER = lamp("construction_blinker",
            new LedLook(0xFF8C00, 0x2A2A2A, 0xF0F0F0, Optional.empty(), Optional.empty()), 4, 0, 5, 12, 14.2, 11);

    public static final DeferredBlock<LampBlock> BUTTON_ROUND = glow("button_round",
            new LedLook(0xFF3030, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 6, 0, 6, 10, 2.2, 10);
    public static final DeferredBlock<LampBlock> BUTTON_BIG_RED = glow("button_big_red",
            new LedLook(0xFF1A1A, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 4.67, 0, 4.67, 11.33, 4.2, 11.33);
    public static final DeferredBlock<LampBlock> BUTTON_PAIR = glow("button_pair",
            new LedLook(0x30FF50, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5, 0, 2.5, 11, 2.2, 13.5);
    public static final DeferredBlock<LampBlock> BUTTON_GRID = glow("button_grid",
            new LedLook(0x4DD0FF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 1, 0, 1, 15, 1.7, 15);
    public static final DeferredBlock<LampBlock> TOGGLE_PANEL = glow("toggle_panel",
            new LedLook(0xFFB020, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 1, 0, 4.5, 15, 3.2, 11.5);
    public static final DeferredBlock<LampBlock> KEY_SWITCH = glow("key_switch",
            new LedLook(0xFF3030, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5.5, 0, 5.5, 10.5, 2.6, 10.5);
    public static final DeferredBlock<LampBlock> KEYPAD = glow("keypad",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5, 0, 2.3, 11, 1.4, 13);
    public static final DeferredBlock<LampBlock> CARD_READER = glow("card_reader",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5.5, 0, 4, 10.5, 1.4, 12);
    public static final DeferredBlock<LampBlock> INTERCOM = glow("intercom",
            new LedLook(0xFFB020, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 4.5, 0, 3.5, 11.5, 1.6, 12.5);
    public static final DeferredBlock<LampBlock> FINGERPRINT_SCANNER = glow("fingerprint_scanner",
            new LedLook(0x4DD0FF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5, 0, 4, 11, 1.6, 12);
    public static final DeferredBlock<LampBlock> DIAL_PANEL = glow("dial_panel",
            new LedLook(0x7CFF6B, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 4, 16, 1.6, 12);
    public static final DeferredBlock<LampBlock> SLIDER_PANEL = glow("slider_panel",
            new LedLook(0x4DD0FF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 3.5, 16, 2, 12.5);
    public static final DeferredBlock<LampBlock> STATUS_BOARD = glow("status_board",
            new LedLook(0x30FF50, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 0, 16, 2.1, 16);
    public static final DeferredBlock<LampBlock> OSCILLOSCOPE = glow("oscilloscope",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 2.5, 0, 3.5, 13.5, 9, 13.2);
    public static final DeferredBlock<LampBlock> SERVER_RACK_BLADES = glow("server_rack_blades",
            new LedLook(0x30FF50, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 1, 0, 3, 15, 16, 13.5);
    public static final DeferredBlock<LampBlock> NETWORK_SWITCH = glow("network_switch",
            new LedLook(0x30FF50, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0.5, 0, 5, 15.5, 2.4, 11.4);
    public static final DeferredBlock<LampBlock> PATCH_PANEL = glow("patch_panel",
            new LedLook(0xFFB020, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0.5, 0, 5.5, 15.5, 3.4, 10.9);
    public static final DeferredBlock<LampBlock> UPS_UNIT = glow("ups_unit",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 2.5, 0, 3, 13.5, 8, 13.5);
    public static final DeferredBlock<LampBlock> LAPTOP_CLOSED = glow("laptop_closed",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 2, 0, 3.8, 14, 2, 12.2);
    public static final DeferredBlock<LampBlock> TABLET_STAND = glow("tablet_stand",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 3, 0, 5.5, 13, 11.4, 10.5);
    public static final DeferredBlock<LampBlock> DESK_TERMINAL = glow("desk_terminal",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 2, 0, 3, 14, 11, 13);
    public static final DeferredBlock<LampBlock> MONITOR_WALL = glow("monitor_wall",
            new LedLook(0xFFFFFF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 2, 16, 1.7, 14);
    public static final DeferredBlock<LampBlock> CLOCK_DIGITAL = glow("clock_digital",
            new LedLook(0xFF3030, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 5, 16, 1.4, 11);
    public static final DeferredBlock<LampBlock> HOLOGRAM_PROJECTOR = glow("hologram_projector",
            new LedLook(0x4DD0FF, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 3.67, 0, 3.6, 12.33, 8, 12.4);
    public static final DeferredBlock<LampBlock> ALARM_HORN = lamp("alarm_horn",
            new LedLook(0xFF8A00, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 4.91, 0, 4.91, 11.09, 6.6, 11.09);
    public static final DeferredBlock<LampBlock> DOOR_STATUS_LIGHT = lamp("door_status_light",
            new LedLook(0xFF3030, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 5.5, 0, 2.5, 10.5, 2.2, 13.5);
    public static final DeferredBlock<LampBlock> HAZARD_LIGHT_BAR = lamp("hazard_light_bar",
            new LedLook(0xFF8A00, 0x2F3136, 0x55585E, Optional.empty(), Optional.empty()), 0, 0, 6.5, 16, 1.6, 9.5);

    public static final DeferredBlock<AnimatedStripBlock> ANIM_STRIP = animStrip("anim_strip", StripLayout.STRIP, 0, 0, 4.8, 16, 3.25, 11.2);
    public static final DeferredBlock<AnimatedStripBlock> ANIM_STRIP_THIN = animStrip("anim_strip_thin", StripLayout.THIN, 0, 0, 6.3, 16, 2.3, 9.7);
    public static final DeferredBlock<AnimatedStripBlock> ANIM_STRIP_DOUBLE = animStrip("anim_strip_double", StripLayout.DOUBLE, 0, 0, 2.8, 16, 3.25, 13.2);
    public static final DeferredBlock<AnimatedStripBlock> ANIM_STRIP_CORNER = animStrip("anim_strip_corner", StripLayout.CORNER, 0, 0, 0, 16, 3.25, 16);
    public static final DeferredBlock<AnimatedStripBlock> ANIM_RING = animStrip("anim_ring", StripLayout.RING, 0.27, 0, 0.27, 15.73, 3.25, 15.73);

    public static final DeferredBlock<BeaconLampBlock> BEACON_BLUE = beacon("beacon_blue", 0x1E50FF);
    public static final DeferredBlock<BeaconLampBlock> BEACON_RED = beacon("beacon_red", 0xFF1A1A);
    public static final DeferredBlock<BeaconLampBlock> BEACON_ORANGE = beacon("beacon_orange", 0xFF9A00);

    public static final Map<String, List<DeferredBlock<? extends LampBlock>>> STYLE_MODELS = new java.util.LinkedHashMap<>();
    private static final LedLook MECHANICAL_LOOK = new LedLook(0xFFD9A0, 0xB08D57, 0x5A5D62, Optional.empty(), Optional.empty());
    private static final LedLook INDUSTRIAL_LOOK = new LedLook(0xFFF4D0, 0xE0A020, 0x3A3C40, Optional.empty(), Optional.empty());
    private static final LedLook MEDICAL_LOOK = new LedLook(0xFFFFFF, 0xF2F4F5, 0x3A9AD9, Optional.empty(), Optional.empty());
    private static final LedLook SCIFI_LOOK = new LedLook(0x4DD0FF, 0xD8DEE6, 0x2F3640, Optional.empty(), Optional.empty());
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_LANTERN = styled("mechanical", "lamp", "mechanical_lantern", MECHANICAL_LOOK, 3.6, 0, 3.6, 12.4, 15, 12.4);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_GEAR_LAMP = styled("mechanical", "lamp", "mechanical_gear_lamp", MECHANICAL_LOOK, 0.69, 0, 0.69, 15.31, 3, 15.31);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_CAGE_TUBE = styled("industrial", "lamp", "industrial_cage_tube", INDUSTRIAL_LOOK, 0.5, 0, 5.4, 15.5, 5.4, 10.6);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_FLOOD_PANEL = styled("industrial", "lamp", "industrial_flood_panel", INDUSTRIAL_LOOK, 1.5, 0, 3.5, 14.5, 3.8, 12.5);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_SURGICAL_LIGHT = styled("medical", "lamp", "medical_surgical_light", MEDICAL_LOOK, 1, 0, 1, 15, 10.2, 15);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_PANEL_LIGHT = styled("medical", "lamp", "medical_panel_light", MEDICAL_LOOK, 0.8, 0, 0.8, 15.2, 1.8, 15.2);
    public static final DeferredBlock<? extends LampBlock> SCIFI_LIGHT_COLUMN = styled("scifi", "lamp", "scifi_light_column", SCIFI_LOOK, 4.8, 0, 4.8, 11.2, 15.7, 11.2);
    public static final DeferredBlock<? extends LampBlock> SCIFI_RING_LIGHT = styled("scifi", "lamp", "scifi_ring_light", SCIFI_LOOK, 1.06, 0, 1.06, 14.94, 5.6, 14.94);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_SHELF = styled("mechanical", "shelf", "mechanical_shelf", MECHANICAL_LOOK, 0.6, 0, 5, 15.4, 16, 11.3);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_SHELF = styled("industrial", "shelf", "industrial_shelf", INDUSTRIAL_LOOK, 0.6, 0, 5, 15.4, 16, 11);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_SHELF = styled("medical", "shelf", "medical_shelf", MEDICAL_LOOK, 0.6, 0, 5, 15.4, 16, 11.15);
    public static final DeferredBlock<? extends LampBlock> SCIFI_SHELF = styled("scifi", "shelf_lit", "scifi_shelf", SCIFI_LOOK, 0.6, 0, 5, 15.4, 16, 11);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_BARREL = styled("mechanical", "prop", "mechanical_barrel", MECHANICAL_LOOK, 2.61, 0, 2.61, 13.39, 13, 13.39);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_IBC_TANK = styled("industrial", "prop", "industrial_ibc_tank", INDUSTRIAL_LOOK, 0.4, 0, 0.4, 15.6, 14.6, 16);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_WASTE_BIN = styled("medical", "prop", "medical_waste_bin", MEDICAL_LOOK, 3.3, 0, 3.4, 12.7, 11.8, 13.2);
    public static final DeferredBlock<? extends LampBlock> SCIFI_CANISTER = styled("scifi", "glow", "scifi_canister", SCIFI_LOOK, 3.18, 0, 3.18, 12.82, 12.8, 12.82);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_PALLET = styled("mechanical", "prop", "mechanical_pallet", MECHANICAL_LOOK, 0.5, 0, 0.5, 15.5, 3.5, 15.5);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_PALLET_LOADED = styled("industrial", "prop", "industrial_pallet_loaded", INDUSTRIAL_LOOK, 0.5, 0, 0.5, 15.5, 13.9, 15.5);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_PALLET = styled("medical", "prop", "medical_pallet", MEDICAL_LOOK, 0.5, 0, 0.5, 15.5, 11.55, 15.5);
    public static final DeferredBlock<? extends LampBlock> SCIFI_HOVER_PALLET = styled("scifi", "glow", "scifi_hover_pallet", SCIFI_LOOK, 0.4, 0, 0.2, 15.6, 4.8, 15.8);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_PIPE_LAMP = styled("mechanical", "lamp", "mechanical_pipe_lamp", MECHANICAL_LOOK, 3.8, 0, 5, 12.2, 12.1, 15);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_GAUGE_LAMP = styled("mechanical", "lamp", "mechanical_gauge_lamp", MECHANICAL_LOOK, 2.5, 0, 2.5, 13.5, 15, 13.5);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_HIGHBAY_LAMP = styled("industrial", "lamp", "industrial_highbay_lamp", INDUSTRIAL_LOOK, 0.49, 5.4, 0.49, 15.51, 16, 15.51);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_WARNING_BEACON = styled("industrial", "lamp", "industrial_warning_beacon", INDUSTRIAL_LOOK, 4, 0, 4, 12, 10.2, 12);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_EXAM_LAMP = styled("medical", "lamp", "medical_exam_lamp", MEDICAL_LOOK, 3.63, 0, 2.98, 13.25, 11.4, 15.6);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_BED_LIGHT = styled("medical", "lamp", "medical_bed_light", MEDICAL_LOOK, 0.4, 0, 6.6, 15.6, 3, 9.9);
    public static final DeferredBlock<? extends LampBlock> SCIFI_ORB_LAMP = styled("scifi", "lamp", "scifi_orb_lamp", SCIFI_LOOK, 2.9, 0, 2.9, 13.1, 13.2, 13.1);
    public static final DeferredBlock<? extends LampBlock> SCIFI_HEX_LIGHT = styled("scifi", "lamp", "scifi_hex_light", SCIFI_LOOK, 0.4, 0, 0.4, 15.6, 1.8, 15.6);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_TOOL_SHELF = styled("mechanical", "shelf", "mechanical_tool_shelf", MECHANICAL_LOOK, 0.6, 0, 5, 15.4, 16, 11);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_WIRE_SHELF = styled("industrial", "shelf", "industrial_wire_shelf", INDUSTRIAL_LOOK, 0.6, 0, 5, 15.4, 16, 11);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_CABINET_SHELF = styled("medical", "shelf", "medical_cabinet_shelf", MEDICAL_LOOK, 0.6, 0, 5, 15.4, 16, 11.2);
    public static final DeferredBlock<? extends LampBlock> SCIFI_DISPLAY_SHELF = styled("scifi", "shelf_lit", "scifi_display_shelf", SCIFI_LOOK, 0.6, 0, 5, 15.4, 16, 11.2);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_BOILER = styled("mechanical", "prop", "mechanical_boiler", MECHANICAL_LOOK, 0.8, 0, 1.33, 15.2, 16, 15);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_CHEMICAL_DRUM = styled("industrial", "prop", "industrial_chemical_drum", INDUSTRIAL_LOOK, 2.7, 0, 2.7, 13.3, 13.8, 13.3);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_OXYGEN_TANKS = styled("medical", "prop", "medical_oxygen_tanks", MEDICAL_LOOK, 1.5, 0, 4.5, 14.5, 14.8, 11.5);
    public static final DeferredBlock<? extends LampBlock> SCIFI_PLASMA_TANK = styled("scifi", "glow", "scifi_plasma_tank", SCIFI_LOOK, 3.38, 0, 3.38, 12.62, 15, 12.62);
    public static final DeferredBlock<? extends LampBlock> MECHANICAL_GEAR_PALLET = styled("mechanical", "prop", "mechanical_gear_pallet", MECHANICAL_LOOK, 0.5, 0, 0.5, 15.5, 9, 15.5);
    public static final DeferredBlock<? extends LampBlock> INDUSTRIAL_DRUM_PALLET = styled("industrial", "prop", "industrial_drum_pallet", INDUSTRIAL_LOOK, 0.5, 0, 0.5, 15.5, 13, 15.5);
    public static final DeferredBlock<? extends LampBlock> MEDICAL_BAG_PALLET = styled("medical", "prop", "medical_bag_pallet", MEDICAL_LOOK, 0.5, 0, 0.5, 15.5, 10.05, 15.5);
    public static final DeferredBlock<? extends LampBlock> SCIFI_CARGO_CRATE = styled("scifi", "glow", "scifi_cargo_crate", SCIFI_LOOK, 0.5, 0, 0.5, 15.5, 12.7, 15.5);

    public static final List<DeferredBlock<? extends LightSwitchBlock>> SWITCHES = new ArrayList<>();

    public static final DeferredBlock<LightSwitchBlock> LIGHT_SWITCH = device("light_switch", LightSwitchBlock::new);
    public static final DeferredBlock<DimmerSwitchBlock> DIMMER_SWITCH = device("dimmer_switch", DimmerSwitchBlock::new);
    public static final DeferredBlock<MotionSensorBlock> MOTION_SENSOR = device("motion_sensor", MotionSensorBlock::new);
    public static final DeferredBlock<DuskSensorBlock> DUSK_SENSOR = device("dusk_sensor", DuskSensorBlock::new);
    public static final DeferredBlock<TimerSwitchBlock> TIMER_SWITCH = device("timer_switch", TimerSwitchBlock::new);

    public static final DeferredBlock<ImageMonitorBlock> IMAGE_MONITOR = BLOCKS.register("image_monitor",
            () -> new ImageMonitorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<TapeAnchorBlock> TAPE_ANCHOR = BLOCKS.register("tape_anchor",
            () -> new TapeAnchorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f)
                    .sound(SoundType.CHAIN)
                    .noOcclusion()));

    public static final DeferredBlock<com.sqdecor.block.cable.CableClipBlock> CABLE_CLIP = BLOCKS.register("cable_clip",
            () -> new com.sqdecor.block.cable.CableClipBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.3f)
                    .sound(SoundType.CHAIN)
                    .noOcclusion()));

    public static final List<DeferredBlock<com.sqdecor.block.cable.CableRackBlock>> CABLE_RACKS = new ArrayList<>();
    public static final DeferredBlock<com.sqdecor.block.cable.CableRackBlock> CABLE_RACK_2 = cableRack(2);
    public static final DeferredBlock<com.sqdecor.block.cable.CableRackBlock> CABLE_RACK_3 = cableRack(3);
    public static final DeferredBlock<com.sqdecor.block.cable.CableRackBlock> CABLE_RACK_5 = cableRack(5);
    public static final DeferredBlock<com.sqdecor.block.cable.CableRackBlock> CABLE_RACK_10 = cableRack(10);

    private static DeferredBlock<com.sqdecor.block.cable.CableRackBlock> cableRack(int slots) {
        DeferredBlock<com.sqdecor.block.cable.CableRackBlock> block = BLOCKS.register("cable_rack_" + slots,
                () -> new com.sqdecor.block.cable.CableRackBlock(slots, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(0.5f)
                        .sound(SoundType.CHAIN)
                        .noOcclusion()));
        CABLE_RACKS.add(block);
        return block;
    }

    public static final List<DeferredBlock<MarqueeBlock>> MARQUEES = new ArrayList<>();
    public static final DeferredBlock<MarqueeBlock> LED_MARQUEE = marquee("led_marquee");
    public static final DeferredBlock<MarqueeBlock> MARQUEE_FRAMED = marquee("marquee_framed");
    public static final DeferredBlock<MarqueeBlock> MARQUEE_HAZARD = marquee("marquee_hazard");
    public static final DeferredBlock<MarqueeBlock> MARQUEE_SLIM = marquee("marquee_slim");
    public static final DeferredBlock<MarqueeBlock> MARQUEE_BEZEL = marquee("marquee_bezel");
    public static final DeferredBlock<MarqueeBlock> MARQUEE_EXIT = marquee("marquee_exit");

    private static DeferredBlock<MarqueeBlock> marquee(String name) {
        DeferredBlock<MarqueeBlock> block = BLOCKS.register(name,
                () -> new MarqueeBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(1.0f)
                        .sound(SoundType.METAL)
                        .lightLevel(state -> state.getValue(MarqueeBlock.LIT) ? 6 : 0)
                        .noOcclusion()));
        MARQUEES.add(block);
        return block;
    }

    private static BlockBehaviour.Properties ledProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.5f)
                .lightLevel(state -> state.getValue(LedLogic.LIGHT_LEVEL))
                .noOcclusion();
    }

    public static final String[] INDUSTRIAL_TEXTURES = {"diamond_plate", "corrugated", "hazard", "riveted", "panels", "vent", "concrete", "checker",
            "wall_bricks", "big_bricks", "small_tiles", "large_tiles", "bolted_plates", "chevron", "small_checker", "smooth_panel", "dots", "cinder_blocks",
            "herringbone", "basket_weave", "thin_stripes", "rivet_strips", "wood_block_floor", "banded_bricks"};
    public static final List<DeferredBlock<? extends Block>> INDUSTRIAL = new ArrayList<>();

    static {
        for (String texture : INDUSTRIAL_TEXTURES) {
            INDUSTRIAL.add(BLOCKS.register("industrial_" + texture, () -> new IndustrialBlock(industrialProperties(false))));
        }
    }

    public static DeferredBlock<? extends Block> industrial(String texture) {
        return INDUSTRIAL.get(java.util.Arrays.asList(INDUSTRIAL_TEXTURES).indexOf(texture));
    }

    public static final String[] FENCE_STYLES = {"mechanical", "industrial", "medical", "scifi"};
    public static final List<DeferredBlock<StyleFenceBlock>> FENCES = new ArrayList<>();

    static {
        for (String style : FENCE_STYLES) {
            FENCES.add(BLOCKS.register(style + "_fence", () -> new StyleFenceBlock(industrialProperties(true))));
        }
    }

    private static BlockBehaviour.Properties industrialProperties(boolean thin) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(net.minecraft.world.level.block.SoundType.METAL);
        return thin ? properties.noOcclusion() : properties;
    }

    private static DeferredBlock<LampBlock> lamp(String name, double... base) {
        return register(BLOCKS.register(name, () -> new LampBlock(ledProperties(), "lamp/" + name, base)));
    }

    private static DeferredBlock<LampBlock> prop(String name, LedLook look, double... base) {
        DeferredBlock<LampBlock> block = register(BLOCKS.register(name,
                () -> new PropBlock(propProperties(), "lamp/" + name, base)));
        look(block, look, false);
        return block;
    }

    private static BlockBehaviour.Properties propProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5f)
                .sound(net.minecraft.world.level.block.SoundType.METAL)
                .noOcclusion();
    }

    private static DeferredBlock<LampBlock> lamp(String name, LedLook look, double... base) {
        DeferredBlock<LampBlock> block = lamp(name, base);
        look(block, look, true);
        return block;
    }

    private static DeferredBlock<LampBlock> glow(String name, LedLook look, double... base) {
        DeferredBlock<LampBlock> block = register(BLOCKS.register(name,
                () -> new GlowBlock(glowProperties(), "lamp/" + name, base)));
        look(block, look, true);
        return block;
    }

    private static BlockBehaviour.Properties glowProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.5f)
                .noOcclusion();
    }

    private static DeferredBlock<AnimatedStripBlock> animStrip(String name, StripLayout layout, double... base) {
        DeferredBlock<AnimatedStripBlock> block = register(BLOCKS.register(name,
                () -> new AnimatedStripBlock(ledProperties(), "lamp/" + name, base, layout)));
        look(block, LedLook.light(0x4DD0FF), true);
        return block;
    }

    private static DeferredBlock<SpotlightBlock> spot(String name, double... base) {
        return register(BLOCKS.register(name, () -> new SpotlightBlock(ledProperties(), "lamp/" + name, base)));
    }

    private static final double[] SHELF_BASE = {0.6, 0, 5, 15.4, 16, 11};

    private static BlockBehaviour.Properties shelfProperties(boolean lit) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.5f)
                .sound(SoundType.WOOD)
                .lightLevel(state -> lit ? state.getValue(LedLogic.LIGHT_LEVEL) : 0)
                .noOcclusion();
    }

    private static DeferredBlock<? extends LampBlock> styled(String style, String kind, String name, LedLook look, double... base) {
        DeferredBlock<? extends LampBlock> block = switch (kind) {
            case "lamp" -> lamp(name, look, base);
            case "prop" -> prop(name, look, base);
            case "glow" -> glow(name, look, base);
            default -> {
                DeferredBlock<ShelfBlock> shelf = shelf(name, kind.equals("shelf_lit"));
                look(shelf, look, kind.equals("shelf_lit"));
                yield shelf;
            }
        };
        STYLE_MODELS.computeIfAbsent(style, s -> new ArrayList<>()).add(block);
        return block;
    }

    private static DeferredBlock<ShelfBlock> shelf(String name, boolean lit) {
        return register(BLOCKS.register(name, () -> new ShelfBlock(shelfProperties(lit), "lamp/" + name, SHELF_BASE, lit, 3)));
    }

    private static DeferredBlock<TallShelfBlock> tallShelf(String name, boolean lit) {
        return register(BLOCKS.register(name, () -> new TallShelfBlock(shelfProperties(lit), "lamp/" + name, SHELF_BASE, lit)));
    }

    private static DeferredBlock<BeaconLampBlock> beacon(String name, int rgb) {
        DeferredBlock<BeaconLampBlock> block = register(BLOCKS.register(name,
                () -> new BeaconLampBlock(ledProperties(), "lamp/beacon", new double[]{4.5, 0, 4.5, 11.5, 8, 11.5})));
        look(block, LedLook.light(rgb), true);
        return block;
    }

    private static void look(DeferredBlock<? extends LampBlock> block, LedLook look, boolean lit) {
        int rgb = lit ? look.rgb() & 0xFFFFFF : 0xFFFFFF;
        if (rgb != 0xFFFFFF) LAMP_LOOKS.put(block, LedLook.light(rgb));
    }

    private static <T extends LampBlock> DeferredBlock<T> register(DeferredBlock<T> lamp) {
        LAMPS.add(lamp);
        return lamp;
    }

    private static <T extends LightSwitchBlock> DeferredBlock<T> device(String name,
                                                                       Function<BlockBehaviour.Properties, T> factory) {
        DeferredBlock<T> block = BLOCKS.register(name, () -> factory.apply(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.5f)
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion()));
        SWITCHES.add(block);
        return block;
    }

}
