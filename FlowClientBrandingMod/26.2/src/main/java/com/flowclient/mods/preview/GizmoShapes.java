package com.flowclient.mods.preview;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

public final class GizmoShapes {
    private static final int CIRCLE_SEGMENTS = 48;

    private GizmoShapes() {
    }

    public static void horizontalCircle(Vec3 center, double radius, int color, float lineWidth) {
        if (radius <= 0.0D) {
            return;
        }

        double y = center.y;
        Vec3 previous = null;
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = (Math.PI * 2.0D * i) / CIRCLE_SEGMENTS;
            Vec3 point = new Vec3(
                    center.x + Math.cos(angle) * radius,
                    y,
                    center.z + Math.sin(angle) * radius
            );
            if (previous != null) {
                Gizmos.line(previous, point, color, lineWidth);
            }
            previous = point;
        }
    }

    public static void wireframeSphere(Vec3 center, double radius, int color, float lineWidth) {
        horizontalCircle(center, radius, color, lineWidth);
        horizontalCircle(center.add(0.0D, radius * 0.5D, 0.0D), radius * 0.86D, color, lineWidth * 0.75F);
        horizontalCircle(center.subtract(0.0D, radius * 0.5D, 0.0D), radius * 0.86D, color, lineWidth * 0.75F);

        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 0.5D * i);
            Vec3 previous = null;
            for (int step = 0; step <= CIRCLE_SEGMENTS; step++) {
                double t = (Math.PI * step) / CIRCLE_SEGMENTS;
                Vec3 point = new Vec3(
                        center.x + Math.cos(angle) * Math.sin(t) * radius,
                        center.y + Math.cos(t) * radius,
                        center.z + Math.sin(angle) * Math.sin(t) * radius
                );
                if (previous != null) {
                    Gizmos.line(previous, point, color, lineWidth * 0.75F);
                }
                previous = point;
            }
        }
    }

    public static void landingCross(Vec3 landing, int color, float lineWidth) {
        double half = 0.35D;
        Gizmos.line(
                landing.add(-half, 0.02D, 0.0D),
                landing.add(half, 0.02D, 0.0D),
                color,
                lineWidth
        );
        Gizmos.line(
                landing.add(0.0D, 0.02D, -half),
                landing.add(0.0D, 0.02D, half),
                color,
                lineWidth
        );
        Gizmos.line(landing, landing.add(0.0D, 2.0D, 0.0D), color, lineWidth * 0.75F);
        Gizmos.point(landing.add(0.0D, 0.05D, 0.0D), color, 5.0F);
    }
}
