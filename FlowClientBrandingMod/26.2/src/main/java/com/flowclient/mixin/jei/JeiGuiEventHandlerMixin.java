package com.flowclient.mixin.jei;

import com.flowclient.mods.jei.JeiDisableGuard;
import mezz.jei.gui.events.GuiEventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiEventHandler.class, remap = false)
public abstract class JeiGuiEventHandlerMixin {
    @Inject(method = "drawForContainerScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockContainerDraw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX,
            int mouseY, CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "drawForScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockScreenDraw(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockClientTick(CallbackInfo ci) {
        JeiDisableGuard.cancelVoid(ci);
    }
}
