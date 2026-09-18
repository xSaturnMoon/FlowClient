package com.flowclient.mods.elytra;

import com.flowclient.mods.preview.GizmoShapes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ElytraFlightPathRenderer {
    private static final int PATH_COLOR = 0xFFAA66FF;
    private static final int END_COLOR = 0xFFFFCC55;

    private ElytraFlightPathRenderer() {
    }

    public static boolean shouldRender() {
        return ElytraFlightPathMod.isEnabled();
    }

    public static void render(Minecraft client) {
        if (!ElytraFlightPathMod.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (!hasElytra(player) || !shouldPredict(player)) {
            return;
        }

        List<Vec3> points = simulate(client.level, player);
        if (points.size() < 2) {
            return;
        }

        for (int i = 1; i < points.size(); i++) {
            Gizmos.line(points.get(i - 1), points.get(i), PATH_COLOR, 2.0F);
        }

        Vec3 end = points.getLast();
        Gizmos.point(end, END_COLOR, 4.0F);
        GizmoShapes.horizontalCircle(end, 0.6D, END_COLOR, 1.25F);
    }

    private static boolean hasElytra(LocalPlayer player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA);
    }

    private static boolean shouldPredict(LocalPlayer player) {
        return player.isFallFlying() || (!player.onGround() && player.fallDistance > 0.5F);
    }

    private static List<Vec3> simulate(Level level, LocalPlayer player) {
        Vec3 position = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 velocity = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        List<Vec3> points = new ArrayList<>();
        points.add(position);

        boolean gliding = player.isFallFlying();
        for (int step = 0; step < 120; step++) {
            velocity = velocity.add(0.0D, gliding ? -0.01D : -0.08D, 0.0D);
            if (gliding || step > 4) {
                gliding = true;
                double lift = Math.max(-0.12D, -look.y * 0.06D);
                velocity = velocity.add(look.scale(0.02D)).add(0.0D, lift, 0.0D);
                velocity = velocity.scale(0.985D);
            }

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
            if (position.y < level.getMinY() - 16) {
                break;
            }
        }

        return points;
    }
}
