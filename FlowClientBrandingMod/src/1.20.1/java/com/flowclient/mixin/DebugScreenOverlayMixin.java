package com.flowclient.mixin;

import com.flowclient.mods.ModifyF3Mod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void flowclient$skipVanillaF3(GuiGraphics graphics, CallbackInfo ci) {
        if (ModifyF3Mod.isEnabled()) {
            ci.cancel();
        }
    }
}
