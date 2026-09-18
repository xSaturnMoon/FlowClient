package com.flowclient.mods.media;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MediaHudRenderer {
    private static final int DISC_SIZE = 26;
    private static final int TEXT_GAP = 8;
    private static final int LINE_GAP = 2;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR = 0xFFB8BEC8;

    private static final MediaHudAnimator ANIMATOR = new MediaHudAnimator();
    private static final MediaHudAfkAnimator AFK_ANIMATOR = new MediaHudAfkAnimator();

    private MediaHudRenderer() {
    }

    static int baseDiscSize() {
        return DISC_SIZE;
    }

    public static int getWidth(Font font) {
        return computeLayout(font, MediaHudSettings.get(), previewState()).width();
    }

    public static int getHeight(Font font) {
        return computeLayout(font, MediaHudSettings.get(), previewState()).height();
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && !MediaHudMod.isEnabled()) {
            return;
        }

        if (!preview) {
            AFK_ANIMATOR.tick();
        }

        MediaHudSettings settings = MediaHudSettings.get();
        MediaSessionState state = preview ? previewState() : MediaSessionPoller.get().state();
        MediaProbeStatus probeStatus = preview ? MediaProbeStatus.ACTIVE : MediaSessionPoller.get().probeStatus();
        float afkBlend = preview ? 0.0F : AFK_ANIMATOR.blend();
        boolean afkAnimating = !preview && AFK_ANIMATOR.isAnimating();

        if (!preview && afkAnimating && !state.active()) {
            state = afkPlaceholderState();
        }

        if (!preview && settings.hideWhenIdle() && !state.active() && !afkAnimating) {
            if (probeStatus == MediaProbeStatus.MISSING) {
                state = errorState("Restart Flow launcher", "Media service missing");
            } else if (probeStatus == MediaProbeStatus.NO_SESSION) {
                return;
            } else {
                return;
            }
        }

        if (!preview && state.active() && state.title().isBlank() && !afkAnimating) {
            return;
        }

        if (!preview) {
            MediaAlbumArtTexture.get().update(mc, state);
            ANIMATOR.tick(state, AFK_ANIMATOR.spinMultiplier());
        }

        if (!preview
                && ANIMATOR.phase() == MediaHudAnimator.Phase.HIDDEN
                && probeStatus != MediaProbeStatus.MISSING
                && !afkAnimating) {
            return;
        }

        final MediaSessionState renderState = state;
        Layout layout = computeLayout(font, settings, renderState);
        int x = HudLayoutManager.get().resolveX(HudElement.MEDIA_HUD, layout.width(), layout.height());
        int y = HudLayoutManager.get().resolveY(HudElement.MEDIA_HUD, layout.width(), layout.height());
        float scale = HudLayoutManager.get().resolveScale(HudElement.MEDIA_HUD);

        if (afkBlend > 0.001F && !preview) {
            drawAfkOverlay(mc, graphics, x, y, scale, settings, afkBlend, preview);
        }

        if (afkBlend < 0.999F || preview) {
            HudRenderHelper.withLayout(graphics, x, y, scale, () ->
                    drawHudPanel(graphics, font, settings, layout, x, y, preview, afkBlend)
            );
        }
    }

    private static void drawHudPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            MediaHudSettings settings,
            Layout layout,
            int x,
            int y,
            boolean preview,
            float afkBlend
    ) {
        float contentAlpha = 1.0F - afkBlend;
        if (contentAlpha <= 0.02F) {
            return;
        }

        if (settings.showBackground()) {
            int alpha = (int) (settings.backgroundOpacity() / 100.0f * 255.0f * contentAlpha) << 24;
            graphics.fill(x, y, x + layout.width(), y + layout.height(), alpha | 0x08080C);
        }

        int discX = x + settings.paddingX();
        int discY = y + settings.paddingY();
        int textBaseX = discX + DISC_SIZE + TEXT_GAP;
        int slideDistance = DISC_SIZE + TEXT_GAP;
        float textSlide = preview ? 0.0F : ANIMATOR.textOffset();
        int textX = textBaseX + Math.round(textSlide * slideDistance);
        int textY = discY + (DISC_SIZE - layout.textBlockHeight()) / 2;

        drawTextBlock(graphics, font, settings, layout, textX, textY, contentAlpha);

        if (afkBlend < 0.02F) {
            MediaDiscRenderer.draw(
                    graphics,
                    discX,
                    discY,
                    DISC_SIZE,
                    preview ? 18.0F : ANIMATOR.discRotation(),
                    MediaAlbumArtTexture.get()
            );
        }
    }

    private static void drawAfkOverlay(
            Minecraft mc,
            GuiGraphicsExtractor graphics,
            int anchorX,
            int anchorY,
            float scale,
            MediaHudSettings settings,
            float afkBlend,
            boolean preview
    ) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenCenterX = screenWidth / 2;
        int screenCenterY = screenHeight / 2;

        int localDiscCenterX = anchorX + settings.paddingX() + DISC_SIZE / 2;
        int localDiscCenterY = anchorY + settings.paddingY() + DISC_SIZE / 2;
        int startCenterX = mapHudToScreenX(localDiscCenterX, anchorX, scale);
        int startCenterY = mapHudToScreenY(localDiscCenterY, anchorY, scale);

        int targetDiscSize = preview ? DISC_SIZE : AFK_ANIMATOR.targetDiscSize(mc);
        int discCenterX = Math.round(lerp(startCenterX, screenCenterX, afkBlend));
        int discCenterY = Math.round(lerp(startCenterY, screenCenterY, afkBlend));
        int discSize = Math.round(lerp(DISC_SIZE * scale, targetDiscSize, afkBlend));
        int discDrawX = discCenterX - discSize / 2;
        int discDrawY = discCenterY - discSize / 2;

        int dimAlpha = (int) (afkBlend * 140.0F) << 24;
        graphics.fill(0, 0, screenWidth, screenHeight, dimAlpha | 0x050508);

        MediaDiscRenderer.draw(
                graphics,
                discDrawX,
                discDrawY,
                discSize,
                preview ? 18.0F : ANIMATOR.discRotation(),
                MediaAlbumArtTexture.get()
        );
    }

    private static int mapHudToScreenX(int localX, int anchorX, float scale) {
        return Math.round(localX * scale + anchorX * (1.0F - scale));
    }

    private static int mapHudToScreenY(int localY, int anchorY, float scale) {
        return Math.round(localY * scale + anchorY * (1.0F - scale));
    }

    private static void drawTextBlock(
            GuiGraphicsExtractor graphics,
            Font font,
            MediaHudSettings settings,
            Layout layout,
            int textX,
            int textY,
            float alpha
    ) {
        int titleColor = withAlpha(TITLE_COLOR, alpha);
        int subtitleColor = withAlpha(SUBTITLE_COLOR, alpha);
        graphics.text(font, layout.title(), textX, textY, titleColor, settings.textShadow());
        if (layout.showArtist()) {
            graphics.text(font, layout.artist(), textX, textY + font.lineHeight + LINE_GAP, subtitleColor, settings.textShadow());
        }
    }

    private static Layout computeLayout(Font font, MediaHudSettings settings, MediaSessionState state) {
        String title = truncate(font, nonEmpty(state.title(), "Nothing playing"), settings.maxTextWidth());
        String artist = truncate(font, nonEmpty(state.artist(), "—"), settings.maxTextWidth());
        boolean showArtist = settings.showArtist();
        int textBlockHeight = showArtist ? font.lineHeight * 2 + LINE_GAP : font.lineHeight;

        int textWidth = Math.max(font.width(title), showArtist ? font.width(artist) : 0);
        textWidth = Math.min(textWidth, settings.maxTextWidth());
        int contentWidth = DISC_SIZE + TEXT_GAP + textWidth;
        int width = contentWidth + settings.paddingX() * 2;
        int height = settings.paddingY() * 2 + DISC_SIZE;

        return new Layout(width, height, title, artist, showArtist, textBlockHeight);
    }

    private static MediaSessionState afkPlaceholderState() {
        return new MediaSessionState(
                true,
                "—",
                "",
                "",
                "afk",
                "",
                0.0,
                0.0,
                false,
                "",
                System.currentTimeMillis()
        );
    }

    private static MediaSessionState previewState() {
        return new MediaSessionState(
                true,
                "Flow Session",
                "Your Artist",
                "Album",
                "preview",
                "",
                92.0,
                210.0,
                true,
                "Spotify.exe",
                System.currentTimeMillis()
        );
    }

    private static MediaSessionState errorState(String title, String artist) {
        return new MediaSessionState(
                true,
                title,
                artist,
                "",
                "error",
                "",
                0.0,
                0.0,
                false,
                "",
                System.currentTimeMillis()
        );
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "…";
        int target = maxWidth - font.width(ellipsis);
        if (target <= 0) {
            return ellipsis;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(text.charAt(i));
            if (font.width(builder.toString()) > target) {
                builder.deleteCharAt(builder.length() - 1);
                break;
            }
        }
        return builder + ellipsis;
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static int withAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }

    private record Layout(
            int width,
            int height,
            String title,
            String artist,
            boolean showArtist,
            int textBlockHeight
    ) {
    }
}
