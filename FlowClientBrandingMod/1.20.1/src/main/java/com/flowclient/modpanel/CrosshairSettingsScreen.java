package com.flowclient.modpanel;

import com.flowclient.mods.render.CrosshairSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CrosshairSettingsScreen extends ModSettingsPanelScreen<CrosshairSettings> {
    private static final Component TITLE = Component.literal("Crosshair");

    public CrosshairSettingsScreen(Screen parent) {
        super(TITLE, parent, "Crosshair");
    }

    @Override
    protected CrosshairSettings settings() {
        return CrosshairSettings.get();
    }

    @Override
    protected void persistSettings() {
        CrosshairSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Style");
        this.addRow("Crosshair Style", s -> s.style().label(), s -> s.setStyle(s.style().next()), s -> s.resetField("style"));
        this.addRow("Size", s -> Integer.toString(s.size()), s -> s.setSize(cycle(s.size(), 2, 16, 1)), s -> s.resetField("size"));
        this.addRow("Thickness", s -> Integer.toString(s.thickness()), s -> s.setThickness(cycle(s.thickness(), 1, 4, 1)), s -> s.resetField("thickness"));
        this.addRow("Center Gap", s -> Integer.toString(s.gap()), s -> s.setGap(cycle(s.gap(), 0, 8, 1)), s -> s.resetField("gap"));
        this.addRow("Center Dot", s -> bool(s.dot()), s -> s.setDot(!s.dot()), s -> s.resetField("dot"));
        this.addRow("Dot Size", s -> Integer.toString(s.dotSize()), s -> s.setDotSize(cycle(s.dotSize(), 1, 4, 1)), s -> s.resetField("dotSize"));

        this.addHeader("Colors");
        this.addRow("Color", s -> s.color().label(), s -> s.setColor(s.color().next()), s -> s.resetField("color"));
        this.addRow("Outline Color", s -> s.outlineColor().label(), s -> s.setOutlineColor(s.outlineColor().next()), s -> s.resetField("outlineColor"));
        this.addRow("Opacity", s -> s.opacity() + "%", s -> s.setOpacity(cycle(s.opacity(), 0, 100, 10)), s -> s.resetField("opacity"));
        this.addRow("Outline Opacity", s -> s.outlineOpacity() + "%", s -> s.setOutlineOpacity(cycle(s.outlineOpacity(), 0, 100, 10)), s -> s.resetField("outlineOpacity"));
        this.addRow("Outline", s -> bool(s.outline()), s -> s.setOutline(!s.outline()), s -> s.resetField("outline"));
        this.addRow("Rainbow", s -> bool(s.rainbow()), s -> s.setRainbow(!s.rainbow()), s -> s.resetField("rainbow"));
        this.addRow("Entity Highlight", s -> bool(s.dynamicColor()), s -> s.setDynamicColor(!s.dynamicColor()), s -> s.resetField("dynamicColor"));
        this.addRow("Show In F3", s -> bool(s.showWhileDebug()), s -> s.setShowWhileDebug(!s.showWhileDebug()), s -> s.resetField("showWhileDebug"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        return current + step > max ? min : current + step;
    }
}
