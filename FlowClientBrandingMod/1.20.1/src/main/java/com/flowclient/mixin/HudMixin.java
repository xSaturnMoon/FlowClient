package com.flowclient.mixin;

import com.flowclient.f3.FlowF3Animation;
import com.flowclient.f3.FlowF3Renderer;
import com.flowclient.mods.ModifyF3Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void flowclient$renderCustomF3(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (!ModifyF3Mod.isEnabled()) {
            return;
        }

        FlowF3Animation.frame(this.minecraft);
        if (!FlowF3Animation.shouldRender()) {
            return;
        }

        FlowF3Renderer.render(this.minecraft, graphics);
    }
}
