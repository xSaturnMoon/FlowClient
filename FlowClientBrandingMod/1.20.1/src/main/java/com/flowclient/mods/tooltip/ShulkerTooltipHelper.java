package com.flowclient.mods.tooltip;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShulkerTooltipHelper {
    private ShulkerTooltipHelper() {
    }

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static List<ItemStack> readContents(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", Tag.TAG_LIST)) {
            return null;
        }

        ListTag items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        if (items.isEmpty()) {
            return null;
        }

        List<ItemStack> contents = new java.util.ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ItemStack entry = ItemStack.of(items.getCompound(i));
            if (!entry.isEmpty()) {
                contents.add(entry);
            }
        }

        return contents.isEmpty() ? null : contents;
    }
}
