package com.flowclient.mixin;

import com.flowclient.mods.freelook.FreelookController;
import com.flowclient.mods.zoom.ZoomController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void flowclient$scrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomController.handleScroll(vertical);
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void flowclient$freelookTurn(LocalPlayer player, double yawDelta, double pitchDelta) {
        if (!FreelookController.applyTurnDelta(yawDelta, pitchDelta)) {
            player.turn(yawDelta, pitchDelta);
        }
    }
}
