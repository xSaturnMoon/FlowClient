package com.flowclient.mixin;

import com.flowclient.mods.render.BlockAnimationController;
import com.flowclient.mods.freelook.FreelookController;
import com.flowclient.mods.otherclient.ClientDetector;
import com.flowclient.mods.otherclient.OtherClientMod;
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
        BlockAnimationController.tick();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void flowclient$otherClientScan(CallbackInfo ci) {
        if (!OtherClientMod.isEnabled()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.tickCount % 200 != 0) return;
        ClientDetector.scanOnlinePlayers();
    }
}
