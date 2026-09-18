package com.flowclient.mixin.voicechat;

import com.flowclient.mods.voicechat.VoiceChatDisableGuard;
import de.maxhenkel.voicechat.voice.client.ChatUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatUtils.class, remap = false)
public abstract class VoiceChatChatUtilsMixin {
    @Inject(method = "sendModErrorMessage(Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockModErrorMessage(String title, String message, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "sendModErrorMessage(Ljava/lang/String;Ljava/lang/Exception;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockModErrorException(String title, Exception exception, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "sendModErrorMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockModError(String message, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "sendModMessage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockModMessage(Component message, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "sendPlayerMessage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flowclient$blockPlayerMessage(Component message, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }
}
