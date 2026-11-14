package com.sqdecor.registry;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.block.entity.ImageMonitorBlockEntity;
import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SQDecor.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LedBlockEntity>> LED =
            BLOCK_ENTITIES.register("led_tube", () -> BlockEntityType.Builder.of(LedBlockEntity::new,
                    ModBlocks.LED_BULB.get(),
                    ModBlocks.LED_CAGE_BULB.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LampBlockEntity>> LAMP =
            BLOCK_ENTITIES.register("lamp", () -> BlockEntityType.Builder.of(LampBlockEntity::new,
                    ModBlocks.LAMPS.stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightSwitchBlockEntity>> LIGHT_SWITCH =
            BLOCK_ENTITIES.register("light_switch", () -> BlockEntityType.Builder.of(LightSwitchBlockEntity::new,
                    ModBlocks.SWITCHES.stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.sqdecor.block.entity.PaintBlockEntity>> PAINT =
            BLOCK_ENTITIES.register("paint", () -> BlockEntityType.Builder.of(com.sqdecor.block.entity.PaintBlockEntity::new,
                    java.util.stream.Stream.concat(ModBlocks.INDUSTRIAL.stream(), ModBlocks.FENCES.stream())
                            .map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MarqueeBlockEntity>> MARQUEE =
            BLOCK_ENTITIES.register("marquee", () -> BlockEntityType.Builder.of(MarqueeBlockEntity::new,
                    ModBlocks.MARQUEES.stream().map(b -> (net.minecraft.world.level.block.Block) b.get())
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TapeAnchorBlockEntity>> TAPE_ANCHOR =
            BLOCK_ENTITIES.register("tape_anchor", () -> BlockEntityType.Builder.of(TapeAnchorBlockEntity::new,
                    ModBlocks.TAPE_ANCHOR.get(), ModBlocks.CABLE_CLIP.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.sqdecor.block.entity.CableRackBlockEntity>> CABLE_RACK =
            BLOCK_ENTITIES.register("cable_rack", () -> BlockEntityType.Builder.of(com.sqdecor.block.entity.CableRackBlockEntity::new,
                    ModBlocks.CABLE_RACKS.stream().map(b -> (net.minecraft.world.level.block.Block) b.get())
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImageMonitorBlockEntity>> IMAGE_MONITOR =
            BLOCK_ENTITIES.register("image_monitor", () -> BlockEntityType.Builder.of(ImageMonitorBlockEntity::new,
                    ModBlocks.IMAGE_MONITOR.get()
            ).build(null));
}
