package com.sqdecor.client.render;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.lamp.ShelfBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class ShelfItems {
    private ShelfItems() {}

    static void render(LampBlockEntity lamp, ShelfBlock shelf, BlockState state, Level level, PoseStack pose,
                       MultiBufferSource buffers, int overlay) {
        if (!lamp.hasShelfItems()) return;
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        int light = LevelRenderer.getLightColor(level, lamp.getBlockPos());
        Quaternionf rotation = ShelfBlock.rotation(state);
        double gap = shelf.gap();
        float scale = (float) (gap * 0.1);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(rotation);
        pose.translate(-0.5, -0.5, -0.5);
        for (int slot = 0; slot < LampBlockEntity.SHELF_SLOTS; slot++) {
            ItemStack stack = lamp.shelfItem(slot);
            if (stack.isEmpty()) continue;
            double[] spot = shelf.spot(slot);
            pose.pushPose();
            pose.translate(0.5 + spot[0] / 16, (spot[1] + gap * 0.4) / 16, 0.5 + spot[2] / 16);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(scale, scale, scale);
            renderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level,
                    (int) lamp.getBlockPos().asLong() + slot);
            pose.popPose();
        }
        pose.popPose();
    }
}
