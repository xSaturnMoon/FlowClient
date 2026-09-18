package com.flowclient.mods.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.ItemContainerContents;

public record ShulkerBoxTooltip(ItemContainerContents contents) implements TooltipComponent {
}
