package com.flowclient.mods.preview;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileSimulation {
    private ProjectileSimulation() {
    }

    public static SimulationResult simulate(
            Level level,
            LocalPlayer player,
            Vec3 start,
            Vec3 velocity,
            double gravity,
            double drag,
            int maxSteps
    ) {
        Vec3 position = start;
        List<Vec3> points = new ArrayList<>();
        points.add(position);

        for (int step = 0; step < maxSteps; step++) {
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
                return new SimulationResult(points, hit.getLocation(), true);
            }

            points.add(next);
            position = next;
            velocity = velocity.scale(drag).subtract(0.0D, gravity, 0.0D);
            if (position.y < level.getMinY() - 16) {
                break;
            }
        }

        return new SimulationResult(points, points.getLast(), false);
    }

    public static SimulationResult simulateEnderPearl(Level level, LocalPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 velocity = player.getViewVector(1.0F).scale(1.5D);
        return simulate(level, player, start, velocity, 0.03D, 0.99D, 80);
    }

    public record SimulationResult(List<Vec3> points, Vec3 landing, boolean hitSurface) {
    }
}
