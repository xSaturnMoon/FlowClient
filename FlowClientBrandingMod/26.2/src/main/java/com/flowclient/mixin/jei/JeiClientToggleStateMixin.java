package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.common.config.ClientToggleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientToggleState.class, remap = false)
public abstract class JeiClientToggleStateMixin {
    @Inject(method = "toggleOverlayEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockOverlayToggle(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "toggleBookmarkEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockBookmarkToggle(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "toggleCheatItemsEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockCheatToggle(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "toggleEditModeEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockEditToggle(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "setBookmarkEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockSetBookmark(boolean enabled, CallbackInfo ci) {
        if (JeiDisableGuard.shouldBlock() && enabled) {
            ci.cancel();
        }
    }

    @Inject(method = "setCheatItemsEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockSetCheat(boolean enabled, CallbackInfo ci) {
        if (JeiDisableGuard.shouldBlock() && enabled) {
            ci.cancel();
        }
    }
}
