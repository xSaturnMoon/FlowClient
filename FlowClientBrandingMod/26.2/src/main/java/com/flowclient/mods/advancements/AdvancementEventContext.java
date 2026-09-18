package com.flowclient.mods.advancements;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class AdvancementEventContext {
    private static final int MAX_EVENTS = 32;

    private static final AdvancementEventContext INSTANCE = new AdvancementEventContext();

    private final Deque<ItemStack> consumedItems = new ArrayDeque<>();
    private final Deque<ItemStack> totemUses = new ArrayDeque<>();
    private final Deque<KillEvent> kills = new ArrayDeque<>();
    private final Deque<DeathEvent> deaths = new ArrayDeque<>();
    private final Deque<ResourceKey<Recipe<?>>> craftedRecipes = new ArrayDeque<>();
    private final Deque<ItemStack> pickedUpItems = new ArrayDeque<>();

    private Optional<ResourceKey<Level>> lastDimensionFrom = Optional.empty();
    private Optional<ResourceKey<Level>> lastDimensionTo = Optional.empty();
    private Optional<Double> lastEnderEyeDistance = Optional.empty();
    private Optional<BlockState> lastBrokenBlock = Optional.empty();

    private int blockInteractions;
    private int filledBuckets;
    private int crossbowShots;
    private int trades;
    private int tamedAnimals;
    private int curedVillagers;
    private int unlockedRecipes;
    private int durabilityChanges;
    private int genericActivity;

    private AdvancementEventContext() {
    }

    static AdvancementEventContext get() {
        return INSTANCE;
    }

    void reset() {
        this.consumedItems.clear();
        this.totemUses.clear();
        this.kills.clear();
        this.deaths.clear();
        this.craftedRecipes.clear();
        this.pickedUpItems.clear();
        this.lastDimensionFrom = Optional.empty();
        this.lastDimensionTo = Optional.empty();
        this.lastEnderEyeDistance = Optional.empty();
        this.lastBrokenBlock = Optional.empty();
        this.blockInteractions = 0;
        this.filledBuckets = 0;
        this.crossbowShots = 0;
        this.trades = 0;
        this.tamedAnimals = 0;
        this.curedVillagers = 0;
        this.unlockedRecipes = 0;
        this.durabilityChanges = 0;
        this.genericActivity = 0;
    }

    void onBlockBroken(BlockState state) {
        this.lastBrokenBlock = Optional.of(state);
        this.genericActivity++;
    }

    void onItemPickup(ItemStack stack) {
        push(this.pickedUpItems, stack.copy());
        this.genericActivity++;
    }

    void onMobKill(Entity entity) {
        push(this.kills, new KillEvent(entity));
        this.genericActivity++;
    }

    void onDeath(DamageSource source) {
        push(this.deaths, new DeathEvent(source));
        this.genericActivity++;
    }

    void onItemCrafted(ResourceKey<Recipe<?>> recipe) {
        push(this.craftedRecipes, recipe);
        this.genericActivity++;
    }

    void onItemUsed() {
        this.blockInteractions++;
        this.genericActivity++;
    }

    void onItemConsumed(ItemStack stack) {
        push(this.consumedItems, stack.copy());
        this.genericActivity++;
    }

    void onBucketFilled() {
        this.filledBuckets++;
        this.genericActivity++;
    }

    void onCrossbowShot() {
        this.crossbowShots++;
        this.genericActivity++;
    }

    void onAnimalTamed() {
        this.tamedAnimals++;
        this.genericActivity++;
    }

    void onVillagerCured() {
        this.curedVillagers++;
        this.genericActivity++;
    }

    void onTrade() {
        this.trades++;
        this.genericActivity++;
    }

    void onRecipeUnlocked() {
        this.unlockedRecipes++;
        this.genericActivity++;
    }

    void onDurabilityChange() {
        this.durabilityChanges++;
        this.genericActivity++;
    }

    void onDimensionChange(ResourceKey<Level> from, ResourceKey<Level> to) {
        this.lastDimensionFrom = Optional.of(from);
        this.lastDimensionTo = Optional.of(to);
        this.genericActivity++;
    }

    void onTotemUsed(ItemStack stack) {
        push(this.totemUses, stack.copy());
        this.genericActivity++;
    }

    void onEnderEyeUsed(double distance) {
        this.lastEnderEyeDistance = Optional.of(distance);
        this.genericActivity++;
    }

    boolean anyConsumed(Predicate<ItemStack> predicate) {
        return this.consumedItems.stream().anyMatch(predicate);
    }

    boolean anyTotem(Predicate<ItemStack> predicate) {
        return this.totemUses.stream().anyMatch(predicate);
    }

    boolean anyKill(Predicate<KillEvent> predicate) {
        return this.kills.stream().anyMatch(predicate);
    }

    boolean anyDeath(Predicate<DeathEvent> predicate) {
        return this.deaths.stream().anyMatch(predicate);
    }

    boolean anyCrafted(Predicate<ResourceKey<Recipe<?>>> predicate) {
        return this.craftedRecipes.stream().anyMatch(predicate);
    }

    Optional<Double> getLastEnderEyeDistance() {
        return this.lastEnderEyeDistance;
    }

    Optional<ResourceKey<Level>> getLastDimensionFrom() {
        return this.lastDimensionFrom;
    }

    Optional<ResourceKey<Level>> getLastDimensionTo() {
        return this.lastDimensionTo;
    }

    boolean hasConsumed() {
        return !this.consumedItems.isEmpty();
    }

    boolean hasUsedTotem() {
        return !this.totemUses.isEmpty();
    }

    boolean hasUsedEnderEye() {
        return this.lastEnderEyeDistance.isPresent();
    }

    boolean hasChangedDimension() {
        return this.lastDimensionFrom.isPresent() && this.lastDimensionTo.isPresent();
    }

    boolean hasCrafted() {
        return !this.craftedRecipes.isEmpty();
    }

    boolean hasKills() {
        return !this.kills.isEmpty();
    }

    boolean hasDeaths() {
        return !this.deaths.isEmpty();
    }

    boolean hasFilledBucket() {
        return this.filledBuckets > 0;
    }

    boolean hasCrossbowShots() {
        return this.crossbowShots > 0;
    }

    boolean hasTrades() {
        return this.trades > 0;
    }

    boolean hasTamedAnimals() {
        return this.tamedAnimals > 0;
    }

    boolean hasCuredVillagers() {
        return this.curedVillagers > 0;
    }

    boolean hasUnlockedRecipes() {
        return this.unlockedRecipes > 0;
    }

    boolean hasDurabilityChanges() {
        return this.durabilityChanges > 0;
    }

    boolean hasBlockInteractions() {
        return this.blockInteractions > 0 || this.lastBrokenBlock.isPresent();
    }

    boolean hasPickups() {
        return !this.pickedUpItems.isEmpty();
    }

    boolean hasRecentActivity() {
        return this.genericActivity > 0;
    }

    private static <T> void push(Deque<T> deque, T value) {
        deque.addFirst(value);
        while (deque.size() > MAX_EVENTS) {
            deque.removeLast();
        }
    }

    record KillEvent(Entity entity) {
    }

    record DeathEvent(DamageSource damageSource) {
    }
}
