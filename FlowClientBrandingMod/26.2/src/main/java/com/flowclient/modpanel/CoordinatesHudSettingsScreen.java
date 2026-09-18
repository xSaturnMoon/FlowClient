package com.flowclient.modpanel;

import com.flowclient.mods.coordinates.CoordinatesHudSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CoordinatesHudSettingsScreen extends ModSettingsPanelScreen<CoordinatesHudSettings> {
    private static final Component TITLE = Component.literal("Coordinates HUD");

    public CoordinatesHudSettingsScreen(Screen parent) {
        super(TITLE, parent, "Coordinates HUD");
    }

    @Override
    protected CoordinatesHudSettings settings() {
        return CoordinatesHudSettings.get();
    }

    @Override
    protected void persistSettings() {
        CoordinatesHudSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.COORDINATES_HUD);
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Show facing", s -> onOff(s.showFacing()), s -> s.toggleShowFacing(), CoordinatesHudSettings::resetShowFacing);
        this.addRow("Background", s -> onOff(s.showBackground()), s -> s.toggleShowBackground(), CoordinatesHudSettings::resetShowBackground);
        this.addRow("Background opacity", s -> s.backgroundOpacity() + "%", s -> s.cycleBackgroundOpacity(), CoordinatesHudSettings::resetBackgroundOpacity);
        this.addRow("Text shadow", s -> onOff(s.textShadow()), s -> s.toggleTextShadow(), CoordinatesHudSettings::resetTextShadow);
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.cyclePaddingX(), CoordinatesHudSettings::resetPaddingX);
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.cyclePaddingY(), CoordinatesHudSettings::resetPaddingY);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
