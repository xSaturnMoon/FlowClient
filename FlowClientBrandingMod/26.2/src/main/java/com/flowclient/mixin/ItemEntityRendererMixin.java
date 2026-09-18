package com.flowclient.mixin;

import com.flowclient.mods.render.ItemPhysicsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    /**
     * Step 1: when Item Physics is ON, zero out the bobOffset stored in
     * the render state so the vanilla hover/bobbing animation disappears.
     */
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
        at = @At("RETURN")
    )
    private void flowclient$stopBobbing(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (ItemPhysicsMod.isEnabled()) {
            state.bobOffset = 0.0f;
        }
    }

    /**
     * Step 2: before vanilla renders the item, apply a custom PoseStack
     * transform so it lies flat on the ground.
     *
     * Vanilla item rendering (at this point) is already positioned at the
     * entity's feet. Vanilla then:
     *   - translates up by a minimum hover height (~0.25)
     *   - translates up by bobOffset (now 0, so no issue)
     *   - rotates Y for spinning
     *
     * We push a pose here and pop it on RETURN so we scope our changes.
     * We translate DOWN by 0.25 (vanilla's min hover) so the item sits on
     * the surface, then rotate 90° on X to lay it flat.
     */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void flowclient$beforeSubmit(ItemEntityRenderState state, PoseStack poseStack,
                                          SubmitNodeCollector nodeCollector, CameraRenderState cameraState,
                                          CallbackInfo ci) {
        if (ItemPhysicsMod.isEnabled()) {
            poseStack.pushPose();
            // cancel the minimum vanilla hover height so it sits on the ground
            poseStack.translate(0.0f, -0.25f, 0.0f);
            // lay the item flat (rotate 90° around X axis)
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN"))
    private void flowclient$afterSubmit(ItemEntityRenderState state, PoseStack poseStack,
                                         SubmitNodeCollector nodeCollector, CameraRenderState cameraState,
                                         CallbackInfo ci) {
        if (ItemPhysicsMod.isEnabled()) {
            poseStack.popPose();
        }
    }
}
