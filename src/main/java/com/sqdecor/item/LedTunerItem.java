package com.sqdecor.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.client.LedTunerClient;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.AutoMode;
import com.sqdecor.led.LedLogic;
import com.sqdecor.led.RedstoneMode;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LedTunerItem extends Item {
    public LedTunerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) LedTunerClient.open(stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            TunerSettings s = TunerSettings.of(stack);
            int n = (s.level() + 1) % 16;
            s.withLevel(n).store(stack);
            LedLogic.showLevel(player, n);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof LedBlock;
    }

    public static void apply(ItemStack stack, Level level, BlockPos pos, Player player, boolean network) {
        TunerSettings s = TunerSettings.of(stack);

        List<BlockPos> targets = network ? LedLogic.network(level, pos) : List.of(pos);
        List<BlockPos> leds = new ArrayList<>();
        for (BlockPos p : targets) {
            if (!(level.getBlockState(p).getBlock() instanceof LedBlock)) continue;
            leds.add(p);
            LedLogic.applyLevel(level, p, s.level(), true);
            if (s.rgb() >= 0 && level.getBlockEntity(p) instanceof LedBlockEntity be) be.setRgb(s.rgb());
            if (s.mode() >= 0) LedLogic.setRedstoneMode(level, p, RedstoneMode.byId(s.mode()));
            if (s.auto() >= 0) {
                if (level.getBlockEntity(p) instanceof LedBlockEntity be) be.setAutoRange(s.autoRange());
                LedLogic.setAutoMode(level, p, AutoMode.byId(s.auto()));
            }
            if (level.getBlockEntity(p) instanceof LedBlockEntity be) {
                if (s.lightMode() >= 0) be.setLightMode(s.lightMode());
                if (s.lightTint() >= 0) be.setLightTinted(s.lightTint() == 1);
            }
            if (level.getBlockEntity(p) instanceof LampBlockEntity lamp) {
                applyCase(lamp, LampPart.CASE, s.caseRgb(), s.caseBlock());
                applyCase(lamp, LampPart.CASE2, s.case2Rgb(), s.case2Block());
            }
        }
        LedLogic.remember(player, s.level());

        if (network) {
            player.displayClientMessage(Component.translatable("message.sqdecor.tuner_network", leds.size()), true);
        } else {
            LedLogic.showLevel(player, s.level());
        }
    }

    private static void applyCase(LampBlockEntity lamp, LampPart part, int rgb, String blockId) {
        Block block = block(blockId);
        if (block != null) {
            lamp.setCase(part, LampBlockEntity.NO_COLOR, block.defaultBlockState());
        } else if (rgb == TunerSettings.ORIGINAL) {
            lamp.setCase(part, LampBlockEntity.NO_COLOR, null);
        } else if (rgb >= 0) {
            lamp.setCase(part, rgb, null);
        }
    }

    @Nullable
    public static Block block(String id) {
        if (id.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(rl).filter(LampBlockEntity::canUseAsTexture).orElse(null);
    }

    public static void copyLook(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof LedBlockEntity be)) return;
        TunerSettings s = TunerSettings.of(stack);
        int lvl = level.getBlockState(pos).getValue(LedLogic.LIGHT_LEVEL);
        int caseRgb = TunerSettings.KEEP;
        int case2Rgb = TunerSettings.KEEP;
        String caseBlock = "";
        String case2Block = "";
        if (be instanceof LampBlockEntity lamp) {
            caseRgb = copyColor(lamp, LampPart.CASE);
            case2Rgb = copyColor(lamp, LampPart.CASE2);
            caseBlock = blockId(lamp.texture(LampPart.CASE));
            case2Block = blockId(lamp.texture(LampPart.CASE2));
        }
        new TunerSettings(lvl, be.getRgb(), s.mode(), s.program(), caseRgb, case2Rgb, caseBlock, case2Block, s.auto(), s.autoRange(),
                be.getLightMode(), be.isLightTinted() ? 1 : 0).store(stack);
        player.displayClientMessage(Component.translatable("message.sqdecor.tuner_copied"), true);
    }

    private static int copyColor(LampBlockEntity lamp, LampPart part) {
        int rgb = lamp.caseColor(part);
        return rgb == LampBlockEntity.NO_COLOR ? TunerSettings.ORIGINAL : rgb;
    }

    private static String blockId(@Nullable BlockState state) {
        return state == null ? "" : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static final String[] MODES = {"keep", "ignore", "analog", "switch"};
    private static final String[] AUTOS = {"keep", "off", "night", "nearby"};

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TunerSettings s = TunerSettings.of(stack);
        String base = "item.sqdecor.led_tuner.tooltip.";

        TunerScene scene = TunerScene.matching(stack);
        if (scene != null) tooltip.add(Component.translatable(base + "scene", scene.name()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(base + "level", s.level()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(s.rgb() < 0
                ? Component.translatable(base + "color_keep").withStyle(ChatFormatting.GRAY)
                : Component.translatable(base + "color", String.format("%06X", s.rgb())).withStyle(ChatFormatting.GRAY));
        addCase(tooltip, LampPart.CASE, s.caseRgb(), s.caseBlock());
        addCase(tooltip, LampPart.CASE2, s.case2Rgb(), s.case2Block());
        tooltip.add(Component.translatable(base + "redstone",
                Component.translatable("gui.sqdecor.tuner.mode." + MODES[s.mode() + 1])).withStyle(ChatFormatting.GRAY));
        Component auto = Component.translatable("gui.sqdecor.tuner.auto." + AUTOS[s.auto() + 1]);
        tooltip.add((s.auto() == AutoMode.NEARBY.ordinal()
                ? Component.translatable(base + "auto_range", auto, s.autoRange())
                : Component.translatable(base + "auto", auto)).withStyle(ChatFormatting.GRAY));
        for (int i = 1; i <= 5; i++) {
            tooltip.add(Component.translatable(base + "hint" + i).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addCase(List<Component> tooltip, LampPart part, int rgb, String blockId) {
        Component value = caseValue(rgb, blockId);
        if (value != null) {
            tooltip.add(Component.translatable("message.sqdecor.lamp_part", part.displayName(), value)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Nullable
    public static Component caseValue(int rgb, String blockId) {
        Block block = block(blockId);
        if (block != null) return block.getName();
        if (rgb == TunerSettings.ORIGINAL) return Component.translatable("gui.sqdecor.tuner.original");
        if (rgb >= 0) return Component.literal(String.format("#%06X", rgb));
        return null;
    }
}
