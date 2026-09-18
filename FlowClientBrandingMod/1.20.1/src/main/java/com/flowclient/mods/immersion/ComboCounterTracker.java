package com.flowclient.mods.immersion;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ComboCounterTracker {
    private static final long COMBO_WINDOW_MS = 2200L;
    private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();

    private static int combo;
    private static long lastHitMs;
    private static float pulse;

    private ComboCounterTracker() {
    }

    public static void reset() {
        LAST_HEALTH.clear();
        combo = 0;
        lastHitMs = 0L;
        pulse = 0.0F;
    }

    public static int combo() {
        return combo;
    }

    public static float pulse() {
        return pulse;
    }

    public static void tick(Minecraft client) {
        if (!ComboCounterMod.isEnabled() || client == null || client.level == null || client.player == null) {
            LAST_HEALTH.clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (combo > 0 && now - lastHitMs > COMBO_WINDOW_MS) {
            combo = 0;
        }
        if (pulse > 0.0F) {
            pulse = Math.max(0.0F, pulse - 0.08F);
        }

        Player player = client.player;
        for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48.0D))) {
            if (entity == player || !entity.isAlive()) {
                continue;
            }

            int id = entity.getId();
            float health = entity.getHealth();
            Float previous = LAST_HEALTH.get(id);
            if (previous != null) {
                float delta = previous - health;
                if (delta > 0.05F && entity.getLastHurtByMob() == player) {
                    registerHit(now);
                }
            }
            LAST_HEALTH.put(id, health);
        }
    }

    private static void registerHit(long now) {
        if (now - lastHitMs <= COMBO_WINDOW_MS) {
            combo++;
        } else {
            combo = 1;
        }
        lastHitMs = now;
        pulse = 1.0F;
    }
}
