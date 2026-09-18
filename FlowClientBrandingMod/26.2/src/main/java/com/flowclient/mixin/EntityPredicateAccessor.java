package com.flowclient.mixin;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPredicate.class)
public interface EntityPredicateAccessor {
    @Accessor("parts")
    Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate> flowclient$getParts();
}
