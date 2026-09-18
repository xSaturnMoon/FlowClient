package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientDetector;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SkinManager.class)
public abstract class SkinManagerMixin {
    @ModifyVariable(
            method = "registerSkins(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/resources/SkinManager$SkinTextureCallback;Z)V",
            at = @At("HEAD"),
            index = 2
    )
    private SkinManager.SkinTextureCallback flowclient$wrapSkinCallback(
            SkinManager.SkinTextureCallback original,
            GameProfile profile
    ) {
        return (MinecraftProfileTexture.Type type, ResourceLocation location, MinecraftProfileTexture profileTexture) -> {
            original.onSkinTextureAvailable(type, location, profileTexture);
            Minecraft.getInstance().execute(() -> {
                var connection = Minecraft.getInstance().getConnection();
                if (connection == null || profile == null) {
                    return;
                }
                PlayerInfo info = connection.getPlayerInfo(profile.getId());
                if (info != null) {
                    ClientDetector.inspectTextures(profile.getId(), info);
                }
            });
        };
    }
}
