package com.flowclient.mixin.voicechat;

import com.flowclient.mods.voicechat.VoiceChatDisableGuard;
import de.maxhenkel.voicechat.voice.client.KeyEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyEvents.class, remap = false)
public abstract class VoiceChatKeyEventsMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockKeybinds(CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }
}
