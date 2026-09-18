package com.flowclient.mods.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public final class BlockOverlayRenderer {
    private static final Vector3f LINE_NORMAL = new Vector3f();

    private BlockOverlayRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            BlockOutlineRenderState state
    ) {
        if (levelRenderState == null || levelRenderState.cameraRenderState == null) {
            return;
        }

        VoxelShape shape = state.shape();
        if (shape == null || shape.isEmpty()) {
            return;
        }

        BlockOverlaySettings settings = BlockOverlaySettings.get();
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos pos = state.pos();

        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

        boolean afterTerrain = state.isTranslucent();
        switch (settings.style()) {
            case CLASSIC -> submitOutline(submitNodeCollector, poseStack, shape, settings.resolveLineColorArgb(), settings.resolveLineWidth(), afterTerrain);
            case BOLD -> {
                submitOutline(submitNodeCollector, poseStack, shape, settings.resolveLineColorArgb(), settings.resolveLineWidth() + 1.5F, afterTerrain);
                if (settings.outline()) {
                    submitOutline(submitNodeCollector, poseStack, shape, settings.resolveOutlineColorArgb(), settings.resolveLineWidth() + 3.0F, afterTerrain);
                }
            }
            case GLOW, NEON -> {
                submitOutline(submitNodeCollector, poseStack, shape, settings.resolveGlowColorArgb(), settings.resolveGlowWidth(), afterTerrain);
                submitOutline(submitNodeCollector, poseStack, shape, settings.resolveLineColorArgb(), settings.resolveLineWidth(), afterTerrain);
                if (settings.outline()) {
                    submitOutline(submitNodeCollector, poseStack, shape, settings.resolveOutlineColorArgb(), settings.resolveLineWidth() + 1.0F, afterTerrain);
                }
            }
            case CORNERS -> submitCorners(submitNodeCollector, poseStack, shape, settings, afterTerrain);
        }

        poseStack.popPose();
    }

    private static void submitOutline(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            VoxelShape shape,
            int color,
            float width,
            boolean afterTerrain
    ) {
        if ((color >>> 24) == 0 || width <= 0.0F) {
            return;
        }
        submitNodeCollector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), color, width, afterTerrain);
    }

    private static void submitCorners(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            VoxelShape shape,
            BlockOverlaySettings settings,
            boolean afterTerrain
    ) {
        int color = settings.resolveLineColorArgb();
        float width = settings.resolveLineWidth();
        if ((color >>> 24) == 0 || width <= 0.0F) {
            return;
        }

        AABB box = shape.bounds();
        float length = settings.cornerLength() / 16.0F;

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
            cornerEdges(consumer, pose, box.minX, box.minY, box.minZ, length, 1, 1, 1, color, width);
            cornerEdges(consumer, pose, box.maxX, box.minY, box.minZ, length, -1, 1, 1, color, width);
            cornerEdges(consumer, pose, box.minX, box.minY, box.maxZ, length, 1, 1, -1, color, width);
            cornerEdges(consumer, pose, box.maxX, box.minY, box.maxZ, length, -1, 1, -1, color, width);
            cornerEdges(consumer, pose, box.minX, box.maxY, box.minZ, length, 1, -1, 1, color, width);
            cornerEdges(consumer, pose, box.maxX, box.maxY, box.minZ, length, -1, -1, 1, color, width);
            cornerEdges(consumer, pose, box.minX, box.maxY, box.maxZ, length, 1, -1, -1, color, width);
            cornerEdges(consumer, pose, box.maxX, box.maxY, box.maxZ, length, -1, -1, -1, color, width);
        });

        if (settings.outline()) {
            int outlineColor = settings.resolveOutlineColorArgb();
            float outlineWidth = width + 2.0F;
            if ((outlineColor >>> 24) != 0 && outlineWidth > 0.0F) {
                submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
                    cornerEdges(consumer, pose, box.minX, box.minY, box.minZ, length, 1, 1, 1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.maxX, box.minY, box.minZ, length, -1, 1, 1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.minX, box.minY, box.maxZ, length, 1, 1, -1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.maxX, box.minY, box.maxZ, length, -1, 1, -1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.minX, box.maxY, box.minZ, length, 1, -1, 1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.maxX, box.maxY, box.minZ, length, -1, -1, 1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.minX, box.maxY, box.maxZ, length, 1, -1, -1, outlineColor, outlineWidth);
                    cornerEdges(consumer, pose, box.maxX, box.maxY, box.maxZ, length, -1, -1, -1, outlineColor, outlineWidth);
                });
            }
        }
    }

    private static void cornerEdges(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            float length,
            int dirX,
            int dirY,
            int dirZ,
            int color,
            float width
    ) {
        float fx = (float) x;
        float fy = (float) y;
        float fz = (float) z;
        line(consumer, pose, fx, fy, fz, fx + length * dirX, fy, fz, color, width);
        line(consumer, pose, fx, fy, fz, fx, fy + length * dirY, fz, color, width);
        line(consumer, pose, fx, fy, fz, fx, fy, fz + length * dirZ, color, width);
    }

    private static void line(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            int color,
            float width
    ) {
        LINE_NORMAL.set(x2 - x1, y2 - y1, z2 - z1).normalize();
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(color)
                .setNormal(pose, LINE_NORMAL)
                .setLineWidth(width);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, LINE_NORMAL)
                .setLineWidth(width);
    }
}
