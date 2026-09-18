package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.render.ScoreboardAnchor;
import com.flowclient.mods.render.ScoreboardRenderer;
import com.flowclient.mods.render.ScoreboardSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScoreboardSettingsScreen extends ModSettingsPanelScreen<ScoreboardSettings> {
    private static final Component TITLE = Component.literal("Scoreboard");

    public ScoreboardSettingsScreen(Screen parent) {
        super(TITLE, parent, "Scoreboard");
    }

    @Override
    protected ScoreboardSettings settings() {
        return ScoreboardSettings.get();
    }

    @Override
    protected void persistSettings() {
        ScoreboardSettings.get().persist();
    }

    @Override
    protected int extraRowsReserved() {
        return 1;
    }

    @Override
    protected void addExtraWidgets(int frameX, int frameY, int frameWidth, int visibleHeight, int startIndex) {
        this.addHudPositionButton(frameX, frameY, frameWidth, visibleHeight, startIndex, HudElement.SCOREBOARD);
    }

    @Override
    protected void buildSettingRows() {
        this.addHeader("Appearance");
        this.addRow("Background Opacity", s -> s.backgroundOpacity() + "%", s -> s.setBackgroundOpacity(cycle(s.backgroundOpacity(), 0, 100, 10)), s -> s.resetField("backgroundOpacity"));
        this.addRow("Hide Numbers", s -> bool(s.hideNumbers()), s -> s.setHideNumbers(!s.hideNumbers()), s -> s.resetField("hideNumbers"));
        this.addRow("Center Text", s -> bool(s.centerText()), s -> s.setCenterText(!s.centerText()), s -> s.resetField("centerText"));

        this.addHeader("Position");
        this.addRow("Screen Anchor", s -> s.anchor().label(), s -> {
            ScoreboardAnchor next = s.anchor().next();
            if (next == ScoreboardAnchor.CUSTOM) {
                ScoreboardRenderer.seedCustomLayoutFromCurrentAnchor();
            }
            s.setAnchor(next);
        }, s -> s.resetField("anchor"));
        this.addRow("Side Margin", s -> s.sideMargin() + "px", s -> s.setSideMargin(cycle(s.sideMargin(), 0, 40, 2)), s -> s.resetField("sideMargin"));

        this.addHeader("Colors");
        this.addRow("Use Team Colors", s -> bool(s.useTeamColors()), s -> s.setUseTeamColors(!s.useTeamColors()), s -> s.resetField("useTeamColors"));
        this.addRow("Title Color", s -> s.titleColor().label(), s -> s.setTitleColor(s.titleColor().next()), s -> s.resetField("titleColor"));
        this.addRow("Text Color", s -> s.textColor().label(), s -> s.setTextColor(s.textColor().next()), s -> s.resetField("textColor"));
        this.addRow("Score Color", s -> s.scoreColor().label(), s -> s.setScoreColor(s.scoreColor().next()), s -> s.resetField("scoreColor"));
    }

    private static String bool(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private static int cycle(int current, int min, int max, int step) {
        int next = current + step;
        return next > max ? min : next;
    }
}
