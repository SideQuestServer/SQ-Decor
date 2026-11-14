package com.sqdecor.client;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.network.LampPaintPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class LampWheelScreen extends HoldScreen {
    private static final int INNER = 22;
    private static final int OUTER = 76;
    private static final float GAP = 1.5f;

    private final BlockPos pos;
    private final InteractionHand hand;
    private final List<LampPart> parts;
    private int hovered = -1;

    public LampWheelScreen(BlockPos pos, InteractionHand hand, List<LampPart> parts) {
        super(Component.translatable("gui.sqdecor.lamp_wheel"));
        this.pos = pos;
        this.hand = hand;
        this.parts = parts;
    }

    @Override
    protected boolean stillValid() {
        return minecraft.level.getBlockState(pos).getBlock() instanceof LampBlock
                && minecraft.player.canInteractWithBlock(pos, 1.0)
                && minecraft.player.getItemInHand(hand).getItem() instanceof DyeItem;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        int cx = width / 2;
        int cy = height / 2;
        hovered = sectorAt(mouseX, mouseY);

        ItemStack dye = minecraft.player.getItemInHand(hand);
        int dyeRgb = dye.getItem() instanceof DyeItem item ? item.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF : 0xFFFFFF;
        LampBlockEntity lamp = minecraft.level.getBlockEntity(pos) instanceof LampBlockEntity be ? be : null;

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        float step = 360f / parts.size();
        for (int i = 0; i < parts.size(); i++) {
            float start = -90 - step / 2 + i * step + GAP;
            int color = i == hovered ? 0xE0000000 | dyeRgb : 0xA0101010;
            int outer = i == hovered ? OUTER + 4 : OUTER;
            ring(buffer, matrix, cx, cy, INNER, outer, start, start + step - 2 * GAP, color);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        for (int i = 0; i < parts.size(); i++) {
            LampPart part = parts.get(i);
            double angle = Math.toRadians(-90 + i * step);
            int x = cx + (int) Math.round(Math.cos(angle) * (INNER + OUTER) / 2.0);
            int y = cy + (int) Math.round(Math.sin(angle) * (INNER + OUTER) / 2.0);
            int textColor = i == hovered && brightness(dyeRgb) > 160 ? 0xFF202020 : 0xFFFFFFFF;
            graphics.drawCenteredString(font, part.displayName(), x, y - 11, textColor);
            if (lamp == null) continue;
            BlockState texture = lamp.texture(part);
            int current = lamp.color(part);
            if (texture != null) {
                graphics.renderItem(new ItemStack(texture.getBlock()), x - 8, y);
            } else if (current != LampBlockEntity.NO_COLOR) {
                graphics.fill(x - 9, y + 1, x + 9, y + 9, 0xFF000000);
                graphics.fill(x - 8, y + 2, x + 8, y + 8, 0xFF000000 | current);
            }
        }
        graphics.renderItem(dye, cx - 8, cy - 8);
    }

    private static void ring(BufferBuilder buffer, Matrix4f matrix, int cx, int cy, int inner, int outer,
                             float start, float end, int color) {
        int steps = Math.max(2, (int) Math.ceil((end - start) / 4));
        for (int s = 0; s < steps; s++) {
            double a0 = Math.toRadians(start + (end - start) * s / steps);
            double a1 = Math.toRadians(start + (end - start) * (s + 1) / steps);
            float ix0 = cx + (float) Math.cos(a0) * inner, iy0 = cy + (float) Math.sin(a0) * inner;
            float ox0 = cx + (float) Math.cos(a0) * outer, oy0 = cy + (float) Math.sin(a0) * outer;
            float ix1 = cx + (float) Math.cos(a1) * inner, iy1 = cy + (float) Math.sin(a1) * inner;
            float ox1 = cx + (float) Math.cos(a1) * outer, oy1 = cy + (float) Math.sin(a1) * outer;
            buffer.addVertex(matrix, ix0, iy0, 0).setColor(color);
            buffer.addVertex(matrix, ox0, oy0, 0).setColor(color);
            buffer.addVertex(matrix, ox1, oy1, 0).setColor(color);
            buffer.addVertex(matrix, ix0, iy0, 0).setColor(color);
            buffer.addVertex(matrix, ox1, oy1, 0).setColor(color);
            buffer.addVertex(matrix, ix1, iy1, 0).setColor(color);
        }
    }

    private static int brightness(int rgb) {
        return (((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
    }

    private int sectorAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        if (dx * dx + dy * dy < (INNER - 4) * (INNER - 4)) return -1;
        double step = 360.0 / parts.size();
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90 + step / 2;
        angle = ((angle % 360) + 360) % 360;
        return Math.min(parts.size() - 1, (int) (angle / step));
    }

    @Override
    protected void confirm(double mouseX, double mouseY) {
        int sector = sectorAt(mouseX, mouseY);
        if (sector < 0) return;
        PacketDistributor.sendToServer(new LampPaintPayload(pos, parts.get(sector).ordinal(), hand == InteractionHand.OFF_HAND));
        if (minecraft != null && minecraft.player != null) minecraft.player.swing(hand);
    }
}
