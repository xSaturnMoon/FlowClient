package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.ghost.GhostIngredientDragManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GhostIngredientDragManager.class, remap = false)
public abstract class JeiGhostIngredientDragManagerMixin {
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
