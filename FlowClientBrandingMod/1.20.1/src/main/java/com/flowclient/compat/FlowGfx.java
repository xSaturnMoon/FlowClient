package com.flowclient.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class FlowGfx {
    private FlowGfx() {}

    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public static void text(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public static void text(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (size == 16) {
            graphics.renderItem(stack, x, y);
            return;
        }

        float scale = size / 16.0F;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0D);
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    public static void centeredText(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public static void centeredText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public static void blit(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            float u,
            float v,
            float uSize,
            float vSize
    ) {
        graphics.blit(texture, x, y, width, height, (int) (u * width), (int) (v * height), (int) (uSize * width), (int) (vSize * height), width, height);
    }
}
