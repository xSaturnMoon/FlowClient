package com.flowclient.mods.tooltip;

import com.flowclient.compat.FlowGfx;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class ClientShulkerTooltip implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int PADDING = 4;

    private final List<ItemStack> contents;

    public ClientShulkerTooltip(List<ItemStack> contents) {
        this.contents = contents;
    }

    @Override
    public int getHeight() {
        return ROWS * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        return COLS * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int slot = 0; slot < Math.min(this.contents.size(), COLS * ROWS); slot++) {
            int col = slot % COLS;
            int row = slot / COLS;
            int drawX = x + PADDING + col * SLOT_SIZE;
            int drawY = y + PADDING + row * SLOT_SIZE;

            graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0x66000000);
            ItemStack stack = this.contents.get(slot);
            if (!stack.isEmpty()) {
                FlowGfx.item(graphics, stack, drawX, drawY);
                graphics.renderItemDecorations(font, stack, drawX, drawY);
            }
        }
    }
}
