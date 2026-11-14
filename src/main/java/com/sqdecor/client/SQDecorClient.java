package com.sqdecor.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.industrial.Industrial;
import com.sqdecor.client.model.LampGeometryLoader;
import com.sqdecor.client.render.GlowRenderer;
import com.sqdecor.client.render.LedRenderer;
import com.sqdecor.client.render.MarqueeFont;
import com.sqdecor.client.render.ImageMonitorRenderer;
import com.sqdecor.client.render.TapeRenderer;
import com.sqdecor.client.render.MarqueeRenderer;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLook;
import com.sqdecor.registry.ModBlockEntities;
import com.sqdecor.registry.ModBlocks;
import com.sqdecor.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

@Mod(value = SQDecor.MODID, dist = Dist.CLIENT)
public class SQDecorClient {
    public SQDecorClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(SQDecorClient::registerBlockColors);
        modBus.addListener(SQDecorClient::registerItemColors);
        modBus.addListener(SQDecorClient::registerIndustrialBlockColors);
        modBus.addListener(SQDecorClient::registerIndustrialItemColors);
        modBus.addListener(SQDecorClient::registerModelLoaders);
        modBus.addListener(SQDecorClient::registerRenderers);
        modBus.addListener(SQDecorClient::registerReloadListeners);
        modBus.addListener((ModelEvent.BakingCompleted event) -> {
            GlowRenderer.clearCache();
            com.sqdecor.client.render.HotBulbs.clear();
            com.sqdecor.client.render.ChunkExtras.reload();
        });
        modBus.addListener(SQDecorClient::wrapLedModels);
        modBus.addListener(SQDecorClient::wrapTemplateModels);

        com.sqdecor.block.entity.LedBlockEntity.clientLoaded = com.sqdecor.client.render.ChunkExtras::lampLoaded;
        com.sqdecor.block.entity.LedBlockEntity.hotAllowed = com.sqdecor.client.render.HotBulbs::allowed;
        com.sqdecor.block.entity.TapeAnchorBlockEntity.clientChanged = com.sqdecor.client.render.ChunkExtras::anchorChanged;
        NeoForge.EVENT_BUS.addListener(com.sqdecor.client.render.ChunkExtras::onSection);
        NeoForge.EVENT_BUS.addListener(com.sqdecor.client.render.ChunkExtras::onClientTick);
        NeoForge.EVENT_BUS.addListener(PerfProbe::register);
        NeoForge.EVENT_BUS.addListener(PerfProbe::onClientTick);
        NeoForge.EVENT_BUS.addListener(PerfProbe::onFrame);

