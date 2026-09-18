package com.flowclient.mods.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ClientShulkerTooltip implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int PADDING = 4;

    private final ItemContainerContents contents;

    public ClientShulkerTooltip(ItemContainerContents contents) {
        this.contents = contents;
    }

    @Override
    public int getHeight(Font font) {
        return ROWS * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        return COLS * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
        this.contents.copyInto(slots);

        for (int slot = 0; slot < slots.size(); slot++) {
            int col = slot % COLS;
            int row = slot / COLS;
            int drawX = x + PADDING + col * SLOT_SIZE;
            int drawY = y + PADDING + row * SLOT_SIZE;

            graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0x66000000);
            ItemStack stack = slots.get(slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, drawX, drawY);
                graphics.itemDecorations(font, stack, drawX, drawY);
            }
        }
    }
}
