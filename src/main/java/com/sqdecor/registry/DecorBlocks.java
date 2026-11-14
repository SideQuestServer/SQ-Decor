package com.sqdecor.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.sqdecor.block.cable.CableBlock;
import com.sqdecor.block.control.EmergencyStopBlock;
import com.sqdecor.block.control.IndustrialLeverBlock;
import com.sqdecor.block.control.SwitchCabinetBlock;
import com.sqdecor.block.walkway.WalkwayBlock;
import com.sqdecor.block.frame.FrameBlock;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import com.sqdecor.block.industrial.Industrial;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class DecorBlocks {
    private DecorBlocks() {}

    public static final List<DeferredBlock<? extends Block>> DYED = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();
    public static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();
    public static final Map<DeferredBlock<? extends Block>, List<Property<?>>> KEEP = new LinkedHashMap<>();
    public static final List<DeferredBlock<? extends Block>> BY_HAND = new ArrayList<>();
    public static final List<DeferredBlock<? extends Block>> NO_DROP = new ArrayList<>();

    public static final String[] FRAME_NAMES = {"frame_panel", "frame_riveted", "frame_inset", "frame_grid"};
    public static final List<DeferredBlock<FrameBlock>> FRAMES = new ArrayList<>();
    static {
        for (String name : FRAME_NAMES) {
            FRAMES.add(dyed(name, () -> new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops())));
        }
    }

    public static final DeferredBlock<WalkwayBlock> WALKWAY = dyed("grating_walkway", () -> new WalkwayBlock(metal()));

    public static final DeferredBlock<IndustrialLeverBlock> INDUSTRIAL_LEVER = dyed("industrial_lever",
            () -> new IndustrialLeverBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.METAL).noCollission()));
    public static final DeferredBlock<IndustrialLeverBlock> ROTARY_SWITCH = dyed("rotary_switch",
            () -> new IndustrialLeverBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.METAL).noCollission()));
    public static final DeferredBlock<EmergencyStopBlock> EMERGENCY_STOP = block("emergency_stop",
            () -> new EmergencyStopBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0f).noCollission()));
    public static final DeferredBlock<SwitchCabinetBlock> SWITCH_CABINET = dyed("switch_cabinet",
            () -> new SwitchCabinetBlock(metal(), false));
    public static final DeferredBlock<SwitchCabinetBlock> FUSE_BOX = dyed("fuse_box",
            () -> new SwitchCabinetBlock(metal(), true));

    public static final List<DeferredBlock<CableBlock>> CABLES = new ArrayList<>();
    public static final DeferredBlock<CableBlock> CABLE_1 = cable(1);
    public static final DeferredBlock<CableBlock> CABLE_2 = cable(2);
    public static final DeferredBlock<CableBlock> CABLE_3 = cable(3);
    public static final DeferredBlock<CableBlock> CABLE_1_ANCHOR = cable("cable_1_anchor");
    public static final DeferredBlock<CableBlock> CABLE_2_ANCHOR = cable("cable_2_anchor");
    public static final DeferredBlock<CableBlock> CABLE_3_ANCHOR = cable("cable_3_anchor");

    private static DeferredBlock<CableBlock> cable(int wires) {
        return cable("cable_" + wires);
    }

    private static DeferredBlock<CableBlock> cable(String name) {
        DeferredBlock<CableBlock> holder = dyed(name, () -> new CableBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL).strength(0.2f).sound(SoundType.WOOL).noCollission().noOcclusion()));
        BY_HAND.add(holder);
        CABLES.add(holder);
        return holder;
    }

    public static void init() {}

    static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> block) {
        DeferredBlock<T> holder = ModBlocks.BLOCKS.register(name, block);
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(holder));
        ALL.add(holder);
        return holder;
    }

    static <T extends Block> DeferredBlock<T> dyed(String name, Supplier<T> block) {
        DeferredBlock<T> holder = block(name, block);
        DYED.add(holder);
        keep(holder, Industrial.COLOR);
        return holder;
    }

    static void keep(DeferredBlock<? extends Block> holder, Property<?> property) {
        KEEP.computeIfAbsent(holder, h -> new ArrayList<>()).add(property);
    }

    static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion();
    }
}
