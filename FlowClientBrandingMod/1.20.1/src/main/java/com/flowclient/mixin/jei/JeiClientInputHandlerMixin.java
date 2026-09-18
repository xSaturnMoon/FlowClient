package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.input.ClientInputHandler;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientInputHandler.class, remap = false)
public abstract class JeiClientInputHandlerMixin {
    @Inject(method = "onInitGui", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockInitGui(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onKeyboardKeyPressedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockKeyPre(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "onKeyboardKeyPressedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockKeyPost(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "onKeyboardCharTypedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockCharPre(Screen screen, char character, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "onKeyboardCharTypedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockCharPost(Screen screen, char character, int modifiers, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onGuiMouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockMouseClicked(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "onGuiMouseReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockMouseReleased(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "onGuiMouseScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockMouseScroll(double mouseX, double mouseY, double scrollDelta,
            CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }
}
