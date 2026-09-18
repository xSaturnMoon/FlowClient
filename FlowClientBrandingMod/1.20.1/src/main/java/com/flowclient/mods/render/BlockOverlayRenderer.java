package com.flowclient.mods.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockOverlayRenderer {
    private BlockOverlayRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            double camX,
            double camY,
            double camZ,
            BlockPos pos,
            BlockState state
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        VoxelShape shape = state.getShape(client.level, pos);
        if (shape.isEmpty()) {
            return;
        }

        BlockOverlaySettings settings = BlockOverlaySettings.get();
        int color = settings.resolveLineColorArgb();
        if ((color >>> 24) == 0) {
            return;
        }

        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
        LevelRenderer.renderLineBox(poseStack, consumer, shape.bounds(), red, green, blue, alpha);
        poseStack.popPose();
    }
}
