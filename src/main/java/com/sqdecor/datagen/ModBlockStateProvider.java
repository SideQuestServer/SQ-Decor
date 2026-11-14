package com.sqdecor.datagen;

import com.sqdecor.SQDecor;
import com.sqdecor.block.industrial.Industrial;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CrossCollisionBlock;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.block.led.LedCageBulbBlock;
import com.sqdecor.block.marquee.MarqueeBlock;
import com.sqdecor.block.monitor.ImageMonitorBlock;
import com.sqdecor.block.switches.LightSwitchBlock;
import com.sqdecor.led.LedLogic;
import com.sqdecor.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, SQDecor.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        facingBlock(ModBlocks.LED_BULB.get(), blockbench("led/led_bulb"));
        facingRotated(ModBlocks.LED_CAGE_BULB.get(), "led/led_cage_bulb", LedCageBulbBlock.ROTATED);

        for (DeferredBlock<? extends LampBlock> lamp : ModBlocks.LAMPS) {
            facingBlock(lamp.get(), blockbench(lamp.get().model()), LampBlock.TURN);
        }

        simpleBlock(ModBlocks.SHELF_TOP.get(), blockbench("lamp/shelf_blank"));

        for (String texture : ModBlocks.INDUSTRIAL_TEXTURES) {
            simpleBlock(ModBlocks.industrial(texture).get(), blockbench("industrial/industrial_" + texture));
        }
        ModBlocks.FENCES.forEach(fence -> fence(fence.get(), fence.getId().getPath()));

        ModBlocks.SWITCHES.forEach(device -> lightSwitch(device.get(), device.getId().getPath()));
        marquee();
        monitor();
        tapeAnchor();
        cableClip();
        cableRacks();
    }

    private void fence(Block block, String name) {
        var builder = getMultipartBuilder(block);
        builder.part().modelFile(blockbench("style/" + name + "_post")).addModel().end();
        ModelFile side = blockbench("style/" + name + "_side");
        builder.part().modelFile(side).uvLock(true).addModel().condition(CrossCollisionBlock.NORTH, true).end();
        builder.part().modelFile(side).rotationY(90).uvLock(true).addModel().condition(CrossCollisionBlock.EAST, true).end();
        builder.part().modelFile(side).rotationY(180).uvLock(true).addModel().condition(CrossCollisionBlock.SOUTH, true).end();
        builder.part().modelFile(side).rotationY(270).uvLock(true).addModel().condition(CrossCollisionBlock.WEST, true).end();
    }

    private ModelFile blockbench(String path) {
        return models().getExistingFile(modLoc("block/" + path));
    }

    private ModelFile rotated(String path) {
        return new ModelFile.UncheckedModelFile(modLoc("block/" + path + "_rot"));
    }

    private static int yFor(Direction horizontal) {
        return ((int) horizontal.toYRot() + 180) % 360;
    }

    private void facingBlock(Block block, ModelFile model, Property<?>... ignored) {
        getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            return facing(model, dir);
        }, withLightLevel(ignored));
    }

    private void facingRotated(Block block, String path, BooleanProperty rotatedProperty) {
        ModelFile normal = blockbench(path);
        ModelFile turned = rotated(path);
        getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            return facing(state.getValue(rotatedProperty) ? turned : normal, dir);
        }, withLightLevel());
    }

    private static ConfiguredModel[] facing(ModelFile model, Direction dir) {
        int x = switch (dir) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 90;
        };
        int y = dir.getAxis().isHorizontal() ? yFor(dir) : 0;
        return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
    }

    private static Property<?>[] withLightLevel(Property<?>... ignored) {
        Property<?>[] skip = new Property<?>[ignored.length + 1];
        skip[0] = LedLogic.LIGHT_LEVEL;
        System.arraycopy(ignored, 0, skip, 1, ignored.length);
        return skip;
    }

    private void lightSwitch(Block block, String name) {
        ModelFile off = blockbench(name);
        ModelFile on = blockbench(name + "_on");
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(LightSwitchBlock.FACING);
            ModelFile model = state.getValue(LightSwitchBlock.POWERED) ? on : off;
            return switch (state.getValue(LightSwitchBlock.FACE)) {
                case FLOOR -> ConfiguredModel.builder().modelFile(model).rotationY(yFor(facing)).build();
                case WALL -> ConfiguredModel.builder().modelFile(model).rotationX(90).rotationY(yFor(facing)).uvLock(true).build();
                case CEILING -> ConfiguredModel.builder().modelFile(model).rotationX(180)
                        .rotationY((yFor(facing) + 180) % 360).build();
            };
        });
    }

    private void tapeAnchor() {
        ModelFile model = blockbench("tape_anchor");
        getVariantBuilder(ModBlocks.TAPE_ANCHOR.get()).forAllStates(state -> {
            net.minecraft.core.Direction d = state.getValue(com.sqdecor.block.tape.TapeAnchorBlock.FACING);
            ConfiguredModel.Builder<?> b = ConfiguredModel.builder().modelFile(model);
            if (d == net.minecraft.core.Direction.UP) b.rotationX(270);
            else if (d == net.minecraft.core.Direction.DOWN) b.rotationX(90);
            else b.rotationY(yFor(d));
            return b.build();
        });
    }

    private void cableClip() {
        ModelFile model = blockbench("cable_clip");
        getVariantBuilder(ModBlocks.CABLE_CLIP.get()).forAllStates(state -> {
            net.minecraft.core.Direction d = state.getValue(com.sqdecor.block.tape.TapeAnchorBlock.FACING);
            ConfiguredModel.Builder<?> b = ConfiguredModel.builder().modelFile(model);
            if (d == net.minecraft.core.Direction.UP) b.rotationX(270);
            else if (d == net.minecraft.core.Direction.DOWN) b.rotationX(90);
            else b.rotationY(yFor(d));
            return b.build();
        });
    }

    private void cableRacks() {
        for (var rack : ModBlocks.CABLE_RACKS) {
            ModelFile flat = blockbench(rack.getId().getPath());
            ModelFile turned = blockbench(rack.getId().getPath() + "_turned");
            getVariantBuilder(rack.get()).forAllStates(state -> {
                Direction d = state.getValue(com.sqdecor.block.cable.CableRackBlock.FACING);
                ConfiguredModel.Builder<?> b = ConfiguredModel.builder()
                        .modelFile(state.getValue(com.sqdecor.block.cable.CableRackBlock.TURNED) ? turned : flat);
                if (d == Direction.UP) b.rotationX(270);
                else if (d == Direction.DOWN) b.rotationX(90);
                else b.rotationY(yFor(d));
                return b.build();
            });
        }
    }

    private void monitor() {
        ModelFile model = blockbench("image_monitor");
        getVariantBuilder(ModBlocks.IMAGE_MONITOR.get()).forAllStates(state -> {
            int[] rot = ImageMonitorBlock.modelRotation(state.getValue(ImageMonitorBlock.FACING), state.getValue(ImageMonitorBlock.FACE));
            return ConfiguredModel.builder().modelFile(model).rotationX(rot[0]).rotationY(rot[1]).build();
        });
    }

    private void marquee() {
        for (var block : ModBlocks.MARQUEES) {
            ModelFile model = blockbench(block.getId().getPath());
            getVariantBuilder(block.get()).forAllStatesExcept(state ->
                    ConfiguredModel.builder().modelFile(model).rotationY(yFor(state.getValue(MarqueeBlock.FACING))).build(),
                    MarqueeBlock.LIT);
        }
    }
}
