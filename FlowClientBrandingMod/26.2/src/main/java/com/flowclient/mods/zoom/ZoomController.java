package com.flowclient.mods.zoom;

import com.flowclient.mods.ZoomifyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class ZoomController {
  private static final ZoomAnimator ZOOM_ANIMATOR = new ZoomAnimator();
  private static final ZoomAnimator SCROLL_ANIMATOR = new ZoomAnimator();

  private static boolean zoomKeyHeld;
  private static int scrollSteps;
  private static long lastPrepareNs;

  private ZoomController() {}

  public static void setZoomKeyHeld(boolean held) {
    zoomKeyHeld = held;
  }

  public static void handleScroll(double vertical) {
    if (!ZoomifyMod.isEnabled() || !zoomKeyHeld) {
      return;
    }

    ZoomifySettings settings = ZoomifySettings.get();
    if (!settings.enableScrollZoom() || vertical == 0.0) {
      return;
    }

    int direction = vertical > 0.0 ? 1 : -1;
    scrollSteps = Math.max(0, Math.min(settings.scrollStepCount(), scrollSteps + direction));
    updateScrollTarget(settings);
  }

  public static void updateTargets() {
    Minecraft client = Minecraft.getInstance();
    if (!ZoomifyMod.isEnabled() || client.player == null || client.level == null) {
      reset();
      return;
    }

    ZoomifySettings settings = ZoomifySettings.get();
    boolean wantsZoom = zoomKeyHeld && shouldApplyZoom(client, settings);
    if (wantsZoom) {
      ZOOM_ANIMATOR.setTarget(1.0f, (float) settings.zoomInTime(), settings.zoomInTransition());
    } else {
      ZOOM_ANIMATOR.setTarget(0.0f, (float) settings.zoomOutTime(), settings.zoomOutTransition());
      if (!settings.rememberZoomSteps()) {
        scrollSteps = 0;
        updateScrollTarget(settings);
      }
    }
  }

  public static void prepareFov() {
    if (!ZoomifyMod.isEnabled()) {
      return;
    }

    long now = System.nanoTime();
    if (lastPrepareNs != 0L) {
      long elapsedNs = now - lastPrepareNs;
      if (elapsedNs < 500_000L) {
        return;
      }

      float deltaSeconds = elapsedNs / 1_000_000_000f;
      if (deltaSeconds < 0.25f) {
        ZOOM_ANIMATOR.tick(deltaSeconds);
        SCROLL_ANIMATOR.tick(deltaSeconds);
      }
    }
    lastPrepareNs = now;
  }

  public static double modifyFov(double baseFov, boolean changingFov) {
    return modifyFov((float) baseFov, changingFov);
  }

  public static float modifyFov(float baseFov, boolean changingFov) {
    if (!ZoomifyMod.isEnabled()) {
      return baseFov;
    }

    ZoomifySettings settings = ZoomifySettings.get();
    if (!settings.affectHandFov() && !changingFov) {
      return baseFov;
    }

    double multiplier = currentZoomMultiplier(settings);
    if (multiplier <= 1.0001) {
      return baseFov;
    }

    return (float) (baseFov / multiplier);
  }

  public static void reset() {
    zoomKeyHeld = false;
    scrollSteps = 0;
    lastPrepareNs = 0L;
    ZOOM_ANIMATOR.snap(0.0f);
    SCROLL_ANIMATOR.snap(0.0f);
  }

  private static double currentZoomMultiplier(ZoomifySettings settings) {
    double baseZoom = 1.0 + (settings.initialZoom() - 1.0) * ZOOM_ANIMATOR.value();
    if (!settings.enableScrollZoom() || SCROLL_ANIMATOR.value() <= 0.001f) {
      return baseZoom;
    }

    return baseZoom * Math.pow(settings.zoomPerStep(), SCROLL_ANIMATOR.value());
  }

  private static void updateScrollTarget(ZoomifySettings settings) {
    float smoothness = (float) settings.scrollZoomSmoothness();
    float duration = Math.max(0.05f, 0.35f * (1.0f - smoothness) + 0.05f);
    SCROLL_ANIMATOR.setTarget(scrollSteps, duration, ZoomTransition.LINEAR);
  }

  private static boolean shouldApplyZoom(Minecraft client, ZoomifySettings settings) {
    if (client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
      return settings.spyglassZoomBehavior() != SpyglassZoomBehavior.IGNORE;
    }

    return true;
  }
}
