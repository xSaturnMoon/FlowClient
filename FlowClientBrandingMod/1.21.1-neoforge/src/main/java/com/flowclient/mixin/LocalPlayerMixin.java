package com.flowclient.mixin;

import com.flowclient.mods.freelook.FreelookController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void flowclient$freelookLockBody(CallbackInfo ci) {
        FreelookController.maintainBodyOrientation();
    }
}
