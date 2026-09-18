package com.flowclient.modpanel;

import com.flowclient.mods.fps.FpsCounterSettings;
import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FpsCounterSettingsScreen extends ModSettingsPanelScreen<FpsCounterSettings> {
    private static final Component TITLE = Component.literal("FPS Counter");

    public FpsCounterSettingsScreen(Screen parent) {
        super(TITLE, parent, "FPS Counter");
    }

    @Override
    protected FpsCounterSettings settings() {
        return FpsCounterSettings.get();
    }

    @Override
    protected void persistSettings() {
        FpsCounterSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.FPS_COUNTER);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Display");
        this.addRow("Format", s -> s.format().label(), s -> s.setFormat(s.format().next()), s -> s.resetField("format"));
        this.addRow("Color Mode", s -> s.colorMode().label(), s -> s.setColorMode(s.colorMode().next()), s -> s.resetField("colorMode"));
        this.addRow("FPS Smoothing", s -> bool(s.smoothing()), s -> s.setSmoothing(!s.smoothing()), s -> s.resetField("smoothing"));
        this.addRow("Bold Numbers", s -> bool(s.boldNumbers()), s -> s.setBoldNumbers(!s.boldNumbers()), s -> s.resetField("boldNumbers"));

        this.addHeader("Thresholds");
        this.addRow("Low FPS", s -> Integer.toString(s.lowThreshold()), s -> s.setLowThreshold(cycle(s.lowThreshold(), 1, 240, 5)), s -> s.resetField("lowThreshold"));
        this.addRow("Mid FPS", s -> Integer.toString(s.midThreshold()), s -> s.setMidThreshold(cycle(s.midThreshold(), 1, 240, 5)), s -> s.resetField("midThreshold"));
        this.addRow("High FPS", s -> Integer.toString(s.highThreshold()), s -> s.setHighThreshold(cycle(s.highThreshold(), 1, 240, 5)), s -> s.resetField("highThreshold"));

        this.addHeader("Appearance");
        this.addRow("Background", s -> bool(s.showBackground()), s -> s.setShowBackground(!s.showBackground()), s -> s.resetField("showBackground"));
        this.addRow("Background Opacity", s -> s.backgroundOpacity() + "%", s -> s.setBackgroundOpacity(cycle(s.backgroundOpacity(), 0, 100, 10)), s -> s.resetField("backgroundOpacity"));
        this.addRow("Text Shadow", s -> bool(s.textShadow()), s -> s.setTextShadow(!s.textShadow()), s -> s.resetField("textShadow"));
        this.addRow("Outline", s -> bool(s.showOutline()), s -> s.setShowOutline(!s.showOutline()), s -> s.resetField("showOutline"));
        this.addRow("Padding X", s -> Integer.toString(s.paddingX()), s -> s.setPaddingX(cycle(s.paddingX(), 0, 16, 1)), s -> s.resetField("paddingX"));
        this.addRow("Padding Y", s -> Integer.toString(s.paddingY()), s -> s.setPaddingY(cycle(s.paddingY(), 0, 12, 1)), s -> s.resetField("paddingY"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
