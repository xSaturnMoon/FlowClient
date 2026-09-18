package com.flowclient.mods.advancements;

import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.ChangeDimensionTrigger;
import net.minecraft.advancements.triggers.ConsumeItemTrigger;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.EnterBlockTrigger;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.advancements.triggers.UsedEnderEyeTrigger;
import net.minecraft.advancements.triggers.UsedTotemTrigger;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

final class ClientCriterionMatcher {
    private ClientCriterionMatcher() {
    }

    static boolean matches(LocalPlayer player, Criterion<?> criterion, AdvancementEventContext context) {
        var instance = criterion.triggerInstance();
        if (instance instanceof ImpossibleTrigger.TriggerInstance) {
            return false;
        }

        if (instance instanceof SimpleCriterionTrigger.SimpleInstance simple
                && !ClientEntityMatcher.matchesPlayerPredicate(player, simple.player())) {
            return false;
        }

        return switch (instance) {
            case PlayerTrigger.TriggerInstance ignored -> true;
            case InventoryChangeTrigger.TriggerInstance inventory -> matchesInventory(player, inventory);
            case EnterBlockTrigger.TriggerInstance enter -> enter.matches(player.level().getBlockState(player.blockPosition()));
            case ConsumeItemTrigger.TriggerInstance consume -> context.anyConsumed(stack -> consume.matches(stack));
            case ChangeDimensionTrigger.TriggerInstance dimension -> matchesDimension(dimension, context);
            case RecipeCraftedTrigger.TriggerInstance crafted -> context.anyCrafted(recipe -> matchesCrafted(crafted, recipe));
            case KilledTrigger.TriggerInstance killed -> matchesKill(player, killed, triggerId(criterion), context);
            case UsedTotemTrigger.TriggerInstance totem -> context.anyTotem(stack -> totem.matches(stack));
            case UsedEnderEyeTrigger.TriggerInstance eye -> context.getLastEnderEyeDistance()
                    .map(eye::matches)
                    .orElse(false);
            default -> matchesByTrigger(player, criterion, context);
        };
    }

    private static boolean matchesInventory(LocalPlayer player, InventoryChangeTrigger.TriggerInstance instance) {
        var inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            int count = stack.getCount();
            if (instance.matches(inventory, stack, slot, count, count)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesDimension(ChangeDimensionTrigger.TriggerInstance instance, AdvancementEventContext context) {
        Optional<ResourceKey<Level>> from = context.getLastDimensionFrom();
        Optional<ResourceKey<Level>> to = context.getLastDimensionTo();
        if (from.isEmpty() || to.isEmpty()) {
            return false;
        }
        return instance.matches(from.get(), to.get());
    }

    private static boolean matchesCrafted(RecipeCraftedTrigger.TriggerInstance instance, ResourceKey<Recipe<?>> recipe) {
        return instance.recipeId().equals(recipe);
    }

    private static boolean matchesKill(
            LocalPlayer player,
            KilledTrigger.TriggerInstance instance,
            Identifier triggerId,
            AdvancementEventContext context
    ) {
        if (triggerId != null && "entity_killed_player".equals(triggerId.getPath())) {
            return context.anyDeath(death -> {
                Entity killer = death.damageSource().getEntity();
                if (killer == null) {
                    return instance.entity().isEmpty();
                }
                return matchesEntityPredicate(player, instance.entity(), killer);
            });
        }

        return context.anyKill(kill -> matchesEntityPredicate(player, instance.entity(), kill.entity()));
    }

    private static boolean matchesEntityPredicate(
            LocalPlayer player,
            Optional<ContextAwarePredicate> predicate,
            Entity entity
    ) {
        if (predicate.isEmpty()) {
            return true;
        }
        return ClientEntityMatcher.matchesContextPredicate(player, entity, predicate.get());
    }

    private static boolean matchesByTrigger(
            LocalPlayer player,
            Criterion<?> criterion,
            AdvancementEventContext context
    ) {
        Identifier triggerId = triggerId(criterion);
        if (triggerId == null) {
            return false;
        }

        return switch (triggerId.getPath()) {
            case "start_riding" -> player.isPassenger();
            case "using_item" -> !player.getUseItem().isEmpty();
            case "tick" -> player.tickCount > 40;
            case "inventory_changed" -> matchesInventory(player, (InventoryChangeTrigger.TriggerInstance) criterion.triggerInstance());
            case "enter_block" -> !player.level().getBlockState(player.blockPosition()).isAir();
            case "consume_item" -> context.hasConsumed();
            case "changed_dimension" -> context.hasChangedDimension();
            case "recipe_crafted", "crafter_recipe_crafted" -> context.hasCrafted();
            case "player_killed_entity", "kill_mob_near_sculk_catalyst", "killed_by_arrow", "spear_mobs" -> context.hasKills();
            case "entity_killed_player" -> context.hasDeaths();
            case "used_totem" -> context.hasUsedTotem();
            case "used_ender_eye" -> context.hasUsedEnderEye();
            case "filled_bucket" -> context.hasFilledBucket();
            case "shot_crossbow" -> context.hasCrossbowShots();
            case "trade" -> context.hasTrades();
            case "tame_animal" -> context.hasTamedAnimals();
            case "cured_zombie_villager" -> context.hasCuredVillagers();
            case "recipe_unlocked" -> context.hasUnlockedRecipes();
            case "item_durability_changed" -> context.hasDurabilityChanges();
            case "placed_block", "item_used_on_block", "allay_drop_item_on_block", "default_block_use", "any_block_use" ->
                    context.hasBlockInteractions();
            case "thrown_item_picked_up_by_player", "thrown_item_picked_up_by_entity" -> context.hasPickups();
            default -> false;
        };
    }

    private static Identifier triggerId(Criterion<?> criterion) {
        if (criterion.trigger() == null) {
            return null;
        }
        return BuiltInRegistries.TRIGGER_TYPES.getKey(criterion.trigger());
    }
}
