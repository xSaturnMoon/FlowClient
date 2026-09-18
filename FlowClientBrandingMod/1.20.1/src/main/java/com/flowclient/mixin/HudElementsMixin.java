package com.flowclient.mixin;

import com.flowclient.mods.armordurability.ArmorDurabilityAlertRenderer;
import com.flowclient.mods.armor.ArmorHudRenderer;
import com.flowclient.mods.battery.BatteryHudRenderer;
import com.flowclient.mods.blockbreak.BlockBreakProgressRenderer;
import com.flowclient.mods.blockspeed.BlockSpeedRenderer;
import com.flowclient.mods.clock.FlowClockRenderer;
import com.flowclient.mods.coordinates.CoordinatesHudRenderer;
import com.flowclient.mods.cooldown.ItemCooldownHudRenderer;
import com.flowclient.mods.fps.FpsCounterRenderer;
import com.flowclient.mods.health.HealthBarRenderer;
import com.flowclient.mods.immersion.ImmersionHudRenderer;
import com.flowclient.mods.keystrokes.KeystrokesRenderer;
import com.flowclient.mods.media.MediaHudRenderer;
import com.flowclient.mods.ping.PingHudRenderer;
import com.flowclient.mods.render.CrosshairMod;
import com.flowclient.mods.render.CrosshairRenderer;
import com.flowclient.mods.render.DamageIndicatorRenderer;
import com.flowclient.mods.render.ScoreboardMod;
import com.flowclient.mods.render.ScoreboardRenderer;
import com.flowclient.mods.serveraddress.ServerAddressHudRenderer;
import com.flowclient.mods.tab.FlowTabCinematicController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.scores.Objective;

@Mixin(Gui.class)
public abstract class HudElementsMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract Font getFont();

    @Inject(method = "render", at = @At("RETURN"))
    private void flowclient$renderHudExtras(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (this.minecraft.player == null) {
            return;
        }
        if (FlowTabCinematicController.shouldHideHud()) {
            return;
        }

        KeystrokesRenderer.render(this.minecraft, graphics, this.getFont());
        ArmorHudRenderer.render(this.minecraft, graphics, this.getFont());
        FpsCounterRenderer.render(this.minecraft, graphics, this.getFont());
        FlowClockRenderer.render(this.minecraft, graphics, this.getFont());
        HealthBarRenderer.render(this.minecraft, graphics, this.getFont());
        BlockSpeedRenderer.render(this.minecraft, graphics, this.getFont());
        BlockBreakProgressRenderer.render(this.minecraft, graphics, this.getFont());
        ArmorDurabilityAlertRenderer.render(this.minecraft, graphics, this.getFont());
        ItemCooldownHudRenderer.render(this.minecraft, graphics, this.getFont());
        ServerAddressHudRenderer.render(this.minecraft, graphics, this.getFont());
        BatteryHudRenderer.render(this.minecraft, graphics, this.getFont());
        CoordinatesHudRenderer.render(this.minecraft, graphics, this.getFont());
        PingHudRenderer.render(this.minecraft, graphics, this.getFont());
        MediaHudRenderer.render(this.minecraft, graphics, this.getFont());
        DamageIndicatorRenderer.render(this.minecraft, graphics, this.getFont());
        ImmersionHudRenderer.render(this.minecraft, graphics, this.getFont());
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void flowclient$cancelVanillaCrosshair(GuiGraphics graphics, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
            return;
        }
        if (!CrosshairMod.isEnabled()) {
            return;
        }
        ci.cancel();
        CrosshairRenderer.render(this.minecraft, graphics);
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void flowclient$cancelVanillaScoreboard(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        if (FlowTabCinematicController.shouldHideHud()) {
            ci.cancel();
            return;
        }
        if (!ScoreboardMod.isEnabled()) {
            return;
        }
        ci.cancel();
        ScoreboardRenderer.render(this.minecraft, graphics, this.getFont());
    }
}
