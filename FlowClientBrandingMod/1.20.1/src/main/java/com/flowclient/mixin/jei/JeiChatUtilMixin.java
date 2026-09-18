package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiChatFilter;
import mezz.jei.common.util.ChatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatUtil.class, remap = false)
public abstract class JeiChatUtilMixin {
    @Inject(
            method = "writeChatMessage(Lnet/minecraft/class_746;Ljava/lang/String;Lnet/minecraft/class_124;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void flowclient$blockStringMessage(LocalPlayer player, String message, ChatFormatting color, CallbackInfo ci) {
        if (JeiChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }
}
