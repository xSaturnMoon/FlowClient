package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.overlay.IngredientListOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IngredientListOverlay.class, remap = false)
public abstract class JeiIngredientListOverlayMixin {
    @Inject(method = "isListDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$hideList(CallbackInfoReturnable<Boolean> cir) {
        JeiDisableGuard.logListDisplayedCheck();
        JeiDisableGuard.cancelFalse(cir);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawScreen(Minecraft minecraft, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawForeground(Minecraft minecraft, GuiGraphicsExtractor graphics, int mouseX,
            int mouseY, float partialTicks, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawTooltips(Minecraft minecraft, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockDrawOnForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockTick(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }
}
