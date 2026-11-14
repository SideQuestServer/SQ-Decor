package com.sqdecor.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.item.TunerScene;
import com.sqdecor.item.TunerSettings;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.AutoMode;
import com.sqdecor.network.LedTunerPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class LedTunerScreen extends Screen {
    private static final int PW = 220;
    private static final int PH = 288;
    private static final String[] MODE_KEYS = {"keep", "ignore", "analog", "switch"};
    private static final String[] LIGHT_MODE_KEYS = {"keep", "normal", "glow", "spot"};
    private static final String[] LIGHT_TINT_KEYS = {"keep", "white", "colored"};
    private static final String[] AUTO_KEYS = {"keep", "off", "night", "nearby"};
    private static final LampPart[] PARTS = LampPart.PAINTABLE;

    private int level;
    private int mode;
    private int auto;
    private int autoRange;
    private int lightMode;
    private int lightTint;
    private final int[] colors = new int[3];
    private final String[] blocks = new String[3];
    private int part;

    final TunerSettings stored;
    final List<TunerScene> scenes;

    private int px;
    private int py;
    private boolean backgroundDrawn;

    private IntSlider rSlider;
    private IntSlider gSlider;
    private IntSlider bSlider;
    private final List<int[]> swatches = new ArrayList<>();

    public LedTunerScreen(TunerSettings s, List<TunerScene> scenes) {
        super(Component.translatable("gui.sqdecor.tuner.title"));
        this.stored = s;
        this.scenes = scenes;
        level = s.level();
        mode = s.mode();
        auto = s.auto();
        autoRange = s.autoRange();
        lightMode = s.lightMode();
        lightTint = s.lightTint();
        colors[0] = s.rgb();
        colors[1] = s.caseRgb();
        colors[2] = s.case2Rgb();
        blocks[0] = "";
        blocks[1] = s.caseBlock();
        blocks[2] = s.case2Block();
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        int x = px + 10;
        int w = PW - 20;

        addRenderableWidget(new IntSlider(x, py + 22, w, 18,
                Component.translatable("gui.sqdecor.tuner.brightness"), 0, 15, level, v -> level = v));

        addRenderableWidget(Button.builder(partText(), b -> {
            part = (part + 1) % PARTS.length;
            b.setMessage(partText());
            showColor();
        }).bounds(x, py + 44, w, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.part_hint")))
                .build());

        swatches.clear();
        for (int i = 0; i < 16; i++) {
            final DyeColor dye = DyeColor.byId(i);
            final int col = dye.getTextureDiffuseColor() & 0xFFFFFF;
            int sx = x + (i % 8) * 25;
            int sy = py + 68 + (i / 8) * 16;
            swatches.add(new int[]{sx, sy, col});
            addRenderableWidget(Button.builder(Component.empty(), b -> setColor(col))
                    .bounds(sx, sy, 20, 14)
                    .tooltip(Tooltip.create(Component.translatable("color.minecraft." + dye.getName())))
                    .build());
        }

        rSlider = addRenderableWidget(new IntSlider(x, py + 104, w, 18,
                Component.translatable("gui.sqdecor.tuner.red"), 0, 255, 0, v -> rgbChanged()));
        gSlider = addRenderableWidget(new IntSlider(x, py + 124, w, 18,
                Component.translatable("gui.sqdecor.tuner.green"), 0, 255, 0, v -> rgbChanged()));
        bSlider = addRenderableWidget(new IntSlider(x, py + 144, w, 18,
                Component.translatable("gui.sqdecor.tuner.blue"), 0, 255, 0, v -> rgbChanged()));
        showColor();

        IntSlider kelvin = addRenderableWidget(new IntSlider(x, py + 164, w, 18,
                Component.translatable("gui.sqdecor.tuner.temperature"), 1800, 6500, 2700,
                v -> setColor(kelvinToRgb(v))));
        kelvin.setTooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.temperature_hint")));

        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.tuner.keep_color"), b -> {
            colors[part] = TunerSettings.KEEP;
            blocks[part] = "";
        }).bounds(x, py + 190, 98, 20).build());
        addRenderableWidget(Button.builder(modeText(), b -> {
            mode = mode >= 2 ? -1 : mode + 1;
            b.setMessage(modeText());
        }).bounds(x + 102, py + 190, 98, 20).build());

        addRenderableWidget(Button.builder(autoText(), b -> {
            auto = auto >= TunerSettings.AUTO_MAX ? -1 : auto + 1;
            b.setMessage(autoText());
        }).bounds(x, py + 214, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.auto_hint")))
                .build());
        IntSlider range = addRenderableWidget(new IntSlider(x + 102, py + 214, 98, 20,
                Component.translatable("gui.sqdecor.tuner.auto_range"), AutoMode.MIN_RANGE, AutoMode.MAX_RANGE,
                autoRange, v -> autoRange = v));
        range.setTooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.auto_range_hint")));

        addRenderableWidget(Button.builder(lightModeText(), b -> {
            lightMode = lightMode >= TunerSettings.LIGHT_MODE_MAX ? -1 : lightMode + 1;
            b.setMessage(lightModeText());
        }).bounds(x, py + 238, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.light_mode_hint")))
                .build());
        addRenderableWidget(Button.builder(lightTintText(), b -> {
            lightTint = lightTint >= 1 ? -1 : lightTint + 1;
            b.setMessage(lightTintText());
        }).bounds(x + 102, py + 238, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.light_tint_hint")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.tuner.scenes"),
                        b -> minecraft.setScreen(new SceneScreen(this)))
                .bounds(x, py + 262, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.tuner.scenes_hint")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.tuner.done"), b -> {
            PacketDistributor.sendToServer(new LedTunerPayload(current(), List.copyOf(scenes)));
            onClose();
        }).bounds(x + 102, py + 262, 98, 20).build());
    }

    TunerSettings current() {
        return new TunerSettings(level, colors[0], mode, 0, colors[1], colors[2], blocks[1], blocks[2], auto, autoRange,
                lightMode, lightTint);
    }

    private Component partText() {
        return Component.translatable("gui.sqdecor.tuner.part", PARTS[part].displayName());
    }

    private Component modeText() {
        return Component.translatable("gui.sqdecor.tuner.redstone",
                Component.translatable("gui.sqdecor.tuner.mode." + MODE_KEYS[mode + 1]));
    }

    private Component lightModeText() {
        return Component.translatable("gui.sqdecor.tuner.light_mode",
                Component.translatable("gui.sqdecor.tuner.light_mode." + LIGHT_MODE_KEYS[lightMode + 1]));
    }

    private Component lightTintText() {
        return Component.translatable("gui.sqdecor.tuner.light_tint",
                Component.translatable("gui.sqdecor.tuner.light_tint." + LIGHT_TINT_KEYS[lightTint + 1]));
    }

    private Component autoText() {
        return Component.translatable("gui.sqdecor.tuner.auto",
                Component.translatable("gui.sqdecor.tuner.auto." + AUTO_KEYS[auto + 1]));
    }

    private void showColor() {
        int col = colors[part] >= 0 ? colors[part] : LedBlockEntity.DEFAULT_RGB;
        rSlider.setInt((col >> 16) & 0xFF);
        gSlider.setInt((col >> 8) & 0xFF);
        bSlider.setInt(col & 0xFF);
    }

    private void setColor(int col) {
        colors[part] = col;
        blocks[part] = "";
        showColor();
    }

    static int kelvinToRgb(int kelvin) {
        double t = kelvin / 100.0;
        double r = t <= 66 ? 255 : 329.698727446 * Math.pow(t - 60, -0.1332047592);
        double g = t <= 66 ? 99.4708025861 * Math.log(t) - 161.1195681661 : 288.1221695283 * Math.pow(t - 60, -0.0755148492);
        double b = t >= 66 ? 255 : t <= 19 ? 0 : 138.5177312231 * Math.log(t - 10) - 305.0447927307;
        return (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    private void rgbChanged() {
        colors[part] = (rSlider.intValue() << 16) | (gSlider.intValue() << 8) | bSlider.intValue();
        blocks[part] = "";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (backgroundDrawn) return;
        backgroundDrawn = true;
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(px, py, px + PW, py + PH, 0xFF2B2B2B);
        g.renderOutline(px, py, PW, PH, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        backgroundDrawn = false;
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(font, title, px + 10, py + 7, 0xFFFFFF, true);

        int current = blocks[part].isEmpty() ? colors[part] : TunerSettings.KEEP;
        for (int[] s : swatches) {
            g.fill(s[0] + 3, s[1] + 3, s[0] + 17, s[1] + 11, 0xFF000000 | s[2]);
            if (s[2] == current) g.renderOutline(s[0] - 1, s[1] - 1, 22, 16, 0xFFFFFFFF);
        }

        int bx = px + PW - 40;
        Block block = LedTunerItem.block(blocks[part]);
        if (block != null) {
            g.renderItem(new ItemStack(block), bx + 7, py + 3);
        } else {
            int pv = colors[part] >= 0 ? colors[part] : 0x505050;
            g.fill(bx, py + 5, bx + 30, py + 17, 0xFF000000 | pv);
            g.renderOutline(bx, py + 5, 30, 12, 0xFFFFFFFF);
        }
        Component value = LedTunerItem.caseValue(colors[part], blocks[part]);
        if (value == null) value = Component.translatable("gui.sqdecor.tuner.mode.keep");
        if (colors[part] >= 0 && block == null) value = null;
        if (value != null) {
            String text = font.plainSubstrByWidth(value.getString(), 90);
            g.drawString(font, text, bx - 4 - font.width(text), py + 7, 0xFFA0A0A0, false);
        }
    }

    public static class IntSlider extends AbstractSliderButton {
        private final Component label;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        public IntSlider(int x, int y, int w, int h, Component label, int min, int max, int value, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), (double) (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        int intValue() {
            return min + (int) Math.round(value * (max - min));
        }

        void setInt(int v) {
            this.value = (double) (Math.max(min, Math.min(max, v)) - min) / (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty().append(label).append(": " + intValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(intValue());
        }
    }
}
