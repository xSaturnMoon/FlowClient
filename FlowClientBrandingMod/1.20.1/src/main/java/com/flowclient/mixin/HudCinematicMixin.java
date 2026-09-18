package com.flowclient.mixin;

import com.flowclient.mods.tab.FlowTabCinematicController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1100)
public abstract class HudCinematicMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private PlayerTabOverlay tabList;

    @Shadow
    private int screenWidth;

    @Inject(method = "render", at = @At("HEAD"))
    private void flowclient$beginHudFrame(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        FlowTabCinematicController.beginHudFrame();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void flowclient$finishCinematicFrame(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (!FlowTabCinematicController.shouldHideHud() || !FlowTabCinematicController.wasTabRenderedThisFrame()) {
            return;
        }

        FlowTabCinematicController.renderCoverOverlay(graphics, this.minecraft);

        if (this.minecraft.level == null) {
            return;
        }

        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST);
        this.tabList.render(graphics, this.screenWidth, scoreboard, objective);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"
            )
    )
    private void flowclient$hideBossOverlay(BossHealthOverlay overlay, GuiGraphics graphics) {
        if (!FlowTabCinematicController.shouldHideHud()) {
            overlay.render(graphics);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/SubtitleOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"
            )
    )
    private void flowclient$hideSubtitles(SubtitleOverlay overlay, GuiGraphics graphics) {
        if (!FlowTabCinematicController.shouldHideHud()) {
            overlay.render(graphics);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;III)V"
            )
    )
    private void flowclient$hideChat(ChatComponent chat, GuiGraphics graphics, int tickCount, int mouseX, int mouseY) {
        if (!FlowTabCinematicController.shouldHideHud()) {
            chat.render(graphics, tickCount, mouseX, mouseY);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideCrosshair(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideScoreboard(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideEffects(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideHotbar(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSelectedItemName(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderDemoOverlay", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideDemoOverlay(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void flowclient$hidePlayerHealth(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideVehicleHealth(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    private void flowclient$hideSavingIndicator(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
        }
    }
}
