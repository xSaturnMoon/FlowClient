package com.flowclient.mods.battery;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

public final class BatteryHudRenderer {
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 6;
    private static final int ROW_GAP = 3;
    private static final int BAR_TEXT_GAP = 4;

    private BatteryHudRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, BatteryHudSettings.get(), previewDevices());
    }

    public static int getHeight(Font font) {
        return getHeight(font, BatteryHudSettings.get(), previewDevices());
    }

    public static int getWidth(Font font, BatteryHudSettings settings, List<BatteryDevice> devices) {
        int maxText = 0;
        for (BatteryDevice device : devices) {
            maxText = Math.max(maxText, font.width(formatLine(device, settings)));
        }
        int content = settings.showProgressBar()
                ? BAR_WIDTH + BAR_TEXT_GAP + maxText
                : maxText;
        return content + settings.paddingX() * 2;
    }

    public static int getHeight(Font font, BatteryHudSettings settings, List<BatteryDevice> devices) {
        if (devices.isEmpty()) {
            return font.lineHeight + settings.paddingY() * 2;
        }
        int rows = devices.size();
        int rowHeight = Math.max(font.lineHeight, settings.showProgressBar() ? BAR_HEIGHT : font.lineHeight);
        int content = rows * rowHeight + Math.max(0, rows - 1) * ROW_GAP;
        return content + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && !BatteryHudMod.isEnabled()) {
            return;
        }

        BatteryHudSettings settings = BatteryHudSettings.get();
        List<BatteryDevice> resolvedDevices = preview
                ? previewDevices()
                : filterDevices(BatteryPoller.get().snapshot(), settings);
        if (!preview && resolvedDevices.isEmpty()) {
            if (BatteryPoller.get().isScanning()) {
                resolvedDevices = List.of(placeholder("Scanning"));
            } else if (settings.showConnectedDevices() || settings.showSystemBattery()) {
                resolvedDevices = List.of(placeholder("No devices"));
            } else {
                return;
            }
        }

        final List<BatteryDevice> devices = resolvedDevices;
        int width = getWidth(font, settings, devices);
        int height = getHeight(font, settings, devices);
        int x = HudLayoutManager.get().resolveX(HudElement.BATTERY_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.BATTERY_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.BATTERY_HUD);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, ARGB.color(alpha, 8, 8, 12));
            }

            int contentX = x + settings.paddingX();
            int contentY = y + settings.paddingY();
            int rowHeight = Math.max(font.lineHeight, settings.showProgressBar() ? BAR_HEIGHT : font.lineHeight);

            for (int i = 0; i < devices.size(); i++) {
                BatteryDevice device = devices.get(i);
                int rowY = contentY + i * (rowHeight + ROW_GAP);
                int textY = rowY + (settings.showProgressBar() ? (BAR_HEIGHT - font.lineHeight) / 2 : 0);
                int textColor = colorFor(device, settings);
                boolean placeholder = isPlaceholder(device);

                if (settings.showProgressBar() && !placeholder) {
                    drawBar(graphics, contentX, rowY, BAR_WIDTH, BAR_HEIGHT, device.percent(), textColor);
                }

                int textX = settings.showProgressBar()
                        ? contentX + BAR_WIDTH + BAR_TEXT_GAP
                        : contentX;
                graphics.text(
                        font,
                        formatLine(device, settings),
                        textX,
                        textY,
                        textColor,
                        settings.textShadow()
                );
            }
        });
    }

    private static List<BatteryDevice> filterDevices(BatterySnapshot snapshot, BatteryHudSettings settings) {
        if (snapshot == null || snapshot.isEmpty()) {
            return List.of();
        }

        List<BatteryDevice> filtered = new ArrayList<>();
        for (BatteryDevice device : snapshot.devices()) {
            if (device.type() == BatteryDeviceType.SYSTEM && !settings.showSystemBattery()) {
                continue;
            }
            if (device.type() == BatteryDeviceType.CONNECTED && !settings.showConnectedDevices()) {
                continue;
            }
            filtered.add(device);
        }
        return filtered;
    }

    private static BatteryDevice placeholder(String label) {
        return new BatteryDevice(label, 0, false, BatteryDeviceType.CONNECTED);
    }

    private static boolean isPlaceholder(BatteryDevice device) {
        return device.percent() <= 0
                && ("Scanning".equals(device.name()) || "No devices".equals(device.name()));
    }

    private static List<BatteryDevice> previewDevices() {
        return List.of(
                new BatteryDevice("Headphones", 68, false, BatteryDeviceType.CONNECTED),
                new BatteryDevice("Mouse", 45, false, BatteryDeviceType.CONNECTED)
        );
    }

    private static String formatLine(BatteryDevice device, BatteryHudSettings settings) {
        if ("Scanning".equals(device.name())) {
            return "Scanning...";
        }
        if ("No devices".equals(device.name())) {
            return "No devices found";
        }
        String percent = device.percent() + "%";
        String charge = device.charging() ? " +" : "";
        String shortName = shorten(device.name(), 18);
        if (!settings.showDeviceNames()) {
            return percent + charge;
        }
        return shortName + " " + percent + charge;
    }

    private static String shorten(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static int colorFor(BatteryDevice device, BatteryHudSettings settings) {
        if (isPlaceholder(device)) {
            return 0xFF9AA3B2;
        }
        if (device.charging()) {
            return 0xFF66BB6A;
        }
        if (device.percent() <= settings.lowThreshold()) {
            return 0xFFE57373;
        }
        if (device.percent() <= settings.midThreshold()) {
            return 0xFFFFD54F;
        }
        return 0xFFE0E6F0;
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int percent, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        int fillWidth = Math.max(1, (width - 2) * percent / 100);
        graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, fillColor);
        graphics.fill(x, y, x + width, y + 1, 0xFF000000);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
        graphics.fill(x, y, x + 1, y + height, 0xFF000000);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000);
    }
}
