package com.flowclient.mixin;

import com.flowclient.mods.immersion.PortalFxController;
import com.flowclient.mods.advancements.AdvancementsUnlockController;
import com.flowclient.mods.advancements.AdvancementsUnlockMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerRespawnMixin {
    @Unique
    private ResourceKey<Level> flowclient$dimensionBeforeRespawn;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void flowclient$captureDimension(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            this.flowclient$dimensionBeforeRespawn = client.player.level().dimension();
        }
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void flowclient$trackDimensionChange(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || this.flowclient$dimensionBeforeRespawn == null) {
            return;
        }
        ResourceKey<Level> to = client.player.level().dimension();
        if (!this.flowclient$dimensionBeforeRespawn.equals(to)) {
            if (AdvancementsUnlockMod.isEnabled()) {
                AdvancementsUnlockController.get().onDimensionChange(this.flowclient$dimensionBeforeRespawn, to);
            }
            PortalFxController.onDimensionChange(this.flowclient$dimensionBeforeRespawn, to);
        }
        this.flowclient$dimensionBeforeRespawn = null;
    }
}
