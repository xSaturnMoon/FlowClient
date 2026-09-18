package com.flowclient.mixin;

import java.util.List;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContextAwarePredicate.class)
public interface ContextAwarePredicateAccessor {
    @Accessor("conditions")
    List<LootItemCondition> flowclient$getConditions();
}
