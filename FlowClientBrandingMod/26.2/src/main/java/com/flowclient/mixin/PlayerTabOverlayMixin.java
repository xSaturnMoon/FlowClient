package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientBadgeHelper;
import com.flowclient.mods.otherclient.OtherClientMod;
import com.flowclient.mods.tab.FlowTabBridge;
import com.flowclient.mods.tab.FlowTabCinematicController;
import com.flowclient.mods.tab.FlowTabMod;
import com.flowclient.mods.tab.FlowTabRenderer;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin implements FlowTabBridge {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private Component footer;

    @Shadow
    private Component header;

    @Shadow
    protected abstract void extractPingIcon(GuiGraphicsExtractor graphics, int width, int x, int y, PlayerInfo info);

    @Invoker("getPlayerInfos")
    protected abstract List<PlayerInfo> flowclient$getPlayerInfos();

    @Invoker("extractTablistHearts")
    protected abstract void flowclient$extractTablistHearts(
            int x,
            int y,
            int width,
            UUID playerId,
            GuiGraphicsExtractor graphics,
            int rowIndex
    );

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void flowclient$renderCustomTab(
            GuiGraphicsExtractor graphics,
            int width,
            Scoreboard scoreboard,
            Objective objective,
            CallbackInfo ci
    ) {
        if (!FlowTabMod.isEnabled()) {
            return;
        }

        FlowTabCinematicController.onTabRendered();
        FlowTabRenderer.render(this, graphics, width, scoreboard, objective);
        ci.cancel();
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void flowclient$triggerTabDetection(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (!OtherClientMod.isEnabled() || info == null) return;

        Component base = cir.getReturnValue() != null
                ? cir.getReturnValue()
                : Component.literal(info.getProfile().name());
        cir.setReturnValue(ClientBadgeHelper.createTabListName(info, base));
    }

    @Override
    public PlayerTabOverlay overlay() {
        return (PlayerTabOverlay) (Object) this;
    }

    @Override
    public Minecraft minecraft() {
        return this.minecraft;
    }

    @Override
    public List<PlayerInfo> players() {
        return this.flowclient$getPlayerInfos();
    }

    @Override
    public Component header() {
        return this.header;
    }

    @Override
    public Component footer() {
        return this.footer;
    }

    @Override
    public Component nameForDisplay(PlayerInfo info) {
        return this.overlay().getNameForDisplay(info);
    }

    @Override
    public void drawPingIcon(GuiGraphicsExtractor graphics, int columnWidth, int rowX, int rowY, PlayerInfo info) {
        this.extractPingIcon(graphics, columnWidth, rowX, rowY, info);
    }

    @Override
    public void drawHearts(int x, int y, int columnWidth, UUID playerId, GuiGraphicsExtractor graphics, int rowIndex) {
        this.flowclient$extractTablistHearts(x, y, columnWidth, playerId, graphics, rowIndex);
    }

    @Override
    public void drawObjectiveScore(
            Objective objective,
            int x,
            int y,
            int columnWidth,
            PlayerInfo info,
            GuiGraphicsExtractor graphics
    ) {
        // Scores are rendered directly in FlowTabRenderer using public scoreboard APIs.
    }
}
