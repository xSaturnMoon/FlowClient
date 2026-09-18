package com.flowclient.mods.zoom;

import com.flowclient.mods.FlowModConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public final class ZoomifySettings {
  private static final ZoomifySettings DEFAULTS = new ZoomifySettings();

  private double initialZoom = 4.0;
  private double zoomInTime = 1.0;
  private double zoomOutTime = 0.5;
  private ZoomTransition zoomInTransition = ZoomTransition.EASE_OUT_EXPONENTIAL;
  private ZoomTransition zoomOutTransition = ZoomTransition.EASE_OUT_EXPONENTIAL;
  private boolean affectHandFov = false;

  private boolean enableScrollZoom = false;
  private int scrollStepCount = 10;
  private double zoomPerStep = 1.5;
  private double scrollZoomSmoothness = 0.70;
  private boolean rememberZoomSteps = false;

  private SpyglassZoomBehavior spyglassZoomBehavior = SpyglassZoomBehavior.COMBINE;
  private OverlayVisibility overlayVisibility = OverlayVisibility.HOLDING;
  private SoundBehavior soundBehavior = SoundBehavior.MATCH_OVERLAY;

  private static ZoomifySettings current = copy(DEFAULTS);

  private ZoomifySettings() {}

  public static ZoomifySettings get() {
    return current;
  }

  public static void load(JsonObject root) {
    if (root == null || !root.has("zoomifySettings")) {
      reset(false);
      return;
    }

    JsonObject json = GsonHelper.getAsJsonObject(root, "zoomifySettings");
    ZoomifySettings settings = copy(DEFAULTS);
    settings.initialZoom = GsonHelper.getAsDouble(json, "initialZoom", DEFAULTS.initialZoom);
    settings.zoomInTime = GsonHelper.getAsDouble(json, "zoomInTime", DEFAULTS.zoomInTime);
    settings.zoomOutTime = GsonHelper.getAsDouble(json, "zoomOutTime", DEFAULTS.zoomOutTime);
    settings.zoomInTransition = parseTransition(
        GsonHelper.getAsString(json, "zoomInTransition", DEFAULTS.zoomInTransition.name()),
        DEFAULTS.zoomInTransition);
    settings.zoomOutTransition = parseTransition(
        GsonHelper.getAsString(json, "zoomOutTransition", DEFAULTS.zoomOutTransition.name()),
        DEFAULTS.zoomOutTransition);
    settings.affectHandFov = GsonHelper.getAsBoolean(json, "affectHandFov", DEFAULTS.affectHandFov);
    settings.enableScrollZoom = GsonHelper.getAsBoolean(json, "enableScrollZoom", DEFAULTS.enableScrollZoom);
    settings.scrollStepCount = GsonHelper.getAsInt(json, "scrollStepCount", DEFAULTS.scrollStepCount);
    settings.zoomPerStep = GsonHelper.getAsDouble(json, "zoomPerStep", DEFAULTS.zoomPerStep);
    settings.scrollZoomSmoothness =
        GsonHelper.getAsDouble(json, "scrollZoomSmoothness", DEFAULTS.scrollZoomSmoothness);
    settings.rememberZoomSteps =
        GsonHelper.getAsBoolean(json, "rememberZoomSteps", DEFAULTS.rememberZoomSteps);
    settings.spyglassZoomBehavior = parseEnum(
        GsonHelper.getAsString(json, "spyglassZoomBehavior", DEFAULTS.spyglassZoomBehavior.name()),
        SpyglassZoomBehavior.class,
        DEFAULTS.spyglassZoomBehavior);
    settings.overlayVisibility = parseEnum(
        GsonHelper.getAsString(json, "overlayVisibility", DEFAULTS.overlayVisibility.name()),
        OverlayVisibility.class,
        DEFAULTS.overlayVisibility);
    settings.soundBehavior = parseEnum(
        GsonHelper.getAsString(json, "soundBehavior", DEFAULTS.soundBehavior.name()),
        SoundBehavior.class,
        DEFAULTS.soundBehavior);
    current = settings;
  }

  public static void save(JsonObject root) {
    JsonObject json = new JsonObject();
    json.addProperty("initialZoom", current.initialZoom);
    json.addProperty("zoomInTime", current.zoomInTime);
    json.addProperty("zoomOutTime", current.zoomOutTime);
    json.addProperty("zoomInTransition", current.zoomInTransition.name());
    json.addProperty("zoomOutTransition", current.zoomOutTransition.name());
    json.addProperty("affectHandFov", current.affectHandFov);
    json.addProperty("enableScrollZoom", current.enableScrollZoom);
    json.addProperty("scrollStepCount", current.scrollStepCount);
    json.addProperty("zoomPerStep", current.zoomPerStep);
    json.addProperty("scrollZoomSmoothness", current.scrollZoomSmoothness);
    json.addProperty("rememberZoomSteps", current.rememberZoomSteps);
    json.addProperty("spyglassZoomBehavior", current.spyglassZoomBehavior.name());
    json.addProperty("overlayVisibility", current.overlayVisibility.name());
    json.addProperty("soundBehavior", current.soundBehavior.name());
    root.add("zoomifySettings", json);
  }

  public static void reset(boolean persist) {
    current = copy(DEFAULTS);
    if (persist) {
      FlowModConfig.save();
    }
  }

  public void resetField(String fieldId) {
    switch (fieldId) {
      case "initialZoom" -> this.initialZoom = DEFAULTS.initialZoom;
      case "zoomInTime" -> this.zoomInTime = DEFAULTS.zoomInTime;
      case "zoomOutTime" -> this.zoomOutTime = DEFAULTS.zoomOutTime;
      case "zoomInTransition" -> this.zoomInTransition = DEFAULTS.zoomInTransition;
      case "zoomOutTransition" -> this.zoomOutTransition = DEFAULTS.zoomOutTransition;
      case "affectHandFov" -> this.affectHandFov = DEFAULTS.affectHandFov;
      case "enableScrollZoom" -> this.enableScrollZoom = DEFAULTS.enableScrollZoom;
      case "scrollStepCount" -> this.scrollStepCount = DEFAULTS.scrollStepCount;
      case "zoomPerStep" -> this.zoomPerStep = DEFAULTS.zoomPerStep;
      case "scrollZoomSmoothness" -> this.scrollZoomSmoothness = DEFAULTS.scrollZoomSmoothness;
      case "rememberZoomSteps" -> this.rememberZoomSteps = DEFAULTS.rememberZoomSteps;
      case "spyglassZoomBehavior" -> this.spyglassZoomBehavior = DEFAULTS.spyglassZoomBehavior;
      case "overlayVisibility" -> this.overlayVisibility = DEFAULTS.overlayVisibility;
      case "soundBehavior" -> this.soundBehavior = DEFAULTS.soundBehavior;
      default -> {
        return;
      }
    }
    FlowModConfig.save();
  }

  public void persist() {
    FlowModConfig.save();
  }

  public double initialZoom() {
    return this.initialZoom;
  }

  public void setInitialZoom(double value) {
    this.initialZoom = clamp(value, 1.1, 50.0);
  }

  public double zoomInTime() {
    return this.zoomInTime;
  }

  public void setZoomInTime(double value) {
    this.zoomInTime = clamp(value, 0.0, 5.0);
  }

  public double zoomOutTime() {
    return this.zoomOutTime;
  }

  public void setZoomOutTime(double value) {
    this.zoomOutTime = clamp(value, 0.0, 5.0);
  }

  public ZoomTransition zoomInTransition() {
    return this.zoomInTransition;
  }

  public void setZoomInTransition(ZoomTransition value) {
    this.zoomInTransition = value;
  }

  public ZoomTransition zoomOutTransition() {
    return this.zoomOutTransition;
  }

  public void setZoomOutTransition(ZoomTransition value) {
    this.zoomOutTransition = value;
  }

  public boolean affectHandFov() {
    return this.affectHandFov;
  }

  public void setAffectHandFov(boolean value) {
    this.affectHandFov = value;
  }

  public boolean enableScrollZoom() {
    return this.enableScrollZoom;
  }

  public void setEnableScrollZoom(boolean value) {
    this.enableScrollZoom = value;
  }

  public int scrollStepCount() {
    return this.scrollStepCount;
  }

  public void setScrollStepCount(int value) {
    this.scrollStepCount = Math.max(1, Math.min(50, value));
  }

  public double zoomPerStep() {
    return this.zoomPerStep;
  }

  public void setZoomPerStep(double value) {
    this.zoomPerStep = clamp(value, 1.01, 5.0);
  }

  public double scrollZoomSmoothness() {
    return this.scrollZoomSmoothness;
  }

  public void setScrollZoomSmoothness(double value) {
    this.scrollZoomSmoothness = clamp(value, 0.0, 1.0);
  }

  public boolean rememberZoomSteps() {
    return this.rememberZoomSteps;
  }

  public void setRememberZoomSteps(boolean value) {
    this.rememberZoomSteps = value;
  }

  public SpyglassZoomBehavior spyglassZoomBehavior() {
    return this.spyglassZoomBehavior;
  }

  public void setSpyglassZoomBehavior(SpyglassZoomBehavior value) {
    this.spyglassZoomBehavior = value;
  }

  public OverlayVisibility overlayVisibility() {
    return this.overlayVisibility;
  }

  public void setOverlayVisibility(OverlayVisibility value) {
    this.overlayVisibility = value;
  }

  public SoundBehavior soundBehavior() {
    return this.soundBehavior;
  }

  public void setSoundBehavior(SoundBehavior value) {
    this.soundBehavior = value;
  }

  private static ZoomifySettings copy(ZoomifySettings source) {
    ZoomifySettings copy = new ZoomifySettings();
    copy.initialZoom = source.initialZoom;
    copy.zoomInTime = source.zoomInTime;
    copy.zoomOutTime = source.zoomOutTime;
    copy.zoomInTransition = source.zoomInTransition;
    copy.zoomOutTransition = source.zoomOutTransition;
    copy.affectHandFov = source.affectHandFov;
    copy.enableScrollZoom = source.enableScrollZoom;
    copy.scrollStepCount = source.scrollStepCount;
    copy.zoomPerStep = source.zoomPerStep;
    copy.scrollZoomSmoothness = source.scrollZoomSmoothness;
    copy.rememberZoomSteps = source.rememberZoomSteps;
    copy.spyglassZoomBehavior = source.spyglassZoomBehavior;
    copy.overlayVisibility = source.overlayVisibility;
    copy.soundBehavior = source.soundBehavior;
    return copy;
  }

  private static ZoomTransition parseTransition(String name, ZoomTransition fallback) {
    try {
      return ZoomTransition.valueOf(name);
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private static <T extends Enum<T>> T parseEnum(String name, Class<T> type, T fallback) {
    try {
      return Enum.valueOf(type, name);
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
