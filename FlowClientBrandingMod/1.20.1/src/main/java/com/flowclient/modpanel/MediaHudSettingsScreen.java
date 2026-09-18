package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.media.MediaHudSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MediaHudSettingsScreen extends ModSettingsPanelScreen<MediaHudSettings> {
    private static final Component TITLE = Component.literal("Media HUD");

    public MediaHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Media HUD");
    }

    @Override
    protected MediaHudSettings settings() {
        return MediaHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        MediaHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.MEDIA_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Show artist", s -> s.showArtist() ? "ON" : "OFF", s -> s.toggleShowArtist(), MediaHudSettings::resetShowArtist);
        this.addRow("Hide when idle", s -> s.hideWhenIdle() ? "ON" : "OFF", s -> s.toggleHideWhenIdle(), MediaHudSettings::resetHideWhenIdle);
        this.addRow("Background", s -> s.showBackground() ? "ON" : "OFF", s -> s.toggleShowBackground(), MediaHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), MediaHudSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> s.textShadow() ? "ON" : "OFF", s -> s.toggleTextShadow(), MediaHudSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.setPaddingX(cycle(s.paddingX(), 0, 20, 1)), MediaHudSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.setPaddingY(cycle(s.paddingY(), 0, 16, 1)), MediaHudSettings::resetPaddingY);
        this.addRow("Max text width", s -> Integer.toString(s.maxTextWidth()), s -> s.setMaxTextWidth(cycle(s.maxTextWidth(), 80, 320, 10)), MediaHudSettings::resetMaxTextWidth);
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
