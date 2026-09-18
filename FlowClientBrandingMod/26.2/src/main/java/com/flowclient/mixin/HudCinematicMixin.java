package com.flowclient.mixin;

import com.flowclient.mods.tab.FlowTabCinematicController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Hud.class, priority = 1100)
public abstract class HudCinematicMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Invoker("extractTabList")
    protected abstract void flowclient$extractTabList(GuiGraphicsExtractor graphics, DeltaTracker delta);

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void flowclient$beginHudFrame(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        FlowTabCinematicController.beginHudFrame();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void flowclient$finishCinematicFrame(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (!FlowTabCinematicController.shouldHideHud() || !FlowTabCinematicController.wasTabRenderedThisFrame()) {
            return;
        }

        FlowTabCinematicController.renderCoverOverlay(graphics, this.minecraft);
        this.flowclient$extractTabList(graphics, delta);
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideBossOverlay(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSubtitleOverlay", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSubtitles(GuiGraphicsExtractor graphics, boolean visible, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCameraOverlays", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideCameraOverlays(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSleepOverlay(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideActionBar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideTitle(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractChat", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideChat(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideScoreboard(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideCrosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideEffects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideHotbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideItemHotbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSelectedItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractDemoOverlay", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideDemoOverlay(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void flowclient$hidePlayerHealth(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideVehicleHealth(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSavingIndicator", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSavingIndicator(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }
}
