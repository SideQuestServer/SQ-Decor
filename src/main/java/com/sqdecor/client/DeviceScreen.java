package com.sqdecor.client;

import java.util.ArrayList;
import java.util.List;

import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.DeviceSetting;
import com.sqdecor.network.DeviceSettingsPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public class DeviceScreen extends Screen {
    private static final int PW = 220;

    private final BlockPos pos;
    private final List<DeviceSetting> settings;
    private final int[] values;
    private int px;
    private int py;
    private int ph;
    private List<FormattedCharSequence> hint = List.of();
    private boolean backgroundDrawn;

    public DeviceScreen(BlockPos pos, Component title, List<DeviceSetting> settings, LightSwitchBlockEntity be) {
        super(title);
        this.pos = pos;
        this.settings = settings;
        this.values = new int[settings.size()];
        for (int i = 0; i < values.length; i++) values[i] = be.get(settings.get(i));
    }

    @Override
    protected void init() {
        hint = font.split(Component.translatable("gui.sqdecor.device.hint." + settings.getFirst().key), PW - 20);
        ph = 30 + settings.size() * 22 + hint.size() * 10 + 34;
        px = (width - PW) / 2;
        py = (height - ph) / 2;
        int y = py + 22;
        for (int i = 0; i < settings.size(); i++) {
            DeviceSetting setting = settings.get(i);
            final int index = i;
            addRenderableWidget(new LedTunerScreen.IntSlider(px + 10, y, PW - 20, 18, setting.label(),
                    setting.min, setting.max, values[i], v -> values[index] = v));
            y += 22;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.tuner.done"), b -> {
            List<Integer> list = new ArrayList<>();
            for (int v : values) list.add(v);
            PacketDistributor.sendToServer(new DeviceSettingsPayload(pos, list));
            onClose();
        }).bounds(px + 10, py + ph - 26, PW - 20, 20).build());
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
        g.fill(px, py, px + PW, py + ph, 0xFF2B2B2B);
        g.renderOutline(px, py, PW, ph, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        backgroundDrawn = false;
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, px + 10, py + 7, 0xFFFFFF, true);
        int y = py + 26 + settings.size() * 22;
        for (FormattedCharSequence line : hint) {
            g.drawString(font, line, px + 10, y, 0xFFA0A0A0, false);
            y += 10;
        }
    }
}
