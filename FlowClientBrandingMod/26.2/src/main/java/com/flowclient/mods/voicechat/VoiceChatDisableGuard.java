package com.flowclient.mods.voicechat;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class VoiceChatDisableGuard {
    private VoiceChatDisableGuard() {
    }

    public static boolean shouldBlock() {
        return !VoiceChatMod.isEnabled();
    }

    public static void cancelVoid(CallbackInfo ci) {
        if (shouldBlock()) {
            ci.cancel();
        }
    }

    public static void cancelFalse(CallbackInfoReturnable<Boolean> cir) {
        if (shouldBlock()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
