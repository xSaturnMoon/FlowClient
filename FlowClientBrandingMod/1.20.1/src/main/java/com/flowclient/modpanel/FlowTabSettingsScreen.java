package com.flowclient.modpanel;

import com.flowclient.mods.tab.FlowTabSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FlowTabSettingsScreen extends ModSettingsPanelScreen<FlowTabSettings> {
    private static final Component TITLE = Component.literal("Flow Tab");

    public FlowTabSettingsScreen(Screen parent) {
        super(TITLE, parent, "Flow Tab");
    }

    @Override
    protected FlowTabSettings settings() {
        return FlowTabSettings.get();
    }

    @Override
    protected void persistSettings() {
        FlowTabSettings.get().persist();
    }

    @Override
    protected void buildSettingRows() {
        this.addRow("Player Heads", s -> bool(s.showHeads()), s -> s.setShowHeads(!s.showHeads()), s -> s.resetField("showHeads"));
        this.addRow("Name Align", s -> s.nameAlign().label(), s -> s.setNameAlign(s.nameAlign().next()), s -> s.resetField("nameAlign"));
        this.addRow("Row Spacing", s -> s.rowSpacing() + "px", s -> s.setRowSpacing(cycle(s.rowSpacing(), 0, 8, 1)), s -> s.resetField("rowSpacing"));
        this.addRow("Column Spacing", s -> s.columnGap() + "px", s -> s.setColumnGap(cycle(s.columnGap(), 0, 15, 1)), s -> s.resetField("columnGap"));
        this.addRow("Sort Order", s -> s.sort().label(), s -> s.setSort(s.sort().next()), s -> s.resetField("sort"));
        this.addRow("Position", s -> s.position().label(), s -> s.setPosition(s.position().next()), s -> s.resetField("position"));

        this.addHeader("Cinematic Mode");
        this.addRow("Cinematic Tab", s -> bool(s.cinematicMode()), s -> s.setCinematicMode(!s.cinematicMode()), s -> s.resetField("cinematicMode"));
        this.addRow("Background Dim", s -> s.cinematicDimOpacity() + "%", s -> s.setCinematicDimOpacity(cycle(s.cinematicDimOpacity(), 0, 90, 5)), s -> s.resetField("cinematicDimOpacity"));
        this.addRow("Background Blur", s -> bool(s.cinematicBlur()), s -> s.setCinematicBlur(!s.cinematicBlur()), s -> s.resetField("cinematicBlur"));
        this.addRow("Blur Strength", s -> Integer.toString(s.cinematicBlurStrength()), s -> s.setCinematicBlurStrength(cycle(s.cinematicBlurStrength(), 0, 10, 1)), s -> s.resetField("cinematicBlurStrength"));
        this.addRow("Zoom Amount", s -> s.cinematicZoomPercent() + "%", s -> s.setCinematicZoomPercent(cycle(s.cinematicZoomPercent(), 0, 20, 1)), s -> s.resetField("cinematicZoomPercent"));
        this.addRow("Transition Speed", s -> s.cinematicTransitionMs() + "ms", s -> s.setCinematicTransitionMs(cycle(s.cinematicTransitionMs(), 80, 600, 20)), s -> s.resetField("cinematicTransitionMs"));
        this.addRow("Hide Other HUD", s -> bool(s.cinematicHideHud()), s -> s.setCinematicHideHud(!s.cinematicHideHud()), s -> s.resetField("cinematicHideHud"));
        this.addRow("Vignette", s -> bool(s.cinematicVignette()), s -> s.setCinematicVignette(!s.cinematicVignette()), s -> s.resetField("cinematicVignette"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
