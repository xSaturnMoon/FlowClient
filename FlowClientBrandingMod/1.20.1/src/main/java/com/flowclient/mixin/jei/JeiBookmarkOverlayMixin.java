package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BookmarkOverlay.class, remap = false)
public abstract class JeiBookmarkOverlayMixin {
    @Inject(method = "isListDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$hideList(CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawScreen(Minecraft minecraft, GuiGraphics graphics, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawTooltips(Minecraft minecraft, GuiGraphics graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawOnForeground(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }
}
