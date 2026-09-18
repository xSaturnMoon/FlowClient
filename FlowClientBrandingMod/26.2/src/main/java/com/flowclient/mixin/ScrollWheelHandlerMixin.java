package com.flowclient.mixin;

import com.flowclient.mods.zoom.ZoomController;
import net.minecraft.client.ScrollWheelHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScrollWheelHandler.class)
public abstract class ScrollWheelHandlerMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void flowclient$scrollZoom(double horizontal, double vertical, CallbackInfoReturnable<org.joml.Vector2i> cir) {
        ZoomController.handleScroll(vertical);
    }
}
