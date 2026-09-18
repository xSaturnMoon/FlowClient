package com.flowclient.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class WorldProjection {
    private WorldProjection() {}

    public static Vec3 projectPointToScreen(Minecraft client, Vec3 worldPos) {
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f relative = new Vector3f(
                (float) (worldPos.x - camPos.x),
                (float) (worldPos.y - camPos.y),
                (float) (worldPos.z - camPos.z)
        );

        relative.rotate(camera.rotation());
        if (relative.z() >= 0.0F) {
            return new Vec3(0.0D, 0.0D, -1.0D);
        }

        float fov = client.options.fov().get();
        float halfHeight = client.getWindow().getScreenHeight() / 2.0F;
        float scale = halfHeight / (relative.z() * (float) Math.tan(Math.toRadians(fov / 2.0F)));

        float screenX = relative.x() * scale + client.getWindow().getScreenWidth() / 2.0F;
        float screenY = -relative.y() * scale + client.getWindow().getScreenHeight() / 2.0F;

        double ndcX = (screenX / client.getWindow().getScreenWidth()) * 2.0D - 1.0D;
        double ndcY = 1.0D - (screenY / client.getWindow().getScreenHeight()) * 2.0D;
        double depth = -relative.z() / 1000.0D;
        return new Vec3(ndcX, ndcY, depth);
    }
}
