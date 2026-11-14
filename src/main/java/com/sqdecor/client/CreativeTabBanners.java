package com.sqdecor.client;

import com.sqdecor.registry.TabGroups;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class CreativeTabBanners {
    private CreativeTabBanners() {}

    private static final int WIDTH = 162;
    private static final int HEIGHT = 18;

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        var slots = screen.getMenu().slots;
        for (int i = 0; i < 45 && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            TabGroups.Group group = TabGroups.headerGroup(stack.getItem());
            if (group == null) continue;
            int x = screen.getGuiLeft() + slot.x - 1;
            int y = screen.getGuiTop() + slot.y - 1;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF000000);
            graphics.fillGradient(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1,
                    0xFF000000 | shade(group.color(), 1f), 0xFF000000 | shade(group.color(), 0.55f));
            graphics.drawString(font, stack.getHoverName().getString(), x + 6, y + 5, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (TabGroups.isLayoutItem(event.getItemStack().getItem())) event.setCanceled(true);
    }

    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        Slot slot = screen.getSlotUnderMouse();
        if (slot != null && TabGroups.isLayoutItem(slot.getItem().getItem())) event.setCanceled(true);
    }

    private static int shade(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 255) * f);
        int g = (int) (((rgb >> 8) & 255) * f);
        int b = (int) ((rgb & 255) * f);
        return r << 16 | g << 8 | b;
    }
}
