package com.flowclient.modpanel;

import com.flowclient.mods.zoom.ZoomifySettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ZoomifySettingsScreen extends ModSettingsPanelScreen<ZoomifySettings> {
    private static final Component TITLE = Component.literal("Zoomify");

    public ZoomifySettingsScreen(Screen parent) {
        super(TITLE, parent, "Zoomify");
    }

    @Override
    protected ZoomifySettings settings() {
        return ZoomifySettings.get();
    }

    @Override
    protected void persistSettings() {
        ZoomifySettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Main Zoom Settings");
        this.addRow("Initial Zoom", s -> s.initialZoom() + "x", s -> s.setInitialZoom(nextZoom(s.initialZoom())), s -> s.resetField("initialZoom"));
        this.addRow("Zoom In Time", s -> String.format("%.1f secs", s.zoomInTime()), s -> s.setZoomInTime(nextTime(s.zoomInTime())), s -> s.resetField("zoomInTime"));
        this.addRow("Zoom Out Time", s -> String.format("%.1f secs", s.zoomOutTime()), s -> s.setZoomOutTime(nextTime(s.zoomOutTime())), s -> s.resetField("zoomOutTime"));
        this.addRow("Zoom In Transition", s -> s.zoomInTransition().label(), s -> s.setZoomInTransition(s.zoomInTransition().next()), s -> s.resetField("zoomInTransition"));
        this.addRow("Zoom Out Transition", s -> s.zoomOutTransition().label(), s -> s.setZoomOutTransition(s.zoomOutTransition().next()), s -> s.resetField("zoomOutTransition"));
        this.addRow("Affect Hand FOV", s -> boolLabel(s.affectHandFov()), s -> s.setAffectHandFov(!s.affectHandFov()), s -> s.resetField("affectHandFov"));

        this.addHeader("Scrolling");
        this.addRow("Enable Scroll Zoom", s -> boolLabel(s.enableScrollZoom()), s -> s.setEnableScrollZoom(!s.enableScrollZoom()), s -> s.resetField("enableScrollZoom"));
        this.addRow("Scroll Step Count", s -> Integer.toString(s.scrollStepCount()), s -> s.setScrollStepCount(s.scrollStepCount() >= 50 ? 1 : s.scrollStepCount() + 1), s -> s.resetField("scrollStepCount"));
        this.addRow("Zoom Per Step", s -> s.zoomPerStep() + "x", s -> s.setZoomPerStep(nextStepZoom(s.zoomPerStep())), s -> s.resetField("zoomPerStep"));
        this.addRow("Scroll Zoom Smoothness", s -> Math.round(s.scrollZoomSmoothness() * 100.0) + "%", s -> s.setScrollZoomSmoothness(nextPercent(s.scrollZoomSmoothness())), s -> s.resetField("scrollZoomSmoothness"));
        this.addRow("Remember Zoom Steps", s -> boolLabel(s.rememberZoomSteps()), s -> s.setRememberZoomSteps(!s.rememberZoomSteps()), s -> s.resetField("rememberZoomSteps"));

        this.addHeader("Spyglass");
        this.addRow("Zoom Behavior", s -> s.spyglassZoomBehavior().label(), s -> s.setSpyglassZoomBehavior(s.spyglassZoomBehavior().next()), s -> s.resetField("spyglassZoomBehavior"));
        this.addRow("Overlay Visibility", s -> s.overlayVisibility().label(), s -> s.setOverlayVisibility(s.overlayVisibility().next()), s -> s.resetField("overlayVisibility"));
        this.addRow("Sound Behavior", s -> s.soundBehavior().label(), s -> s.setSoundBehavior(s.soundBehavior().next()), s -> s.resetField("soundBehavior"));
    }

    private static String boolLabel(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static double nextZoom(double current) {
        return cycle(new double[]{2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0}, current);
    }

    private static double nextTime(double current) {
        return cycle(new double[]{0.0, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0}, current);
    }

    private static double nextStepZoom(double current) {
        return cycle(new double[]{1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0}, current);
    }

    private static double nextPercent(double current) {
        return cycle(new double[]{0.0, 0.1, 0.25, 0.5, 0.7, 0.85, 1.0}, current);
    }

    private static double cycle(double[] values, double current) {
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - current) < 0.001) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }
}
