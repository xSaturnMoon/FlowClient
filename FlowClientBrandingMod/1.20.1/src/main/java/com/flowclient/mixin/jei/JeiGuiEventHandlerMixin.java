package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.events.GuiEventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiEventHandler.class, remap = false)
public abstract class JeiGuiEventHandlerMixin {
    @Inject(method = "onDrawBackgroundPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockBackgroundDraw(Screen screen, GuiGraphics graphics, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onDrawForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockForegroundDraw(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX,
            int mouseY, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onDrawScreenPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockScreenDraw(Screen screen, GuiGraphics graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockClientTick(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }
}
