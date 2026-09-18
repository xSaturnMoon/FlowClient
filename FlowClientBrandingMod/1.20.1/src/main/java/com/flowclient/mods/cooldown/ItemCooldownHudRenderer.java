package com.flowclient.mods.cooldown;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.flowclient.compat.FlowArgb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ItemCooldownHudRenderer {
    private static final int BAR_HEIGHT = 6;
    private static final int ROW_GAP = 4;
    private static final int PADDING = 4;

    private ItemCooldownHudRenderer() {
    }

    public static int getWidth(Font font) {
        return getWidth(font, ItemCooldownHudSettings.get(), true, true);
    }

    public static int getHeight(Font font) {
        return getHeight(font, ItemCooldownHudSettings.get(), true, true);
    }

    public static int getWidth(Font font, ItemCooldownHudSettings settings, boolean showAttack, boolean showItem) {
        return settings.barWidth() + PADDING * 2;
    }

    public static int getHeight(Font font, ItemCooldownHudSettings settings, boolean showAttack, boolean showItem) {
        int rows = 0;
        if (showAttack && settings.showAttackCooldown()) {
            rows++;
        }
        if (showItem && settings.showItemCooldown()) {
            rows++;
        }
        if (rows == 0) {
            rows = 1;
        }
        return PADDING * 2 + rows * BAR_HEIGHT + Math.max(0, rows - 1) * ROW_GAP;
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !ItemCooldownHudMod.isEnabled()) {
            return;
        }

        Player player = mc.player;
        if (!preview && player == null) {
            return;
        }

        float partialTick = mc.getFrameTime();
        float attackReady = preview ? 0.45f : player.getAttackStrengthScale(partialTick);
        ItemStack mainHand = preview || player == null ? ItemStack.EMPTY : player.getMainHandItem();
        float itemCooldown = preview ? 0.35f : (mainHand.isEmpty()
                ? 0.0f
                : player.getCooldowns().getCooldownPercent(mainHand.getItem(), partialTick));

        ItemCooldownHudSettings settings = ItemCooldownHudSettings.get();
        boolean showAttack = settings.showAttackCooldown() && (preview || attackReady < 1.0f);
        boolean showItem = settings.showItemCooldown() && (preview || itemCooldown > 0.0f);
        if (!preview && !showAttack && !showItem) {
            return;
        }

        int width = getWidth(font, settings, showAttack || preview, showItem || preview);
        int height = getHeight(font, settings, showAttack || preview, showItem || preview);
        int x = HudLayoutManager.get().resolveX(HudElement.ITEM_COOLDOWN_HUD, width, height);
        int y = HudLayoutManager.get().resolveY(HudElement.ITEM_COOLDOWN_HUD, width, height);
        float scale = HudLayoutManager.get().resolveScale(HudElement.ITEM_COOLDOWN_HUD);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> {
            if (settings.showBackground()) {
                int alpha = (settings.backgroundOpacity() * 255) / 100;
                graphics.fill(x, y, x + width, y + height, FlowArgb.color(alpha, 8, 8, 12));
            }

            int barX = x + PADDING;
            int barY = y + PADDING;
            int barWidth = settings.barWidth();

            if (showAttack || preview) {
                drawBar(graphics, barX, barY, barWidth, preview ? 0.45f : attackReady, 0xFF55AAFF);
                barY += BAR_HEIGHT + ROW_GAP;
            }

            if (showItem || preview) {
                float fill = preview ? 0.35f : (1.0f - itemCooldown);
                drawBar(graphics, barX, barY, barWidth, fill, 0xFFFFAA55);
            }
        });
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, float fill, int color) {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, FlowArgb.color(255, 30, 30, 30));
        int filled = Math.max(1, (int) (width * Math.max(0.0f, Math.min(1.0f, fill))));
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        graphics.fill(x, y, x + width, y + 1, FlowArgb.color(255, 0, 0, 0));
        graphics.fill(x, y + BAR_HEIGHT - 1, x + width, y + BAR_HEIGHT, FlowArgb.color(255, 0, 0, 0));
    }
}
