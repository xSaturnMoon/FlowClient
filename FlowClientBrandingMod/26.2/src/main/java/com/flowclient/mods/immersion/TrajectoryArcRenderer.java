package com.flowclient.mods.immersion;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TrajectoryArcRenderer {
    private TrajectoryArcRenderer() {
    }

    public static void render(Minecraft client) {
        if (!TrajectoryArcMod.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        TrajectoryProfile profile = resolveProfile(client.player);
        if (profile == null) {
            return;
        }

        List<Vec3> points = simulate(client.level, client.player, profile);
        if (points.size() < 2) {
            return;
        }

        int color = profile.color();
        for (int i = 1; i < points.size(); i++) {
            Gizmos.line(points.get(i - 1), points.get(i), color, 2.0F);
        }
        Gizmos.point(points.getLast(), 0xFFFFEE55, 4.0F);
    }

    private static TrajectoryProfile resolveProfile(LocalPlayer player) {
        ItemStack using = player.getUseItem();
        if (player.isUsingItem() && using.getItem() instanceof BowItem) {
            float power = BowItem.getPowerForTime(player.getTicksUsingItem());
            if (power < 0.1F) {
                return null;
            }
            return new TrajectoryProfile(power * 3.0F, 0.05D, 0.99D, 0xFF55FFAA);
        }

        ItemStack main = player.getMainHandItem();
        Item item = main.getItem();
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG
                || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION
                || item == Items.EXPERIENCE_BOTTLE) {
            return new TrajectoryProfile(1.5F, 0.03D, 0.99D, 0xFF66AAFF);
        }
        if (item == Items.TRIDENT) {
            return new TrajectoryProfile(2.5F, 0.03D, 0.99D, 0xFF88DDFF);
        }
        if (item == Items.BOW) {
            return new TrajectoryProfile(3.0F, 0.05D, 0.99D, 0xFF55FFAA);
        }
        if (item == Items.CROSSBOW && net.minecraft.world.item.CrossbowItem.isCharged(main)) {
            return new TrajectoryProfile(3.15F, 0.05D, 0.99D, 0xFF55FFAA);
        }
        return null;
    }

    private static List<Vec3> simulate(Level level, LocalPlayer player, TrajectoryProfile profile) {
        Vec3 position = player.getEyePosition();
        Vec3 velocity = player.getViewVector(1.0F).scale(profile.initialSpeed());
        List<Vec3> points = new ArrayList<>();
        points.add(position);

        for (int step = 0; step < 80; step++) {
            Vec3 next = position.add(velocity);
            HitResult hit = level.clip(new ClipContext(
                    position,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                break;
            }

            points.add(next);
            position = next;
            velocity = velocity.scale(profile.drag()).subtract(0.0D, profile.gravity(), 0.0D);
            if (position.y < level.getMinY() - 16) {
                break;
            }
        }
        return points;
    }

    private record TrajectoryProfile(float initialSpeed, double gravity, double drag, int color) {
    }
}
