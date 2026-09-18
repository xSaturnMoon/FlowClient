package com.flowclient.mixin;

import com.flowclient.mods.freelook.FreelookController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /**
     * Redirect the call to player.turn() inside turnPlayer().
     * At this point yawDelta and pitchDelta are ALREADY multiplied by
     * Minecraft's sensitivity — so we can pass them straight to the camera.
     * We halve them because Minecraft's sensitivity formula produces values
     * that, when fed back into applyTurnDelta, result in double speed
     * compared to normal play in third-person mode (the camera sees both
     * the raw player rotation AND our override every frame).
     */
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
