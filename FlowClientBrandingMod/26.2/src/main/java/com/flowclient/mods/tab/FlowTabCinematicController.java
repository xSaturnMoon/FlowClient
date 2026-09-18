package com.flowclient.mods.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FlowTabCinematicController {
    private static final TabCinematicAnimator STRENGTH = new TabCinematicAnimator();
    private static final TabCinematicAnimator HUD_HIDE = new TabCinematicAnimator();
    private static final float HUD_RELEASE_SPEED = 2.0f;

    private static int tabVisibleFrames;
    private static boolean tabRenderedThisFrame;
    private static long lastPrepareNs;

    private FlowTabCinematicController() {
    }

    public static void beginHudFrame() {
        tabRenderedThisFrame = false;
    }

    public static void onTabRendered() {
        tabVisibleFrames = 1;
        tabRenderedThisFrame = true;
    }

    public static boolean wasTabRenderedThisFrame() {
        return tabRenderedThisFrame;
    }

    public static void tick(Minecraft client) {
        if (!FlowTabMod.isEnabled()) {
            STRENGTH.snap(0.0f);
            HUD_HIDE.snap(0.0f);
            tabVisibleFrames = 0;
            return;
        }

        FlowTabSettings settings = FlowTabSettings.get();
        if (!settings.cinematicMode()) {
            STRENGTH.snap(0.0f);
            HUD_HIDE.snap(0.0f);
            tabVisibleFrames = 0;
            return;
        }

        boolean tabOpen = tabVisibleFrames > 0;
        if (tabVisibleFrames > 0) {
            tabVisibleFrames--;
        }

        updateTargets(settings, tabOpen);
    }

    public static void prepareStrength() {
        if (!FlowTabMod.isEnabled() || !FlowTabSettings.get().cinematicMode()) {
            return;
        }

        long now = System.nanoTime();
        if (lastPrepareNs != 0L) {
            long elapsedNs = now - lastPrepareNs;
            if (elapsedNs >= 500_000L) {
                float deltaSeconds = Math.min(0.1f, elapsedNs / 1_000_000_000f);
                STRENGTH.tick(deltaSeconds);
                HUD_HIDE.tick(deltaSeconds);
            }
        }
        lastPrepareNs = now;
    }

    public static float strength() {
        return STRENGTH.value();
    }

    public static boolean isActive() {
        return strength() > 0.001f;
    }

    public static boolean shouldHideHud() {
        FlowTabSettings settings = FlowTabSettings.get();
        return settings.cinematicMode() && settings.cinematicHideHud() && HUD_HIDE.value() > 0.001f;
    }

    public static float modifyFov(float baseFov) {
        FlowTabSettings settings = FlowTabSettings.get();
        if (!FlowTabMod.isEnabled() || !settings.cinematicMode() || settings.cinematicZoomPercent() <= 0) {
            return baseFov;
        }

        float zoom = settings.cinematicZoomPercent() / 100.0f;
        float multiplier = 1.0f + zoom * strength();
        return baseFov / multiplier;
    }

    public static void renderCoverOverlay(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        FlowTabSettings settings = FlowTabSettings.get();
        if (!FlowTabMod.isEnabled() || !settings.cinematicMode()) {
            return;
        }

        float strength = strength();
        if (strength <= 0.001f) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        if (settings.cinematicBlur()) {
            graphics.blurBeforeThisStratum();
        }

        int dimAlpha = (int) (settings.cinematicDimOpacity() * strength * 2.55f);
        if (dimAlpha > 0) {
            graphics.fill(0, 0, width, height, (dimAlpha << 24));
        }

        if (settings.cinematicVignette()) {
            renderSoftVignette(graphics, width, height, strength);
        }
    }

    private static void updateTargets(FlowTabSettings settings, boolean tabOpen) {
        float transitionSeconds = settings.cinematicTransitionMs() / 1000.0f;
        STRENGTH.setTarget(tabOpen ? 1.0f : 0.0f, transitionSeconds);

        float hudTransition = tabOpen
                ? transitionSeconds
                : Math.max(0.05f, transitionSeconds / HUD_RELEASE_SPEED);
        HUD_HIDE.setTarget(tabOpen ? 1.0f : 0.0f, hudTransition);
    }

    private static void renderSoftVignette(GuiGraphicsExtractor graphics, int width, int height, float strength) {
        int maxEdge = Math.min(width, height) / 2;
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float t = (i + 1) / (float) steps;
            int inset = (int) (maxEdge * 0.22f * t * strength);
            int alpha = (int) (18 * strength * t);
            if (alpha <= 0 || inset <= 0) {
                continue;
            }
            int color = alpha << 24;
            graphics.fill(inset, inset, width - inset, inset + 1, color);
            graphics.fill(inset, height - inset - 1, width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + 1, height - inset, color);
            graphics.fill(width - inset - 1, inset, width - inset, height - inset, color);
        }
    }
}
