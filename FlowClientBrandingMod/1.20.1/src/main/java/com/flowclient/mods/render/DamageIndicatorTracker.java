package com.flowclient.mods.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DamageIndicatorTracker {
    private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
    private static final List<DamagePopup> POPUPS = new ArrayList<>();

    private DamageIndicatorTracker() {
    }

    public static void clear() {
        LAST_HEALTH.clear();
        POPUPS.clear();
    }

    public static List<DamagePopup> popups() {
        return POPUPS;
    }

    public static void tick(Minecraft client) {
        if (client == null || client.level == null || !DamageIndicatorMod.isEnabled()) {
            LAST_HEALTH.clear();
            return;
        }

        long now = System.currentTimeMillis();
        DamageIndicatorSettings settings = DamageIndicatorSettings.get();

        if (client.player == null) {
            return;
        }

        for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, client.player.getBoundingBox().inflate(48.0D))) {
            if (entity == client.player) {
                continue;
            }

            int id = entity.getId();
            float health = entity.getHealth();
            Float previous = LAST_HEALTH.get(id);
            if (previous != null) {
                float delta = previous - health;
                if (delta > 0.05F) {
                    spawn(entity, delta, false, now);
                } else if (settings.showHealing() && delta < -0.05F) {
                    spawn(entity, -delta, true, now);
                }
            }
            LAST_HEALTH.put(id, health);
        }

        POPUPS.removeIf(popup -> now - popup.spawnTimeMs() > settings.durationMs());
    }

    private static void spawn(LivingEntity entity, float amount, boolean healing, long now) {
        Vec3 position = entity.position().add(0.0D, entity.getBbHeight() + 0.35D, 0.0D);
        POPUPS.add(new DamagePopup(entity.getId(), amount, healing, position, now));
    }

    public record DamagePopup(
            int entityId,
            float amount,
            boolean healing,
            Vec3 worldPosition,
            long spawnTimeMs,
            float rise
    ) {
        DamagePopup(int entityId, float amount, boolean healing, Vec3 worldPosition, long spawnTimeMs) {
            this(entityId, amount, healing, worldPosition, spawnTimeMs, 0.0F);
        }

        DamagePopup withRise(float rise) {
            return new DamagePopup(this.entityId, this.amount, this.healing, this.worldPosition, this.spawnTimeMs, rise);
        }
    }

    static void advancePopups() {
        for (int i = 0; i < POPUPS.size(); i++) {
            DamagePopup popup = POPUPS.get(i);
            POPUPS.set(i, popup.withRise(popup.rise() + 0.04F));
        }
    }
}
