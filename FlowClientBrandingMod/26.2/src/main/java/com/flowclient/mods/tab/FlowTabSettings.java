package com.flowclient.mods.tab;

import com.flowclient.mods.FlowModConfig;
import com.flowclient.mods.render.ScoreboardColor;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class FlowTabSettings {
    private static final FlowTabSettings DEFAULTS = new FlowTabSettings();

    private boolean showHeads = true;
    private ScoreboardColor pingColorGood = ScoreboardColor.GREEN;
    private ScoreboardColor pingColorMedium = ScoreboardColor.GOLD;
    private ScoreboardColor pingColorBad = ScoreboardColor.RED;
    private FlowTabSort sort = FlowTabSort.VANILLA;
    private FlowTabPosition position = FlowTabPosition.TOP_CENTER;
    private FlowTabNameAlign nameAlign = FlowTabNameAlign.LEFT;
    private int rowSpacing = 0;
    private int columnGap = 5;
    private boolean cinematicMode = false;
    private int cinematicDimOpacity = 45;
    private boolean cinematicBlur = true;
    private int cinematicBlurStrength = 5;
    private int cinematicZoomPercent = 6;
    private int cinematicTransitionMs = 220;
    private boolean cinematicHideHud = true;
    private boolean cinematicVignette = false;

    private static FlowTabSettings current = copy(DEFAULTS);

    private FlowTabSettings() {
    }

    public static FlowTabSettings get() {
        return current;
    }

    public static void load(JsonObject root) {
        if (root == null || !root.has("flowTabSettings")) {
            reset(false);
            return;
        }

        JsonObject json = GsonHelper.getAsJsonObject(root, "flowTabSettings");
        FlowTabSettings settings = copy(DEFAULTS);
        settings.showHeads = GsonHelper.getAsBoolean(json, "showHeads", DEFAULTS.showHeads);
        settings.pingColorGood = parseColor(GsonHelper.getAsString(json, "pingColorGood", DEFAULTS.pingColorGood.name()), DEFAULTS.pingColorGood);
        settings.pingColorMedium = parseColor(GsonHelper.getAsString(json, "pingColorMedium", DEFAULTS.pingColorMedium.name()), DEFAULTS.pingColorMedium);
        settings.pingColorBad = parseColor(GsonHelper.getAsString(json, "pingColorBad", DEFAULTS.pingColorBad.name()), DEFAULTS.pingColorBad);
        settings.sort = parseSort(GsonHelper.getAsString(json, "sort", DEFAULTS.sort.name()), DEFAULTS.sort);
        if ("TAB_ORDER".equals(GsonHelper.getAsString(json, "sort", ""))) {
            settings.sort = FlowTabSort.VANILLA;
        }
        settings.position = parsePosition(GsonHelper.getAsString(json, "position", DEFAULTS.position.name()), DEFAULTS.position);
        settings.nameAlign = parseNameAlign(GsonHelper.getAsString(json, "nameAlign", DEFAULTS.nameAlign.name()), DEFAULTS.nameAlign);
        settings.rowSpacing = clamp(GsonHelper.getAsInt(json, "rowSpacing", DEFAULTS.rowSpacing), 0, 8);
        settings.columnGap = clamp(GsonHelper.getAsInt(json, "columnGap", DEFAULTS.columnGap), 0, 15);
        settings.cinematicMode = GsonHelper.getAsBoolean(json, "cinematicMode", DEFAULTS.cinematicMode);
        settings.cinematicDimOpacity = clamp(GsonHelper.getAsInt(json, "cinematicDimOpacity", DEFAULTS.cinematicDimOpacity), 0, 90);
        settings.cinematicBlur = GsonHelper.getAsBoolean(json, "cinematicBlur", DEFAULTS.cinematicBlur);
        settings.cinematicBlurStrength = clamp(GsonHelper.getAsInt(json, "cinematicBlurStrength", DEFAULTS.cinematicBlurStrength), 0, 10);
        settings.cinematicZoomPercent = clamp(GsonHelper.getAsInt(json, "cinematicZoomPercent", DEFAULTS.cinematicZoomPercent), 0, 20);
        settings.cinematicTransitionMs = clamp(GsonHelper.getAsInt(json, "cinematicTransitionMs", DEFAULTS.cinematicTransitionMs), 80, 600);
        settings.cinematicHideHud = GsonHelper.getAsBoolean(json, "cinematicHideHud", DEFAULTS.cinematicHideHud);
        settings.cinematicVignette = GsonHelper.getAsBoolean(json, "cinematicVignette", DEFAULTS.cinematicVignette);
        current = settings;
    }

    public static void save(JsonObject root) {
        JsonObject json = new JsonObject();
        json.addProperty("showHeads", current.showHeads);
        json.addProperty("pingColorGood", current.pingColorGood.name());
        json.addProperty("pingColorMedium", current.pingColorMedium.name());
        json.addProperty("pingColorBad", current.pingColorBad.name());
        json.addProperty("sort", current.sort.name());
        json.addProperty("position", current.position.name());
        json.addProperty("nameAlign", current.nameAlign.name());
        json.addProperty("rowSpacing", current.rowSpacing);
        json.addProperty("columnGap", current.columnGap);
        json.addProperty("cinematicMode", current.cinematicMode);
        json.addProperty("cinematicDimOpacity", current.cinematicDimOpacity);
        json.addProperty("cinematicBlur", current.cinematicBlur);
        json.addProperty("cinematicBlurStrength", current.cinematicBlurStrength);
        json.addProperty("cinematicZoomPercent", current.cinematicZoomPercent);
        json.addProperty("cinematicTransitionMs", current.cinematicTransitionMs);
        json.addProperty("cinematicHideHud", current.cinematicHideHud);
        json.addProperty("cinematicVignette", current.cinematicVignette);
        root.add("flowTabSettings", json);
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
            case "showHeads" -> this.showHeads = DEFAULTS.showHeads;
            case "pingColorGood" -> this.pingColorGood = DEFAULTS.pingColorGood;
            case "pingColorMedium" -> this.pingColorMedium = DEFAULTS.pingColorMedium;
            case "pingColorBad" -> this.pingColorBad = DEFAULTS.pingColorBad;
            case "sort" -> this.sort = DEFAULTS.sort;
            case "position" -> this.position = DEFAULTS.position;
            case "nameAlign" -> this.nameAlign = DEFAULTS.nameAlign;
            case "rowSpacing" -> this.rowSpacing = DEFAULTS.rowSpacing;
            case "columnGap" -> this.columnGap = DEFAULTS.columnGap;
            case "cinematicMode" -> this.cinematicMode = DEFAULTS.cinematicMode;
            case "cinematicDimOpacity" -> this.cinematicDimOpacity = DEFAULTS.cinematicDimOpacity;
            case "cinematicBlur" -> this.cinematicBlur = DEFAULTS.cinematicBlur;
            case "cinematicBlurStrength" -> this.cinematicBlurStrength = DEFAULTS.cinematicBlurStrength;
            case "cinematicZoomPercent" -> this.cinematicZoomPercent = DEFAULTS.cinematicZoomPercent;
            case "cinematicTransitionMs" -> this.cinematicTransitionMs = DEFAULTS.cinematicTransitionMs;
            case "cinematicHideHud" -> this.cinematicHideHud = DEFAULTS.cinematicHideHud;
            case "cinematicVignette" -> this.cinematicVignette = DEFAULTS.cinematicVignette;
            default -> {
            }
        }
    }

    public boolean showHeads() {
        return showHeads;
    }

    public ScoreboardColor pingColorGood() {
        return pingColorGood;
    }

    public ScoreboardColor pingColorMedium() {
        return pingColorMedium;
    }

    public ScoreboardColor pingColorBad() {
        return pingColorBad;
    }

    public FlowTabSort sort() {
        return sort;
    }

    public FlowTabPosition position() {
        return position;
    }

    public FlowTabNameAlign nameAlign() {
        return nameAlign;
    }

    public int rowSpacing() {
        return rowSpacing;
    }

    public int columnGap() {
        return columnGap;
    }

    public boolean cinematicMode() {
        return cinematicMode;
    }

    public int cinematicDimOpacity() {
        return cinematicDimOpacity;
    }

    public boolean cinematicBlur() {
        return cinematicBlur;
    }

    public int cinematicBlurStrength() {
        return cinematicBlurStrength;
    }

    public int cinematicZoomPercent() {
        return cinematicZoomPercent;
    }

    public int cinematicTransitionMs() {
        return cinematicTransitionMs;
    }

    public boolean cinematicHideHud() {
        return cinematicHideHud;
    }

    public boolean cinematicVignette() {
        return cinematicVignette;
    }

    public void setShowHeads(boolean showHeads) {
        this.showHeads = showHeads;
    }

    public void setPingColorGood(ScoreboardColor pingColorGood) {
        this.pingColorGood = pingColorGood;
    }

    public void setPingColorMedium(ScoreboardColor pingColorMedium) {
        this.pingColorMedium = pingColorMedium;
    }

    public void setPingColorBad(ScoreboardColor pingColorBad) {
        this.pingColorBad = pingColorBad;
    }

    public void setSort(FlowTabSort sort) {
        this.sort = sort;
    }

    public void setPosition(FlowTabPosition position) {
        this.position = position;
    }

    public void setNameAlign(FlowTabNameAlign nameAlign) {
        this.nameAlign = nameAlign;
    }

    public void setRowSpacing(int rowSpacing) {
        this.rowSpacing = clamp(rowSpacing, 0, 8);
    }

    public void setColumnGap(int columnGap) {
        this.columnGap = clamp(columnGap, 0, 15);
    }

    public void setCinematicMode(boolean cinematicMode) {
        this.cinematicMode = cinematicMode;
    }

    public void setCinematicDimOpacity(int cinematicDimOpacity) {
        this.cinematicDimOpacity = clamp(cinematicDimOpacity, 0, 90);
    }

    public void setCinematicBlur(boolean cinematicBlur) {
        this.cinematicBlur = cinematicBlur;
    }

    public void setCinematicBlurStrength(int cinematicBlurStrength) {
        this.cinematicBlurStrength = clamp(cinematicBlurStrength, 0, 10);
    }

    public void setCinematicZoomPercent(int cinematicZoomPercent) {
        this.cinematicZoomPercent = clamp(cinematicZoomPercent, 0, 20);
    }

    public void setCinematicTransitionMs(int cinematicTransitionMs) {
        this.cinematicTransitionMs = clamp(cinematicTransitionMs, 80, 600);
    }

    public void setCinematicHideHud(boolean cinematicHideHud) {
        this.cinematicHideHud = cinematicHideHud;
    }

    public void setCinematicVignette(boolean cinematicVignette) {
        this.cinematicVignette = cinematicVignette;
    }

    private static FlowTabSettings copy(FlowTabSettings source) {
        FlowTabSettings copy = new FlowTabSettings();
        copy.showHeads = source.showHeads;
        copy.pingColorGood = source.pingColorGood;
        copy.pingColorMedium = source.pingColorMedium;
        copy.pingColorBad = source.pingColorBad;
        copy.sort = source.sort;
        copy.position = source.position;
        copy.nameAlign = source.nameAlign;
        copy.rowSpacing = source.rowSpacing;
        copy.columnGap = source.columnGap;
        copy.cinematicMode = source.cinematicMode;
        copy.cinematicDimOpacity = source.cinematicDimOpacity;
        copy.cinematicBlur = source.cinematicBlur;
        copy.cinematicBlurStrength = source.cinematicBlurStrength;
        copy.cinematicZoomPercent = source.cinematicZoomPercent;
        copy.cinematicTransitionMs = source.cinematicTransitionMs;
        copy.cinematicHideHud = source.cinematicHideHud;
        copy.cinematicVignette = source.cinematicVignette;
        return copy;
    }

    private static ScoreboardColor parseColor(String value, ScoreboardColor fallback) {
        try {
            return ScoreboardColor.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static FlowTabSort parseSort(String value, FlowTabSort fallback) {
        try {
            return FlowTabSort.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static FlowTabPosition parsePosition(String value, FlowTabPosition fallback) {
        try {
            return FlowTabPosition.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static FlowTabNameAlign parseNameAlign(String value, FlowTabNameAlign fallback) {
        try {
            return FlowTabNameAlign.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
