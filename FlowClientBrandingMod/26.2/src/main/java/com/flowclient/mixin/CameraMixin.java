package com.flowclient.mixin;

import com.flowclient.mods.tab.FlowTabCinematicController;
import com.flowclient.mods.zoom.ZoomController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("HEAD"))
    private void flowclient$prepareZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        ZoomController.prepareFov();
        FlowTabCinematicController.prepareStrength();
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void flowclient$applyWorldZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float modified = ZoomController.modifyFov(cir.getReturnValueF(), true);
        modified = FlowTabCinematicController.modifyFov(modified);
        if (modified != cir.getReturnValueF()) {
            cir.setReturnValue(modified);
        }
    }

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void flowclient$applyHudZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float modified = ZoomController.modifyFov(cir.getReturnValueF(), false);
        modified = FlowTabCinematicController.modifyFov(modified);
        if (modified != cir.getReturnValueF()) {
            cir.setReturnValue(modified);
        }
    }
}
