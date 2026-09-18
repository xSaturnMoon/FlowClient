package com.flowclient.mixin;

import com.flowclient.mods.render.BlockOverlayMod;
import com.flowclient.mods.render.BlockOverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void flowclient$customBlockOutline(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci
    ) {
        if (!BlockOverlayMod.isEnabled()) {
            return;
        }

        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        if (state == null) {
            return;
        }

        ci.cancel();
        BlockOverlayRenderer.render(poseStack, submitNodeCollector, levelRenderState, state);
    }
}
