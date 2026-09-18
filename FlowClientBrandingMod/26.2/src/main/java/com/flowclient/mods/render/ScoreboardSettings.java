package com.flowclient.mods.render;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ScoreboardSettings {
    private static final ScoreboardSettings DEFAULTS = new ScoreboardSettings();

    private int backgroundOpacity = 70;
    private boolean hideNumbers = false;
    private boolean centerText = true;
    private boolean useTeamColors = true;
    private ScoreboardAnchor anchor = ScoreboardAnchor.RIGHT;
    private int sideMargin = 2;
    private ScoreboardColor titleColor = ScoreboardColor.WHITE;
    private ScoreboardColor textColor = ScoreboardColor.WHITE;
    private ScoreboardColor scoreColor = ScoreboardColor.RED;

    private static ScoreboardSettings current = copy(DEFAULTS);

    private ScoreboardSettings() {
    }

    public static ScoreboardSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("scoreboardSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "scoreboardSettings");
        ScoreboardSettings settings = copy(DEFAULTS);
        settings.backgroundOpacity = clamp(GsonHelper.getAsInt(json, "backgroundOpacity", DEFAULTS.backgroundOpacity), 0, 100);
        settings.hideNumbers = GsonHelper.getAsBoolean(json, "hideNumbers", DEFAULTS.hideNumbers);
        settings.centerText = GsonHelper.getAsBoolean(json, "centerText", DEFAULTS.centerText);
        settings.useTeamColors = GsonHelper.getAsBoolean(json, "useTeamColors", DEFAULTS.useTeamColors);
        settings.anchor = parseAnchor(GsonHelper.getAsString(json, "anchor", DEFAULTS.anchor.name()), DEFAULTS.anchor);
        settings.sideMargin = clamp(GsonHelper.getAsInt(json, "sideMargin", DEFAULTS.sideMargin), 0, 40);
        settings.titleColor = parseColor(GsonHelper.getAsString(json, "titleColor", DEFAULTS.titleColor.name()), DEFAULTS.titleColor);
        settings.textColor = parseColor(GsonHelper.getAsString(json, "textColor", DEFAULTS.textColor.name()), DEFAULTS.textColor);
        settings.scoreColor = parseColor(GsonHelper.getAsString(json, "scoreColor", DEFAULTS.scoreColor.name()), DEFAULTS.scoreColor);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("backgroundOpacity", current.backgroundOpacity);
        json.addProperty("hideNumbers", current.hideNumbers);
        json.addProperty("centerText", current.centerText);
        json.addProperty("useTeamColors", current.useTeamColors);
        json.addProperty("anchor", current.anchor.name());
        json.addProperty("sideMargin", current.sideMargin);
        json.addProperty("titleColor", current.titleColor.name());
        json.addProperty("textColor", current.textColor.name());
        json.addProperty("scoreColor", current.scoreColor.name());
        root.add("scoreboardSettings", json);
    }

    public static void reset(boolean persist) {
        current = copy(DEFAULTS);
        if (persist) {
            FlowModConfig.save();
        }
    }

    public void persist() {
        FlowModConfig.save();
    }

    public void resetField(String fieldId) {
        switch (fieldId) {
            case "backgroundOpacity" -> this.backgroundOpacity = DEFAULTS.backgroundOpacity;
            case "hideNumbers" -> this.hideNumbers = DEFAULTS.hideNumbers;
            case "centerText" -> this.centerText = DEFAULTS.centerText;
            case "useTeamColors" -> this.useTeamColors = DEFAULTS.useTeamColors;
            case "anchor" -> this.anchor = DEFAULTS.anchor;
            case "sideMargin" -> this.sideMargin = DEFAULTS.sideMargin;
            case "titleColor" -> this.titleColor = DEFAULTS.titleColor;
            case "textColor" -> this.textColor = DEFAULTS.textColor;
            case "scoreColor" -> this.scoreColor = DEFAULTS.scoreColor;
            default -> {
            }
        }
    }

    public int backgroundOpacity() {
        return this.backgroundOpacity;
    }

    public boolean hideNumbers() {
        return this.hideNumbers;
    }

    public boolean centerText() {
        return this.centerText;
    }

    public boolean useTeamColors() {
        return this.useTeamColors;
    }

    public ScoreboardAnchor anchor() {
        return this.anchor;
    }

    public int sideMargin() {
        return this.sideMargin;
    }

    public ScoreboardColor titleColor() {
        return this.titleColor;
    }

    public ScoreboardColor textColor() {
        return this.textColor;
    }

    public ScoreboardColor scoreColor() {
        return this.scoreColor;
    }

    public void setBackgroundOpacity(int backgroundOpacity) {
        this.backgroundOpacity = clamp(backgroundOpacity, 0, 100);
    }

    public void setHideNumbers(boolean hideNumbers) {
        this.hideNumbers = hideNumbers;
    }

    public void setCenterText(boolean centerText) {
        this.centerText = centerText;
    }

    public void setUseTeamColors(boolean useTeamColors) {
        this.useTeamColors = useTeamColors;
    }

    public void setAnchor(ScoreboardAnchor anchor) {
        this.anchor = anchor;
    }

    public void setSideMargin(int sideMargin) {
        this.sideMargin = clamp(sideMargin, 0, 40);
    }

    public void setTitleColor(ScoreboardColor titleColor) {
        this.titleColor = titleColor;
    }

    public void setTextColor(ScoreboardColor textColor) {
        this.textColor = textColor;
    }

    public void setScoreColor(ScoreboardColor scoreColor) {
        this.scoreColor = scoreColor;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ScoreboardSettings copy(ScoreboardSettings source) {
        ScoreboardSettings copy = new ScoreboardSettings();
        copy.backgroundOpacity = source.backgroundOpacity;
        copy.hideNumbers = source.hideNumbers;
        copy.centerText = source.centerText;
        copy.useTeamColors = source.useTeamColors;
        copy.anchor = source.anchor;
        copy.sideMargin = source.sideMargin;
        copy.titleColor = source.titleColor;
        copy.textColor = source.textColor;
        copy.scoreColor = source.scoreColor;
        return copy;
    }

    private static ScoreboardColor parseColor(String value, ScoreboardColor fallback) {
        try {
            return ScoreboardColor.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static ScoreboardAnchor parseAnchor(String value, ScoreboardAnchor fallback) {
        try {
            return ScoreboardAnchor.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
