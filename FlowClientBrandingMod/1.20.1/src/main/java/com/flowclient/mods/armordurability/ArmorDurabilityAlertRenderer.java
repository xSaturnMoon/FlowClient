package com.flowclient.mods.armordurability;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.flowclient.compat.FlowArgb;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorDurabilityAlertRenderer {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private ArmorDurabilityAlertRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, previewLines());
    }

    public static int getHeight(Font font) {
        return getHeight(font, previewLines());
    }

    public static int getWidth(Font font, List<String> lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width + 8;
    }

    public static int getHeight(Font font, List<String> lines) {
        if (lines.isEmpty()) {
            return font.lineHeight + 8;
        }
        return lines.size() * (font.lineHeight + 2) + 6;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !ArmorDurabilityAlertMod.isEnabled()) {
            return;
        }

        List<String> lines = preview ? previewLines() : collectAlerts(mc.player, ArmorDurabilityAlertSettings.get().thresholdPercent());
        if (!preview && lines.isEmpty()) {
            return;
        }

        ArmorDurabilityAlertSettings settings = ArmorDurabilityAlertSettings.get();
        int width = getWidth(font, lines);
        int height = getHeight(font, lines);
        int x = HudLayoutManager.get().resolveX(HudElement.ARMOR_DURABILITY_ALERT, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.ARMOR_DURABILITY_ALERT, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.ARMOR_DURABILITY_ALERT);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, FlowArgb.color(alpha, 40, 12, 12));
            }

            int lineY = y + 4;
            for (String line : lines) {
                FlowGfx.text(graphics, font, line, x + 4, lineY, 0xFFFF6655, settings.textShadow());
                lineY += font.lineHeight + 2;
            }
        });
    }

    private static List<String> collectAlerts(Player player, int thresholdPercent) {
        List<String> lines = new ArrayList<>();
        if (player == null) {
            return lines;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }

            int max = stack.getMaxDamage();
            if (max <= 0) {
                continue;
            }

            int remaining = max - stack.getDamageValue();
            int percent = Math.round(remaining * 100.0f / max);
            if (percent <= thresholdPercent) {
                lines.add(slotLabel(slot) + " " + percent + "%");
            }
        }
        return lines;
    }

    private static List<String> previewLines() {
        return List.of("Helmet 18%", "Boots 12%");
    }

    private static String slotLabel(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "Helmet";
            case CHEST -> "Chest";
            case LEGS -> "Legs";
            case FEET -> "Boots";
            default -> "Armor";
        };
    }
}
