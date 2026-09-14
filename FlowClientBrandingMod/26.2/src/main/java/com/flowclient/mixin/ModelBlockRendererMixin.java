package com.flowclient.mixin;

import com.flowclient.mods.render.BlockAnimationController;
import com.flowclient.mods.render.BlockAnimationMod;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses static block tessellation while BlockAnimationController
 * has an active falling-block animation at that position.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    @Inject(
        method = "tesselateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void flowclient$hideAnimatingBlock(
            BlockQuadOutput output,
            float x, float y, float z,
            BlockAndTintGetter getter,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed,
            CallbackInfo ci
    ) {
        if (BlockAnimationMod.isEnabled() && BlockAnimationController.isAnimating(pos)) {
            ci.cancel();
        }
    }
}