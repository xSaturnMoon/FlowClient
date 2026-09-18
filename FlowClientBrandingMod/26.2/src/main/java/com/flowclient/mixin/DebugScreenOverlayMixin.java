package com.flowclient.mixin;

import com.flowclient.f3.FlowF3Renderer;
import com.flowclient.mods.ModifyF3Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private void extractLines(GuiGraphicsExtractor graphics, List<String> lines, boolean leftAlign) {
        throw new AssertionError();
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V"
            )
    )
    private void flowclient$redirectExtractLines(
            DebugScreenOverlay overlay,
            GuiGraphicsExtractor graphics,
            List<String> lines,
            boolean leftAlign
    ) {
        if (!ModifyF3Mod.isEnabled()) {
            this.extractLines(graphics, lines, leftAlign);
        }
    }
}
