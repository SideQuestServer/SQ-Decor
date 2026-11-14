package com.sqdecor.datagen;

import java.util.concurrent.CompletableFuture;

import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.LED_BULB.get(), 2)
                .pattern("P")
                .pattern("G")
                .pattern("N")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LED_TUBE.get(), 2)
                .pattern("PPP")
                .pattern("NGN")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LED_PANEL.get(), 2)
                .pattern("PPP")
                .pattern("GGG")
                .pattern("NNN")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.LED_TUNER.get())
                .pattern(" G ")
                .pattern("NRN")
                .pattern(" I ")
                .define('G', Items.GLOWSTONE_DUST)
                .define('N', Items.IRON_NUGGET)
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LED_STRIP.get(), 8)
                .pattern("PPP")
                .pattern("GNG")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LED_CORNER.get())
                .requires(ModBlocks.LED_STRIP.get())
                .unlockedBy("has_led_strip", has(ModBlocks.LED_STRIP.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LED_STRIP.get())
                .requires(ModBlocks.LED_CORNER.get())
                .unlockedBy("has_led_corner", has(ModBlocks.LED_CORNER.get()))
                .save(output, "sqdecor:led_strip_from_corner");

        stonecut(output, ModBlocks.LED_STRIP.get(), ModBlocks.LED_STRIP_CORNER_L.get(), ModBlocks.LED_STRIP_CROSS.get(),
                ModBlocks.LED_MIDDLE.get(), ModBlocks.LED_MIDDLE_HIGH.get());
        stonecut(output, ModBlocks.LED_TUBE.get(), ModBlocks.LED_STRIP_1.get());
        stonecut(output, ModBlocks.LED_TUBE.get(), ModBlocks.STRIP_LIGHT.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.CEILING_ROUND.get(), ModBlocks.CEILING_SQUARE.get(),
                ModBlocks.CEILING_RING.get(), ModBlocks.LED_PANEL_ROUND.get());
        stonecut(output, ModBlocks.SPOTLIGHT.get(), ModBlocks.DOWNLIGHT.get(), ModBlocks.FLOODLIGHT.get(),
                ModBlocks.TRACK_SPOTS.get(), ModBlocks.TRIPLE_SPOTS.get());
        stonecut(output, ModItems.LED_BULB.get(), ModBlocks.GLOBE_LAMP.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.LED_PANEL_BEVEL.get(), ModBlocks.LED_PANEL_LAMELLA.get(),
                ModBlocks.LED_PANEL_BEVEL_LAMELLA.get(), ModBlocks.LED_PANEL_SQUARE.get());

        combine(output, ModBlocks.LED_STRIP_1_MIDDLE.get(), ModBlocks.LED_STRIP_1.get(), "AA");
        combine(output, ModBlocks.LED_STRIP_1_LONG.get(), ModBlocks.LED_STRIP_1.get(), "AAA");
        combine(output, ModBlocks.LED_STRIP_2.get(), ModBlocks.LED_STRIP_1.get(), "A", "A");
        combine(output, ModBlocks.LED_STRIP_2_MIDDLE.get(), ModBlocks.LED_STRIP_2.get(), "AA");
        combine(output, ModBlocks.LED_STRIP_2_LONG.get(), ModBlocks.LED_STRIP_2.get(), "AAA");
        combine(output, ModBlocks.LED_PANEL_MIDDLE.get(), ModBlocks.LED_PANEL.get(), "AA");
        combine(output, ModBlocks.LED_PANEL_LONG.get(), ModBlocks.LED_PANEL.get(), "AAA");
        combine(output, ModBlocks.LED_PANEL_SQUARE_MIDDLE.get(), ModBlocks.LED_PANEL_SQUARE.get(), "AA", "AA");
        combine(output, ModBlocks.LED_PANEL_SQUARE_BIG.get(), ModBlocks.LED_PANEL_SQUARE.get(), "AAA", "AAA", "AAA");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModItems.LED_CAGE_BULB.get())
                .requires(ModItems.LED_BULB.get())
                .requires(Items.IRON_BARS)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HANG_LAMP.get())
                .requires(ModItems.LED_BULB.get())
                .requires(Items.CHAIN)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WALL_LAMP.get())
                .requires(ModItems.LED_BULB.get())
                .requires(Items.IRON_NUGGET, 2)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.industrial("diamond_plate").get(), 8)
                .requires(Items.IRON_INGOT, 4)
                .requires(Items.STONE)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
        for (String texture : ModBlocks.INDUSTRIAL_TEXTURES) {
            Item block = ModBlocks.industrial(texture).get().asItem();
            if (!texture.equals("diamond_plate")) {
                stonecutAs(output, ModBlocks.industrial("diamond_plate").get(), block);
            }
        }

        styles(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.LIGHT_SWITCH.get())
                .requires(Items.STONE_BUTTON)
                .requires(Items.IRON_NUGGET)
                .requires(Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        device(output, ModBlocks.DIMMER_SWITCH.get(), Items.COMPARATOR);
        device(output, ModBlocks.MOTION_SENSOR.get(), Items.OBSERVER);
        device(output, ModBlocks.DUSK_SENSOR.get(), Items.DAYLIGHT_DETECTOR);
        device(output, ModBlocks.TIMER_SWITCH.get(), Items.CLOCK);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SPOTLIGHT.get())
                .pattern("IPI")
                .pattern("IBI")
                .pattern(" N ")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.GLASS_PANE)
                .define('B', ModItems.LED_BULB.get())
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);
        beacon(output, ModBlocks.BEACON_BLUE.get(), Items.BLUE_DYE);
        beacon(output, ModBlocks.BEACON_RED.get(), Items.RED_DYE);
        beacon(output, ModBlocks.BEACON_ORANGE.get(), Items.ORANGE_DYE);

        stonecut(output, ModItems.LED_CAGE_BULB.get(), ModBlocks.INDUSTRIAL_PENDANT.get(), ModBlocks.INDUSTRIAL_CAGE.get(),
                ModBlocks.HIGH_BAY.get(), ModBlocks.BULKHEAD.get(), ModBlocks.STACK_LIGHT.get());
        stonecut(output, ModBlocks.SPOTLIGHT.get(), ModBlocks.WORK_LIGHT.get(), ModBlocks.WORK_LIGHT_TALL.get(), ModBlocks.WORK_LIGHT_SMALL.get());
        stonecut(output, ModItems.LED_BULB.get(), ModBlocks.LAB_LAMP.get());
        stonecut(output, ModBlocks.LED_TUBE.get(), ModBlocks.LAB_TUBE.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.EXIT_SIGN_LEFT.get(), ModBlocks.EXIT_SIGN_RIGHT.get(), ModBlocks.EXIT_SIGN_DOWN.get(), ModBlocks.ASSEMBLY_POINT_SIGN.get(), ModBlocks.SIGN_EXIT.get(), ModBlocks.SIGN_WARNING.get(), ModBlocks.SIGN_ARROW.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.SIGN_NO_ENTRY.get(), ModBlocks.SIGN_STOP.get(), ModBlocks.SIGN_EXCLAMATION.get(), ModBlocks.EMERGENCY_LIGHT.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.ANIM_STRIP.get(), ModBlocks.ANIM_STRIP_THIN.get(), ModBlocks.ANIM_STRIP_DOUBLE.get(), ModBlocks.ANIM_STRIP_CORNER.get(), ModBlocks.ANIM_RING.get());
        stonecut(output, ModBlocks.industrial("diamond_plate").get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.BUTTON_ROUND.get(), ModBlocks.BUTTON_BIG_RED.get(), ModBlocks.BUTTON_PAIR.get(), ModBlocks.BUTTON_GRID.get(), ModBlocks.TOGGLE_PANEL.get(), ModBlocks.KEY_SWITCH.get(), ModBlocks.KEYPAD.get(), ModBlocks.CARD_READER.get(), ModBlocks.INTERCOM.get(), ModBlocks.FINGERPRINT_SCANNER.get(), ModBlocks.DIAL_PANEL.get(), ModBlocks.SLIDER_PANEL.get(), ModBlocks.STATUS_BOARD.get(), ModBlocks.OSCILLOSCOPE.get(), ModBlocks.SERVER_RACK_BLADES.get(), ModBlocks.NETWORK_SWITCH.get(), ModBlocks.PATCH_PANEL.get(), ModBlocks.UPS_UNIT.get(), ModBlocks.LAPTOP_CLOSED.get(), ModBlocks.TABLET_STAND.get(), ModBlocks.DESK_TERMINAL.get(), ModBlocks.MONITOR_WALL.get(), ModBlocks.CLOCK_DIGITAL.get(), ModBlocks.HOLOGRAM_PROJECTOR.get(), ModBlocks.ALARM_HORN.get(), ModBlocks.DOOR_STATUS_LIGHT.get(), ModBlocks.HAZARD_LIGHT_BAR.get());
        stonecut(output, ModBlocks.industrial("diamond_plate").get(), ModBlocks.SKIRTING.get(), ModBlocks.WALL_TRIM.get(), ModBlocks.FLOOR_GRATE.get(), ModBlocks.FLOOR_HATCH.get(), ModBlocks.WALL_VENT.get(), ModBlocks.TRAFFIC_CONE.get(), ModBlocks.PYLON_TALL.get(), ModBlocks.BARRIER_POST.get(), ModBlocks.BARRIER_POST_CHAIN.get(), ModBlocks.BARRIER_GATE.get(), ModBlocks.BARRIER_FENCE.get(), ModBlocks.JERSEY_BARRIER.get(), ModBlocks.BARREL.get(), ModBlocks.RAILING.get(), ModBlocks.LADDER.get(), ModBlocks.HAZARD_STRIP.get(), ModBlocks.BOLLARD.get(), ModBlocks.PALLET.get(), ModBlocks.CRATE.get(), ModBlocks.WORKBENCH.get(), ModBlocks.TOOL_WALL.get(), ModBlocks.OFFICE_CHAIR.get(), ModBlocks.OFFICE_DESK.get(), ModBlocks.FILING_CABINET.get(), ModBlocks.TOOL_CABINET.get());
        stonecut(output, ModBlocks.LED_BULB.get(), ModBlocks.CONSTRUCTION_BLINKER.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.DESKTOP_MONITOR.get(), ModBlocks.CRT_MONITOR.get(), ModBlocks.LAPTOP.get(), ModBlocks.TOWER_PC.get(), ModBlocks.SERVER_RACK.get(), ModBlocks.KEYBOARD.get(), ModBlocks.FRAMED_GRAPH.get(), ModBlocks.FRAMED_MAP.get(), ModBlocks.FRAMED_STATUS.get(), ModBlocks.WALL_SCREEN.get(), ModBlocks.WALL_TERMINAL.get(), ModBlocks.RADAR_SCREEN.get(), ModBlocks.BLINKEN_PANEL.get());
        stonecut(output, ModBlocks.LED_PANEL.get(), ModBlocks.SIGN_PARKING.get(), ModBlocks.SIGN_INFO.get(), ModBlocks.SIGN_FIRST_AID.get(), ModBlocks.SIGN_ELECTRIC.get(), ModBlocks.SIGN_OK.get(), ModBlocks.SIGN_CROSS.get(), ModBlocks.SEVEN_SEGMENT.get(), ModBlocks.DOT_DISPLAY.get(), ModBlocks.ARROW_SIGNAL.get(), ModBlocks.CROSS_SIGNAL.get(), ModBlocks.BAR_DISPLAY.get());
        stonecut(output, ModBlocks.LED_BULB.get(), ModBlocks.STATUS_LIGHT.get(), ModBlocks.SIGNAL_POST.get());
        stonecut(output, ModBlocks.LED_CAGE_BULB.get(), ModBlocks.WALL_CAGE.get());
        stonecut(output, ModBlocks.LED_TUBE.get(), ModBlocks.TUBE_PENDANT.get());
        stonecut(output, ModBlocks.LED_BULB.get(), ModBlocks.TRAFFIC_LIGHT.get(), ModBlocks.FLOOR_LAMP.get(), ModBlocks.FLASK_LAMP.get());
        stonecut(output, ModBlocks.LED_STRIP.get(), ModBlocks.NEON_RING.get());
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SHELF.get())
                .requires(ModBlocks.LED_STRIP.get())
                .requires(Ingredient.of(ItemTags.WOODEN_SLABS), 4)
                .unlockedBy("has_led_strip", has(ModBlocks.LED_STRIP.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SHELF_PLAIN.get())
                .requires(Ingredient.of(ItemTags.WOODEN_SLABS), 4)
                .requires(Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SHELF_TALL.get())
                .requires(ModBlocks.SHELF.get())
                .requires(Ingredient.of(ItemTags.WOODEN_SLABS), 2)
                .unlockedBy("has_shelf", has(ModBlocks.SHELF.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SHELF_TALL_PLAIN.get())
                .requires(ModBlocks.SHELF_PLAIN.get())
                .requires(Ingredient.of(ItemTags.WOODEN_SLABS), 2)
                .unlockedBy("has_shelf_plain", has(ModBlocks.SHELF_PLAIN.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WALL_SHELF.get())
                .requires(ModBlocks.LED_STRIP.get())
                .requires(ItemTags.WOODEN_SLABS)
                .unlockedBy("has_led_strip", has(ModBlocks.LED_STRIP.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.LED_REMOTE.get())
                .requires(Items.STONE_BUTTON)
                .requires(Items.IRON_INGOT)
                .requires(Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.THIN_CABLE.get(), 8)
                .requires(Items.COPPER_INGOT)
                .requires(Items.STRING)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        int[][] rackParts = {{2, 1, 1}, {3, 1, 2}, {5, 1, 4}, {10, 2, 4}};
        for (int[] part : rackParts) {
            var rack = ModBlocks.CABLE_RACKS.stream().filter(r -> r.get().slots() == part[0]).findFirst().orElseThrow();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, rack.get())
                    .requires(Items.IRON_INGOT, part[1])
                    .requires(Items.IRON_NUGGET, part[2])
                    .unlockedBy("has_thin_cable", has(ModItems.THIN_CABLE.get()))
                    .save(output);
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CAUTION_TAPE.get(), 4)
                .pattern("YKY")
                .pattern("KYK")
                .pattern("YKY")
                .define('Y', Items.YELLOW_DYE)
                .define('K', Items.BLACK_DYE)
                .unlockedBy("has_yellow_dye", has(Items.YELLOW_DYE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.IMAGE_MONITOR.get(), 4)
                .pattern("III")
                .pattern("PLR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.GLASS_PANE)
                .define('L', ModItems.LED_BULB.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.LED_MARQUEE.get(), 2)
                .pattern("PPP")
                .pattern("GKG")
                .pattern("NNN")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('K', Items.BLACK_DYE)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST))
                .save(output);
        stonecut(output, ModItems.LED_MARQUEE.get(), ModItems.MARQUEE_FRAMED.get(), ModItems.MARQUEE_HAZARD.get(),
                ModItems.MARQUEE_SLIM.get(), ModItems.MARQUEE_BEZEL.get(), ModItems.MARQUEE_EXIT.get());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HANDBOOK.get())
                .requires(Items.BOOK)
                .requires(ModItems.LED_BULB.get())
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);
    }

    private static void styles(RecipeOutput output) {
        ItemLike[] materials = {Items.COPPER_INGOT, ModBlocks.industrial("diamond_plate").get(), Items.WHITE_CONCRETE, Items.AMETHYST_SHARD};
        for (int i = 0; i < materials.length; i++) {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FENCES.get(i).get(), 6)
                    .pattern("#I#").pattern("#I#")
                    .define('#', materials[i]).define('I', Items.IRON_INGOT)
                    .unlockedBy("has_" + name(materials[i]), has(materials[i])).save(output);
            for (var model : ModBlocks.STYLE_MODELS.getOrDefault(ModBlocks.FENCE_STYLES[i], java.util.List.of())) {
                boolean lit = model.get().hasLight() && !(model.get() instanceof com.sqdecor.block.lamp.ShelfBlock);
                stonecut(output, lit ? ModItems.LED_BULB.get() : ModBlocks.FENCES.get(i).get(), model.get());
            }
        }
    }

    private static void stonecut(RecipeOutput output, ItemLike from, ItemLike... results) {
        for (ItemLike result : results) {
            SingleItemRecipeBuilder.stonecutting(Ingredient.of(from), RecipeCategory.BUILDING_BLOCKS, result)
                    .unlockedBy("has_" + name(from), has(from))
                    .save(output);
        }
    }

    private static void combine(RecipeOutput output, ItemLike result, ItemLike from, String... pattern) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result);
        for (String row : pattern) builder.pattern(row);
        builder.define('A', from)
                .unlockedBy("has_" + name(from), has(from))
                .save(output);
    }

    private static void stonecutAs(RecipeOutput output, ItemLike from, ItemLike result) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(from), RecipeCategory.BUILDING_BLOCKS, result)
                .unlockedBy("has_" + name(from), has(from))
                .save(output, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.sqdecor.SQDecor.MODID,
                        name(result) + "_from_" + name(from)));
    }

    private static String name(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    private static void beacon(RecipeOutput output, ItemLike beacon, ItemLike dye) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, beacon)
                .requires(ModItems.LED_BULB.get())
                .requires(Items.GLASS)
                .requires(Items.IRON_INGOT)
                .requires(dye)
                .unlockedBy("has_led_bulb", has(ModItems.LED_BULB.get()))
                .save(output);
    }

    private static void device(RecipeOutput output, ItemLike device, ItemLike extra) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, device)
                .requires(ModBlocks.LIGHT_SWITCH.get())
                .requires(extra)
                .unlockedBy("has_light_switch", has(ModBlocks.LIGHT_SWITCH.get()))
                .save(output);
    }

}
