package com.sqdecor.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.network.MarqueePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

public class MarqueeScreen extends Screen {
    private static final int PW = 220;
    private static final int PH = 142;

    private final BlockPos pos;
    private String text;
    private int rgb;
    private int speed;
    private boolean on;

    private EditBox textBox;
    private final List<int[]> swatches = new ArrayList<>();
    private int px;
    private int py;
    private boolean backgroundDrawn;

    public MarqueeScreen(BlockPos pos, String text, int rgb, int speed, boolean on) {
        super(Component.translatable("gui.sqdecor.marquee.title"));
        this.pos = pos;
        this.text = text;
        this.rgb = rgb;
        this.speed = speed;
        this.on = on;
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        int x = px + 10;
        int w = PW - 20;

        textBox = new EditBox(font, x, py + 22, w, 20, Component.translatable("gui.sqdecor.marquee.text"));
        textBox.setMaxLength(MarqueeBlockEntity.MAX_TEXT);
        textBox.setHint(Component.translatable("gui.sqdecor.marquee.text"));
        textBox.setValue(text);
        textBox.setResponder(v -> text = v);
        addRenderableWidget(textBox);
        setInitialFocus(textBox);

        swatches.clear();
        for (int i = 0; i < 16; i++) {
            final DyeColor dye = DyeColor.byId(i);
            final int col = dye.getTextureDiffuseColor() & 0xFFFFFF;
            int sx = x + (i % 8) * 25;
            int sy = py + 48 + (i / 8) * 16;
            swatches.add(new int[]{sx, sy, col});
            addRenderableWidget(Button.builder(Component.empty(), b -> rgb = col)
                    .bounds(sx, sy, 20, 14)
                    .tooltip(Tooltip.create(Component.translatable("color.minecraft." + dye.getName())))
                    .build());
        }

        LedTunerScreen.IntSlider slider = new LedTunerScreen.IntSlider(x, py + 84, w, 18,
                Component.translatable("gui.sqdecor.marquee.speed"), 0, MarqueeBlockEntity.MAX_SPEED, speed, v -> speed = v);
        slider.setTooltip(Tooltip.create(Component.translatable("gui.sqdecor.marquee.speed_hint")));
        addRenderableWidget(slider);

        addRenderableWidget(Button.builder(onText(), b -> {
            on = !on;
            b.setMessage(onText());
        }).bounds(x, py + 110, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.tuner.done"), b -> done())
                .bounds(x + 102, py + 110, 98, 20).build());
    }

    private Component onText() {
        return Component.translatable("gui.sqdecor.marquee.power",
                Component.translatable(on ? "message.sqdecor.on" : "message.sqdecor.off"));
    }

    private void done() {
        PacketDistributor.sendToServer(new MarqueePayload(pos, MarqueeBlockEntity.clean(text), rgb, speed, on));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            done();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

        for (int[] s : swatches) {
            g.fill(s[0] + 3, s[1] + 3, s[0] + 17, s[1] + 11, 0xFF000000 | s[2]);
            if (s[2] == rgb) g.renderOutline(s[0] - 1, s[1] - 1, 22, 16, 0xFFFFFFFF);
        }
        int bx = px + PW - 40;
        g.fill(bx, py + 5, bx + 30, py + 17, 0xFF000000 | rgb);
        g.renderOutline(bx, py + 5, 30, 12, 0xFFFFFFFF);
    }
}
