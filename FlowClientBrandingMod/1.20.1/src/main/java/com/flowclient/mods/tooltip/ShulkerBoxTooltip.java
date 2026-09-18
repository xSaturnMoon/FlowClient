package com.flowclient.mods.tooltip;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record ShulkerBoxTooltip(List<ItemStack> contents) implements TooltipComponent {
}
