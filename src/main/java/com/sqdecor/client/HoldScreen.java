package com.sqdecor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class HoldScreen extends Screen {
    private double lastMouseX;
    private double lastMouseY;

    protected HoldScreen(Component title) {
        super(title);
    }

    protected abstract void confirm(double mouseX, double mouseY);

    protected abstract boolean stillValid();

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || !stillValid()) onClose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (minecraft != null && minecraft.options.keyUse.matchesMouse(button)) {
            finish(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            finish(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyUse.matches(keyCode, scanCode)) {
            finish(lastMouseX, lastMouseY);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void finish(double mouseX, double mouseY) {
        confirm(mouseX, mouseY);
        onClose();
    }
}
