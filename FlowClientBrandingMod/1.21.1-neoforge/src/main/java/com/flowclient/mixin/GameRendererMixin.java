package com.flowclient.mixin;

import com.flowclient.mods.zoom.ZoomController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("HEAD"))
    private void flowclient$prepareZoom(
            net.minecraft.client.Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Double> cir) {
        ZoomController.prepareFov();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void flowclient$applyZoom(
            net.minecraft.client.Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Double> cir) {
        double modified = ZoomController.modifyFov(cir.getReturnValue(), changingFov);
        if (modified != cir.getReturnValue()) {
            cir.setReturnValue(modified);
        }
    }
}
