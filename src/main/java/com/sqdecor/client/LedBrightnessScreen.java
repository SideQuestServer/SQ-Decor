package com.sqdecor.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.sqdecor.block.led.LedBlock;
import com.sqdecor.network.LedBrightnessPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class LedBrightnessScreen extends HoldScreen {
    private static final int STEP = 9;
    private static final int BAR_H = 10;

    private final BlockPos pos;
    private final int original;
    private int value;
    private int sent;
    private int barX;

    public LedBrightnessScreen(BlockPos pos, int level) {
        super(Component.translatable("gui.sqdecor.tuner.brightness"));
        this.pos = pos;
        this.original = level;
        this.value = level;
        this.sent = level;
    }

    @Override
    protected void init() {
        barX = width / 2 - original * STEP;
    }

    @Override
    protected boolean stillValid() {
        return minecraft.level.getBlockState(pos).getBlock() instanceof LedBlock
                && minecraft.player.canInteractWithBlock(pos, 1.0)
                && minecraft.player.getMainHandItem().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (value != sent) send(value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        value = levelAt(mouseX);
        int y = height / 2 + 14;
        int x2 = barX + 15 * STEP;

        graphics.fill(barX - 3, y - 3, x2 + 4, y + BAR_H + 3, 0xC0101010);
        graphics.fill(barX, y, barX + value * STEP + 1, y + BAR_H, 0xFFFFD84A);
        for (int i = 0; i <= 15; i++) {
            int x = barX + i * STEP;
            graphics.fill(x, y + BAR_H - 3, x + 1, y + BAR_H, i <= value ? 0xFF8A6A00 : 0xFF505050);
        }
        int hx = barX + value * STEP;
        graphics.fill(hx - 2, y - 4, hx + 3, y + BAR_H + 4, 0xFFFFFFFF);

        Component label = Component.empty().append(title).append(": " + value + "/15");
        graphics.drawCenteredString(font, label, (barX + x2) / 2, y - 14, 0xFFFFFFFF);
    }

    private int levelAt(double mouseX) {
        return Mth.clamp((int) Math.round((mouseX - barX) / STEP), 0, 15);
    }

    @Override
    protected void confirm(double mouseX, double mouseY) {
        value = levelAt(mouseX);
        if (value != sent) send(value);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (sent != original) send(original);
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void send(int level) {
        PacketDistributor.sendToServer(new LedBrightnessPayload(pos, level));
        sent = level;
    }
}
