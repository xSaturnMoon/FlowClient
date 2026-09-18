package com.flowclient.mods.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InventorySortExecutor {
    private static final List<Runnable> ACTIONS = new ArrayList<>();

    private InventorySortExecutor() {
    }

    public static boolean isRunning() {
        return !ACTIONS.isEmpty();
    }

    public static void cancel() {
        ACTIONS.clear();
    }

    public static void requestSort(Minecraft client) {
        if (!InventorySortMod.isEnabled() || client.player == null || client.gameMode == null) {
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen<?>)) {
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        List<Integer> slots = collectSortableSlots(menu, client.player.getInventory());
        if (slots.size() < 2) {
            return;
        }

        ACTIONS.clear();
        if (!menu.getCarried().isEmpty()) {
            ACTIONS.add(() -> click(client, AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0, ClickType.PICKUP));
        }

        if (InventorySortSettings.get().mergeStacks()) {
            for (int slot : slots) {
                ItemStack stack = menu.getSlot(slot).getItem();
                if (stack.isEmpty() || stack.getCount() >= stack.getMaxStackSize()) {
                    continue;
                }
                int menuSlot = slot;
                ACTIONS.add(() -> click(client, menuSlot, 0, ClickType.QUICK_MOVE));
            }
        }

        for (int i = 0; i < slots.size(); i++) {
            int best = i;
            for (int j = i + 1; j < slots.size(); j++) {
                if (compareStacks(menu.getSlot(slots.get(j)).getItem(), menu.getSlot(slots.get(best)).getItem()) < 0) {
                    best = j;
                }
            }
            if (best != i) {
                int slotA = slots.get(i);
                int slotB = slots.get(best);
                ACTIONS.add(() -> swapSlots(client, slotA, slotB));
                slots.set(i, slotB);
                slots.set(best, slotA);
            }
        }
    }

    public static void tick(Minecraft client) {
        if (ACTIONS.isEmpty() || client.player == null || client.gameMode == null) {
            ACTIONS.clear();
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen<?>)) {
            ACTIONS.clear();
            return;
        }

        ACTIONS.remove(0).run();
    }

    private static List<Integer> collectSortableSlots(AbstractContainerMenu menu, Inventory inventory) {
        List<Integer> slots = new ArrayList<>();
        InventorySortSettings settings = InventorySortSettings.get();

        for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
            Slot slot = menu.getSlot(menuIndex);
            if (slot.container != inventory) {
                continue;
            }

            int containerSlot = slot.getContainerSlot();
            boolean hotbar = containerSlot >= 0 && containerSlot < Inventory.getSelectionSize();
            boolean main = containerSlot >= Inventory.getSelectionSize() && containerSlot < 36;
            if (hotbar && !settings.includeHotbar()) {
                continue;
            }
            if (!hotbar && !main) {
                continue;
            }

            slots.add(menuIndex);
        }

        slots.sort(Integer::compareTo);
        return slots;
    }

    private static void swapSlots(Minecraft client, int slotA, int slotB) {
        if (slotA == slotB) {
            return;
        }
        click(client, slotA, 0, ClickType.PICKUP);
        click(client, slotB, 0, ClickType.PICKUP);
        click(client, slotA, 0, ClickType.PICKUP);
    }

    private static void click(Minecraft client, int slot, int button, ClickType clickType) {
        MultiPlayerGameMode mode = client.gameMode;
        LocalPlayer player = client.player;
        if (mode == null || player == null) {
            return;
        }
        mode.handleInventoryMouseClick(player.containerMenu.containerId, slot, button, clickType, player);
    }

    private static int compareStacks(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 0;
        }
        if (left.isEmpty()) {
            return 1;
        }
        if (right.isEmpty()) {
            return -1;
        }

        Comparator<ItemStack> comparator = Comparator
                .comparing((ItemStack stack) -> stack.getItem().getDescriptionId())
                .thenComparing(ItemStack::getCount, Comparator.reverseOrder());
        return comparator.compare(left, right);
    }
}
