package com.flowclient.mixin.voicechat;

import com.flowclient.mods.voicechat.VoiceChatDisableGuard;
import de.maxhenkel.voicechat.voice.client.PTTKeyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PTTKeyHandler.class, remap = false)
public abstract class VoiceChatPTTKeyHandlerMixin {
    @Inject(method = "onKeyboardEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockKeyboardPtt(long window, int key, int action, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockMousePtt(long window, int button, int action, int modifiers, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }
}
