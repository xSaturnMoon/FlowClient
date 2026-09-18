package com.flowclient.mods.armor;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Equipment HUD with configurable slots, durability display, and panel styling.
 */
public final class ArmorHudRenderer {
    private static final int ICON_SIZE = 16;
    private static final int ROW_H = 18;
    private static final int TEXT_WIDTH = 36;

    private ArmorHudRenderer() {}

    public static int getWidth() {
        return getWidth(Minecraft.getInstance().font, ArmorHudSettings.get(), previewSlots());
    }

    public static int getHeight() {
        return getHeight(ArmorHudSettings.get(), previewSlots());
    }

    public static int getWidth(Font font, ArmorHudSettings settings, List<EquipmentSlot> slots) {
        if (slots.isEmpty()) {
            return ICON_SIZE + TEXT_WIDTH + settings.paddingX() * 2;
        }
        return ICON_SIZE + TEXT_WIDTH + settings.paddingX() * 2;
    }

    public static int getHeight(ArmorHudSettings settings, List<EquipmentSlot> slots) {
        if (slots.isEmpty()) {
            return ROW_H + settings.paddingY() * 2;
        }
        return slots.size() * ROW_H + settings.paddingY() * 2;
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphicsExtractor graphics, Font font, boolean preview) {
        if (!preview && (!ArmorHudMod.isEnabled() || mc.player == null)) {
            return;
        }

        ArmorHudSettings settings = ArmorHudSettings.get();
        Player player = preview && mc.player == null ? null : mc.player;
        List<EquipmentSlot> slots = preview ? previewSlots() : visibleSlots(player, settings);
        if (slots.isEmpty()) {
            return;
        }

        int width = getWidth(font, settings, slots);
        int height = getHeight(settings, slots);
        int x = HudLayoutManager.get().resolveX(HudElement.ARMOR_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.ARMOR_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.ARMOR_HUD);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, (alpha << 24) | 0x08080C);
            }

            if (settings.showOutline()) {
                graphics.fill(x, y, x + width, y + 1, 0xFF4A9EE0);
                graphics.fill(x, y + height - 1, x + width, y + height, 0xFF1E2A40);
                graphics.fill(x, y, x + 1, y + height, 0xFF4A9EE0);
                graphics.fill(x + width - 1, y, x + width, y + height, 0xFF1E2A40);
            }

            int iconX = x + settings.paddingX() + TEXT_WIDTH;
            int rowY = y + settings.paddingY();

            for (EquipmentSlot slot : slots) {
                ItemStack stack = player != null ? player.getItemBySlot(slot) : ItemStack.EMPTY;
                if (preview && stack.isEmpty()) {
                    stack = previewStack(slot);
                }

                if (!stack.isEmpty()) {
                    if (settings.showDurability() && stack.isDamageableItem()) {
                        int remaining = stack.getMaxDamage() - stack.getDamageValue();
                        String text = formatDurability(remaining, stack.getMaxDamage(), settings.durabilityFormat());
                        int textW = font.width(text);
                        int color = durabilityTextColor(remaining, stack.getMaxDamage());
                        graphics.text(font, text, iconX - 5 - textW, rowY + 5, color, settings.textShadow());
                    }

                    graphics.item(stack, iconX, rowY);
                }

                rowY += ROW_H;
            }
        });
    }

    private static List<EquipmentSlot> visibleSlots(Player player, ArmorHudSettings settings) {
        List<EquipmentSlot> slots = new ArrayList<>(5);
        if (settings.showMainHand()) {
            addIfEquipped(slots, player, EquipmentSlot.MAINHAND);
        }
        if (settings.showHelmet()) {
            addIfEquipped(slots, player, EquipmentSlot.HEAD);
        }
        if (settings.showChestplate()) {
            addIfEquipped(slots, player, EquipmentSlot.CHEST);
        }
        if (settings.showLeggings()) {
            addIfEquipped(slots, player, EquipmentSlot.LEGS);
        }
        if (settings.showBoots()) {
            addIfEquipped(slots, player, EquipmentSlot.FEET);
        }
        return slots;
    }

    private static void addIfEquipped(List<EquipmentSlot> slots, Player player, EquipmentSlot slot) {
        if (player != null && !player.getItemBySlot(slot).isEmpty()) {
            slots.add(slot);
        }
    }

    private static List<EquipmentSlot> previewSlots() {
        ArmorHudSettings settings = ArmorHudSettings.get();
        List<EquipmentSlot> slots = new ArrayList<>(5);
        if (settings.showMainHand()) slots.add(EquipmentSlot.MAINHAND);
        if (settings.showHelmet()) slots.add(EquipmentSlot.HEAD);
        if (settings.showChestplate()) slots.add(EquipmentSlot.CHEST);
        if (settings.showLeggings()) slots.add(EquipmentSlot.LEGS);
        if (settings.showBoots()) slots.add(EquipmentSlot.FEET);
        return slots;
    }

    private static String formatDurability(int remaining, int max, ArmorHudDurabilityFormat format) {
        if (format == ArmorHudDurabilityFormat.PERCENT) {
            int percent = max > 0 ? Math.round(remaining * 100f / max) : 0;
            return percent + "%";
        }
        return String.valueOf(remaining);
    }

    private static ItemStack previewStack(EquipmentSlot slot) {
        ItemStack stack = switch (slot) {
            case MAINHAND -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            case HEAD -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
            case CHEST -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            case LEGS -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
            case FEET -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS);
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            stack.setDamageValue(stack.getMaxDamage() / 4);
        }
        return stack;
    }

    private static int durabilityTextColor(int remaining, int max) {
        if (max <= 0) {
            return 0xFFFFFFFF;
        }
        float ratio = (float) remaining / max;
        if (ratio > 0.5f) {
            return 0xFFFFFFFF;
        }
        if (ratio > 0.25f) {
            return 0xFFFFEE58;
        }
        return 0xFFFF7043;
    }
}
