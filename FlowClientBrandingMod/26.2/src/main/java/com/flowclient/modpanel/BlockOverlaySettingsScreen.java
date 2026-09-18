package com.flowclient.modpanel;

import com.flowclient.mods.render.BlockOverlaySettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BlockOverlaySettingsScreen extends ModSettingsPanelScreen<BlockOverlaySettings> {
    private static final Component TITLE = Component.literal("Block Overlay");

    public BlockOverlaySettingsScreen(Screen parent) {
        super(TITLE, parent, "Block Overlay");
    }

    @Override
    protected BlockOverlaySettings settings() {
        return BlockOverlaySettings.get();
    }

    @Override
    protected void persistSettings() {
        BlockOverlaySettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Style");
        this.addRow("Render Style", s -> s.style().label(), s -> s.setStyle(s.style().next()), s -> s.resetField("style"));
        this.addRow("Pulse", s -> bool(s.pulse()), s -> s.setPulse(!s.pulse()), s -> s.resetField("pulse"));
        this.addRow("Rainbow", s -> bool(s.rainbow()), s -> s.setRainbow(!s.rainbow()), s -> s.resetField("rainbow"));
        this.addRow("Dark Outline", s -> bool(s.outline()), s -> s.setOutline(!s.outline()), s -> s.resetField("outline"));

        this.addHeader("Colors");
        this.addRow("Line Color", s -> s.lineColor().label(), s -> s.setLineColor(s.lineColor().next()), s -> s.resetField("lineColor"));
        this.addRow("Glow Color", s -> s.glowColor().label(), s -> s.setGlowColor(s.glowColor().next()), s -> s.resetField("glowColor"));
        this.addRow("Line Opacity", s -> s.lineOpacity() + "%", s -> s.setLineOpacity(cycle(s.lineOpacity(), 0, 100, 10)), s -> s.resetField("lineOpacity"));
        this.addRow("Glow Opacity", s -> s.glowOpacity() + "%", s -> s.setGlowOpacity(cycle(s.glowOpacity(), 0, 100, 10)), s -> s.resetField("glowOpacity"));

        this.addHeader("Shape");
        this.addRow("Line Width", s -> String.format("%.1f", s.lineWidth()), s -> s.setLineWidth(cycleFloat(s.lineWidth(), 0.5F, 8.0F, 0.5F)), s -> s.resetField("lineWidth"));
        this.addRow("Glow Width", s -> String.format("%.1f", s.glowWidth()), s -> s.setGlowWidth(cycleFloat(s.glowWidth(), 1.0F, 12.0F, 0.5F)), s -> s.resetField("glowWidth"));
        this.addRow("Corner Length", s -> s.cornerLength() + "px", s -> s.setCornerLength(cycle(s.cornerLength(), 2, 16, 1)), s -> s.resetField("cornerLength"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        return current + step > max ? min : current + step;
    }

    private static float cycleFloat(float current, float min, float max, float step) {
        return current + step > max ? min : current + step;
    }
}
