package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientDetector;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(SkinManager.class)
public abstract class SkinManagerMixin {

    @Inject(method = "get", at = @At("RETURN"))
    private void flowclient$onSkinLoaded(
            GameProfile profile,
            CallbackInfoReturnable<CompletableFuture<Optional<net.minecraft.world.entity.player.PlayerSkin>>> cir
    ) {
        CompletableFuture<Optional<net.minecraft.world.entity.player.PlayerSkin>> future = cir.getReturnValue();
        if (future == null || profile == null) return;

        future.thenAccept(optional -> {
            if (optional.isEmpty()) return;
            Minecraft.getInstance().execute(() -> {
                var connection = Minecraft.getInstance().getConnection();
                if (connection == null) return;
                PlayerInfo info = connection.getPlayerInfo(profile.id());
                if (info != null) {
                    ClientDetector.inspectTextures(profile.id(), info);
                }
            });
        });
    }
}
