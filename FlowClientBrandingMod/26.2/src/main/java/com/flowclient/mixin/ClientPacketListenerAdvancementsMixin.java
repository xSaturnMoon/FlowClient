package com.flowclient.mixin;

import com.flowclient.mods.advancements.AdvancementsUnlockMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerAdvancementsMixin {
    @Inject(method = "handleUpdateAdvancementsPacket", at = @At("HEAD"), cancellable = true)
    private void flowclient$blockAdvancementReset(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (!AdvancementsUnlockMod.isEnabled()) {
            return;
        }

        if (packet.shouldReset()) {
            ci.cancel();
        }
    }
}
