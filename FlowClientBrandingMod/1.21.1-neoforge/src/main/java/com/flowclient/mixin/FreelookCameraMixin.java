package com.flowclient.mixin;

import com.flowclient.mods.freelook.FreelookController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreelookCameraMixin {
    private static boolean applyingFreelook;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setRotation", at = @At("TAIL"))
    private void flowclient$overrideFreelook(float yaw, float pitch, CallbackInfo ci) {
        if (!FreelookController.isEngaged() || applyingFreelook) {
            return;
        }

        applyingFreelook = true;
        this.setRotation(FreelookController.getCameraYaw(), FreelookController.getCameraPitch());
        applyingFreelook = false;
    }
}
