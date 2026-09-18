package com.flowclient.mods.advancements;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Client-side advancement unlock is not supported on 1.20.1; this controller is a no-op stub.
 */
public final class AdvancementsUnlockController {
    private static final AdvancementsUnlockController INSTANCE = new AdvancementsUnlockController();

    private AdvancementsUnlockController() {
    }

    public static AdvancementsUnlockController get() {
        return INSTANCE;
    }

    public void reset() {
    }

    public void onBlockBroken(BlockState state) {
    }

    public void onItemPickup(ItemStack stack) {
    }

    public void onMobKill(Entity entity) {
    }

    public void onDeath(DamageSource source) {
    }

    public void onItemCrafted(ResourceKey<Recipe<?>> recipe) {
    }

    public void onItemUsed() {
    }

    public void onItemConsumed(ItemStack stack) {
    }

    public void onDimensionChange(ResourceKey<Level> from, ResourceKey<Level> to) {
    }

    public void tick(Minecraft client) {
    }
}
