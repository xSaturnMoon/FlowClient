package com.flowclient.mixin;

import com.flowclient.mods.render.BlockAnimationController;
import com.flowclient.mods.render.BlockOverlayMod;
import com.flowclient.mods.render.BlockOverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void flowclient$customBlockOutline(
            PoseStack poseStack,
            VertexConsumer consumer,
            Entity entity,
            double camX,
            double camY,
            double camZ,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (!BlockOverlayMod.isEnabled()) {
            return;
        }

        ci.cancel();
        BlockOverlayRenderer.render(poseStack, consumer, camX, camY, camZ, pos, state);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void flowclient$renderBlockAnimations(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        BlockAnimationController.render(poseStack, camera, partialTick);
    }
}