package com.flowclient.mods.freelook;

import com.flowclient.mods.FreelookMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreelookController {
    private static boolean keyHeld;
    private static boolean engaged;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float bodyYaw;
    private static float bodyPitch;
    private static float bodyHeadYaw;
    private static CameraType savedCameraType;
    private static boolean forcedPerspective;

    private FreelookController() {}

    public static void updateKeyState(boolean held, boolean allowed) {
        if (!FreelookMod.isEnabled() || !allowed) {
            if (!held) {
                disengage();
            }
            keyHeld = false;
            return;
        }

        if (held && !keyHeld) {
            begin();
        } else if (!held && keyHeld) {
            disengage();
        }
        keyHeld = held;
    }

    public static boolean applyTurnDelta(double yawDelta, double pitchDelta) {
        if (!FreelookMod.isEnabled() || !engaged) {
            return false;
        }

        cameraYaw += (float) (yawDelta * 0.5);
        cameraPitch = Mth.clamp(cameraPitch + (float) (pitchDelta * 0.5), -90.0f, 90.0f);
        return true;
    }

    public static void maintainBodyOrientation() {
        if (!engaged) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.setYRot(bodyYaw);
        client.player.setXRot(bodyPitch);
        client.player.setYHeadRot(bodyHeadYaw);
        client.player.yRotO = bodyYaw;
        client.player.xRotO = bodyPitch;
    }

    public static boolean isEngaged() {
        return engaged;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    private static void begin() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        engaged = true;
        bodyYaw = client.player.getYRot();
        bodyPitch = client.player.getXRot();
        bodyHeadYaw = client.player.getYHeadRot();

        var camera = client.gameRenderer.getMainCamera();
        cameraYaw = camera.getYRot();
        cameraPitch = camera.getXRot();

        savedCameraType = client.options.getCameraType();
        if (savedCameraType == CameraType.FIRST_PERSON) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            forcedPerspective = true;
        } else {
            forcedPerspective = false;
        }
    }

    private static void disengage() {
        if (!engaged) {
            return;
        }
        engaged = false;

        Minecraft client = Minecraft.getInstance();
        if (forcedPerspective && savedCameraType != null) {
            client.options.setCameraType(savedCameraType);
        }
    }
}
