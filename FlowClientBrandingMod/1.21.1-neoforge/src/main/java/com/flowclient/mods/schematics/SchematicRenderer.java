package com.flowclient.mods.schematics;

import com.flowclient.mods.schematics.model.Schematic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;

public final class SchematicRenderer {
    private static final float OUTLINE_R = 0f;
    private static final float OUTLINE_G = 0.83f;
    private static final float OUTLINE_B = 1f;
    private static final float OUTLINE_A = 0.67f;

    private SchematicRenderer() {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (!AllSchematicsMod.isEnabled() || !PlacementManager.hasPlacement() || client.level == null) {
            return;
        }

        Schematic schematic = PlacementManager.getSchematic();
        Map<BlockPos, BlockState> blocks = schematic.getBlocks();
        if (blocks.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos worldPos = PlacementManager.toWorldPos(entry.getKey());
            AABB box = new AABB(worldPos);
            LevelRenderer.renderLineBox(poseStack, lineConsumer, box, OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}
