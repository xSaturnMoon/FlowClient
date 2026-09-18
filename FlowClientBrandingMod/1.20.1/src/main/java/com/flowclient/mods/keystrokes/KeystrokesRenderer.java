package com.flowclient.mods.keystrokes;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class KeystrokesRenderer {
    private KeystrokesRenderer() {
    }

    public static int getWidth() {
        return getWidth(KeystrokesSettings.get());
    }

    public static int getHeight() {
        return getHeight(KeystrokesSettings.get());
    }

    public static int getWidth(KeystrokesSettings settings) {
        int pad = settings.panelPadding();
        return movementWidth(settings) + pad * 2;
    }

    public static int getHeight(KeystrokesSettings settings) {
        int pad = settings.panelPadding();
        return contentHeight(settings) + pad * 2;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && (!KeystrokesMod.isEnabled() || mc.player == null)) {
            return;
        }

        KeystrokesSettings settings = KeystrokesSettings.get();
        int width = getWidth(settings);
        int height = getHeight(settings);
        int x = HudLayoutManager.get().resolveX(HudElement.KEYSTROKES, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.KEYSTROKES, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.KEYSTROKES);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> drawWidget(mc, graphics, font, settings, x, y, preview));
    }

    private static void drawWidget(
            Minecraft mc,
            GuiGraphics graphics,
            Font font,
            KeystrokesSettings settings,
            int x,
            int y,
            boolean preview
    ) {
        int pad = settings.panelPadding();
        int contentW = movementWidth(settings);
        int contentH = contentHeight(settings);

        if (settings.showPanelBackground()) {
            KeystrokesDraw.fillRounded(
                    graphics,
                    x,
                    y,
                    contentW + pad * 2,
                    contentH + pad * 2,
                    settings.cornerRadius() + 2,
                    0x90080A10
            );
        }

        int cursorY = y + pad;
        int innerX = x + pad;
        var keys = mc.options;

        if (settings.showMovementKeys()) {
            int keyW = settings.keyWidth();
            int keyH = settings.keyHeight();
            int gap = settings.keyGap();

            drawKey(graphics, font, settings, "W", null, innerX + keyW + gap, cursorY, keyW, keyH, preview || keys.keyUp.isDown());
            cursorY += keyH + gap;

            drawKey(graphics, font, settings, "A", null, innerX, cursorY, keyW, keyH, preview || keys.keyLeft.isDown());
            drawKey(graphics, font, settings, "S", null, innerX + keyW + gap, cursorY, keyW, keyH, preview || keys.keyDown.isDown());
            drawKey(graphics, font, settings, "D", null, innerX + (keyW + gap) * 2, cursorY, keyW, keyH, preview || keys.keyRight.isDown());
            cursorY += keyH + gap;
        }

        if (settings.showMouseButtons()) {
            int keyH = settings.keyHeight();
            int gap = settings.keyGap();
            int btnW = (contentW - gap) / 2;
            int leftCps = preview ? 12 : CpsTracker.getLeftCps();
            int rightCps = preview ? 8 : CpsTracker.getRightCps();
            boolean lmb = !preview && keys.keyAttack.isDown();
            boolean rmb = !preview && keys.keyUse.isDown();

            String leftSub = cpsSubLabel(settings, leftCps);
            String rightSub = cpsSubLabel(settings, rightCps);
            int mouseH = mouseRowHeight(settings, leftSub != null || rightSub != null);

            drawKey(graphics, font, settings, "LMB", leftSub, innerX, cursorY, btnW, mouseH, lmb);
            drawKey(graphics, font, settings, "RMB", rightSub, innerX + btnW + gap, cursorY, btnW, mouseH, rmb);
            cursorY += mouseH + gap;
        }

        if (settings.showSpace()) {
            int keyH = settings.keyHeight();
            drawKey(graphics, font, settings, "SPACE", null, innerX, cursorY, contentW, keyH, preview || keys.keyJump.isDown());
            cursorY += keyH + settings.keyGap();
        }

        if (settings.cpsDisplay() == CpsDisplayMode.SEPARATE_ROW) {
            int leftCps = preview ? 12 : CpsTracker.getLeftCps();
            int rightCps = preview ? 8 : CpsTracker.getRightCps();
            String cpsText = "CPS  " + leftCps + "  |  " + rightCps;
            KeystrokesDraw.drawText(graphics, font, cpsText, innerX, cursorY + 1, contentW, settings.theme().idleText(), settings.textShadow());
        }
    }

    private static void drawKey(
            GuiGraphics graphics,
            Font font,
            KeystrokesSettings settings,
            String label,
            String subLabel,
            int x,
            int y,
            int w,
            int h,
            boolean pressed
    ) {
        KeystrokesDraw.drawKey(graphics, font, settings, label, subLabel, x, y, w, h, pressed);
    }

    private static String cpsSubLabel(KeystrokesSettings settings, int cps) {
        return switch (settings.cpsDisplay()) {
            case ON_BUTTONS, BELOW_MOUSE -> cps + " CPS";
            case OFF, SEPARATE_ROW -> null;
        };
    }

    private static int mouseRowHeight(KeystrokesSettings settings, boolean hasSubLabel) {
        int base = settings.keyHeight();
        if (hasSubLabel && (settings.cpsDisplay() == CpsDisplayMode.BELOW_MOUSE
                || settings.cpsDisplay() == CpsDisplayMode.ON_BUTTONS)) {
            return base + 8;
        }
        return base;
    }

    private static int movementWidth(KeystrokesSettings settings) {
        return settings.keyWidth() * 3 + settings.keyGap() * 2;
    }

    private static int contentHeight(KeystrokesSettings settings) {
        int height = 0;
        int gap = settings.keyGap();

        if (settings.showMovementKeys()) {
            height += settings.keyHeight() * 2 + gap;
        }
        if (settings.showMouseButtons()) {
            height += mouseRowHeight(settings, settings.cpsDisplay() == CpsDisplayMode.BELOW_MOUSE
                    || settings.cpsDisplay() == CpsDisplayMode.ON_BUTTONS) + gap;
        }
        if (settings.showSpace()) {
            height += settings.keyHeight() + gap;
        }
        if (settings.cpsDisplay() == CpsDisplayMode.SEPARATE_ROW) {
            height += 10;
        }

        if (height > 0) {
            height -= gap;
        }
        return height;
    }
}
