package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiChatFilter;
import mezz.jei.common.util.ChatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatUtil.class, remap = false)
public abstract class JeiChatUtilMixin {
    @Inject(method = "writeChatMessage(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;Lnet/minecraft/ChatFormatting;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockStringMessage(Player player, String message, ChatFormatting color, CallbackInfo ci) {
        if (JeiChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "writeChatMessage(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockComponentMessage(Player player, Component message, CallbackInfo ci) {
        if (JeiChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }
}
