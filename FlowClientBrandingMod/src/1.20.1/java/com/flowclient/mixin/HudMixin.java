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

    @Inject(method = "renderDebugOverlay", at = @At("RETURN"))
    private void flowclient$renderCustomF3(GuiGraphics graphics, CallbackInfo ci) {
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