        NeoForge.EVENT_BUS.addListener(com.sqdecor.client.render.LateBuffers::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                com.sqdecor.client.render.ImageMonitorRenderer.releaseAll());
        NeoForge.EVENT_BUS.addListener(HoldUseClient::onInteract);
        NeoForge.EVENT_BUS.addListener(HoldUseClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SQDecorClient::lookTooltip);
        NeoForge.EVENT_BUS.addListener(CreativeTabBanners::onRender);
        NeoForge.EVENT_BUS.addListener(CreativeTabBanners::onTooltip);
        NeoForge.EVENT_BUS.addListener(CreativeTabBanners::onClick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LED.get(), LedRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAMP.get(), LedRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MARQUEE.get(), MarqueeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IMAGE_MONITOR.get(), ImageMonitorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TAPE_ANCHOR.get(), TapeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CABLE_RACK.get(), com.sqdecor.client.render.CableRackRenderer::new);
    }

    private static void wrapLedModels(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        var wrapped = new java.util.IdentityHashMap<net.minecraft.client.resources.model.BakedModel,
                com.sqdecor.client.model.HotBulbModel>();
        for (Block block : ledBlocks()) {
            for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
                var location = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
                var model = models.get(location);
                if (model != null && !(model instanceof com.sqdecor.client.model.HotBulbModel)) {
                    models.put(location, wrapped.computeIfAbsent(model, com.sqdecor.client.model.HotBulbModel::new));
                }
            }
        }
    }

    private static void wrapTemplateModels(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        var wrapped = new java.util.IdentityHashMap<net.minecraft.client.resources.model.BakedModel,
                com.sqdecor.client.model.TemplateModel>();
        Stream.concat(ModBlocks.INDUSTRIAL.stream(), ModBlocks.FENCES.stream()).map(d -> (Block) d.get()).forEach(block -> {
            for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
                var location = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
                var model = models.get(location);
                if (model != null && !(model instanceof com.sqdecor.client.model.TemplateModel)) {
                    models.put(location, wrapped.computeIfAbsent(model, com.sqdecor.client.model.TemplateModel::new));
                }
            }
        });
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MarqueeFont.INSTANCE);
    }

    private static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(LampGeometryLoader.ID, LampGeometryLoader.INSTANCE);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return -1;
            if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) return lamp.color(LampPart.byTintIndex(tintIndex));
            if (tintIndex != 0) return -1;
            return level.getBlockEntity(pos) instanceof LedBlockEntity be ? 0xFF000000 | be.getRgb() : -1;
        }, ledBlocks());
    }

    private static void registerIndustrialBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            int rgb = state.getValue(Industrial.COLOR).getTextureDiffuseColor();
            if (level != null && pos != null && state.getBlock() instanceof com.sqdecor.block.industrial.PaintedBlock
                    && level.getBlockEntity(pos) instanceof com.sqdecor.block.entity.PaintBlockEntity be) {
                if (be.template() != null) return -1;
                rgb = tintIndex == 1 ? be.accentColor() : be.baseColor();
            }
            return 0xFF000000 | rgb;
        },
                java.util.stream.Stream.of(ModBlocks.INDUSTRIAL, ModBlocks.FENCES,
                        com.sqdecor.registry.DecorBlocks.DYED)
                        .flatMap(java.util.List::stream).map(d -> (Block) d.get()).toArray(Block[]::new));
    }

    private static void registerIndustrialItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
            String name = props == null ? null : props.properties().get("color");
            DyeColor color = name == null ? DyeColor.WHITE : DyeColor.byName(name, DyeColor.WHITE);
            int rgb = color.getTextureDiffuseColor() & 0xFFFFFF;
            com.sqdecor.block.industrial.BlockPaint paint = stack.get(ModDataComponents.BLOCK_PAINT.get());
            if (paint != null) {
                if (paint.base() >= 0) rgb = paint.base();
                if (tintIndex == 1 && paint.accent() >= 0) rgb = paint.accent();
            }
            return 0xFF000000 | rgb;
        }, java.util.stream.Stream.of(ModBlocks.INDUSTRIAL, ModBlocks.FENCES,
                        com.sqdecor.registry.DecorBlocks.DYED)
                .flatMap(java.util.List::stream).map(b -> ((Block) b.get()).asItem()).toArray(Item[]::new));
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            LedLook look = stack.get(ModDataComponents.LED_LOOK.get());
            if (look == null) return -1;
            int rgb = switch (tintIndex) {
                case 0 -> look.rgb();
                case 1 -> look.caseRgb();
                case 2 -> look.case2Rgb();
                default -> -1;
            };
            return rgb < 0 ? -1 : 0xFF000000 | rgb;
        }, Arrays.stream(ledBlocks()).map(Block::asItem).toArray(Item[]::new));
    }

    private static Block[] ledBlocks() {
        return Stream.concat(Stream.of(ModBlocks.LED_BULB.get(), ModBlocks.LED_CAGE_BULB.get()),
                ModBlocks.LAMPS.stream().map(DeferredBlock::get)).toArray(Block[]::new);
    }

    private static void lookTooltip(ItemTooltipEvent event) {
        LedLook look = event.getItemStack().get(ModDataComponents.LED_LOOK.get());
        if (look == null) return;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(line(LampPart.BULB, Component.literal(String.format("#%06X", look.rgb()))));
        if (look.caseBlock().isPresent()) {
            tooltip.add(line(LampPart.CASE, look.caseBlock().map(Block::getName).get()));
        } else if (look.caseRgb() >= 0) {
            tooltip.add(line(LampPart.CASE, Component.literal(String.format("#%06X", look.caseRgb()))));
        }
        if (look.case2Block().isPresent()) {
            tooltip.add(line(LampPart.CASE2, look.case2Block().map(Block::getName).get()));
        } else if (look.case2Rgb() >= 0) {
            tooltip.add(line(LampPart.CASE2, Component.literal(String.format("#%06X", look.case2Rgb()))));
        }
    }

    private static Component line(LampPart part, Component value) {
        return Component.translatable("message.sqdecor.lamp_part", part.displayName(), value).withStyle(ChatFormatting.GRAY);
    }
}
