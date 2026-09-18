package com.flowclient.mods.advancements;

import com.flowclient.mixin.EntityPredicateAccessor;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityLocationPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.advancements.predicates.entity.MovementPredicate;
import net.minecraft.advancements.predicates.entity.SteppingOnPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import com.flowclient.mixin.ContextAwarePredicateAccessor;

final class ClientEntityMatcher {
    private ClientEntityMatcher() {
    }

    static boolean matchesPlayerPredicate(LocalPlayer player, Optional<ContextAwarePredicate> predicate) {
        if (predicate.isEmpty()) {
            return true;
        }
        return matchesContextPredicate(player, player, predicate.get());
    }

    static boolean matchesEntityPredicate(LocalPlayer player, Entity entity, EntityPredicate predicate) {
        if (predicate == null) {
            return true;
        }

        Map<?, EntitySubPredicate> parts = ((EntityPredicateAccessor) (Object) predicate).flowclient$getParts();
        Vec3 position = entity.position();
        Level level = player.level();
        for (EntitySubPredicate part : parts.values()) {
            if (!matchesSubPredicate(player, entity, level, position, part)) {
                return false;
            }
        }
        return true;
    }

    static boolean matchesContextPredicate(LocalPlayer player, Entity subject, ContextAwarePredicate predicate) {
        for (LootItemCondition condition : ((ContextAwarePredicateAccessor) (Object) predicate).flowclient$getConditions()) {
            if (condition instanceof LootItemEntityPropertyCondition entityCondition) {
                Entity target = resolveEntityTarget(player, subject, entityCondition.entityTarget());
                if (target == null) {
                    return false;
                }
                Optional<EntityPredicate> entityPredicate = entityCondition.predicate();
                if (entityPredicate.isPresent() && !matchesEntityPredicate(player, target, entityPredicate.get())) {
                    return false;
                }
            } else if (condition instanceof LootItemBlockStatePropertyCondition blockCondition) {
                BlockPos pos = subject.blockPosition();
                if (!player.level().getBlockState(pos).is(blockCondition.block())) {
                    return false;
                }
                if (blockCondition.properties().isPresent()
                        && !blockCondition.properties().get().matches(player.level().getBlockState(pos))) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static Entity resolveEntityTarget(LocalPlayer player, Entity subject, LootContext.EntityTarget target) {
        return switch (target) {
            case THIS -> subject;
            case ATTACKER -> player.getLastHurtByMob();
            case DIRECT_ATTACKER -> player.getLastHurtByMob();
            case TARGET_ENTITY -> player.getVehicle();
            default -> subject;
        };
    }

    private static boolean matchesSubPredicate(
            LocalPlayer player,
            Entity entity,
            Level level,
            Vec3 position,
            EntitySubPredicate part
    ) {
        return switch (part) {
            case EntityTypePredicate type -> type.matches(entity.getType().builtInRegistryHolder());
            case EntityFlagsPredicate flags -> flags.matches(entity);
            case EntityLocationPredicate located -> ClientLocationMatcher.matches(level, position, located.predicate());
            case SteppingOnPredicate stepping -> {
                BlockPos below = entity.blockPosition().below();
                yield ClientLocationMatcher.matches(level, Vec3.atCenterOf(below), stepping.predicate());
            }
            case MovementPredicate movement -> movement.matches(
                    entity.getX() - entity.xOld,
                    entity.getY() - entity.yOld,
                    entity.getZ() - entity.zOld,
                    entity.fallDistance
            );
            case EntityEquipmentPredicate equipment -> equipment.matches(entity);
            default -> true;
        };
    }
}
