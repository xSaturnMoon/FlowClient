package com.flowclient.mods.freelook;

import com.flowclient.mods.FreelookMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreelookController {
    // --- state ---
    private static boolean keyHeld;
    private static boolean engaged;

    // The camera angles that freelook controls
    private static float cameraYaw;
    private static float cameraPitch;

    // The player body angles saved when freelook begins
    private static float bodyYaw;
    private static float bodyPitch;
    private static float bodyHeadYaw;

    // The perspective setting before freelook
    private static CameraType savedCameraType;
    // Whether we forced a perspective change
    private static boolean forcedPerspective;

    private FreelookController() {
    }

    // ---------------------------------------------------------------
    // Called every tick from FreelookKeys
    // ---------------------------------------------------------------
    public static void updateKeyState(boolean held, boolean allowed) {
        if (!FreelookMod.isEnabled() || !allowed) {
            if (!held) disengage();
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

    // ---------------------------------------------------------------
    // Called from MouseHandlerMixin with the SCALED mouse deltas
    // (same values that would normally be fed into player.turn())
    // Returns true if freelook consumed the delta (skip normal turn)
    // ---------------------------------------------------------------
    public static boolean applyTurnDelta(double yawDelta, double pitchDelta) {
        if (!FreelookMod.isEnabled() || !engaged) {
            return false;
        }

        // The deltas arriving here are ALREADY multiplied by Minecraft's
        // mouse sensitivity (same path as normal player turning).
        // Apply 0.5x factor: in third-person the camera feels twice as fast
        // visually (camera is decoupled from the body), so we halve the delta
        // to match the same feel as first-person turning — identical to Lunar.
        cameraYaw  += (float) (yawDelta * 0.5);
        cameraPitch = Mth.clamp(cameraPitch + (float) (pitchDelta * 0.5), -90.0f, 90.0f);
        return true;
    }

    // ---------------------------------------------------------------
    // Called every tick from LocalPlayerMixin to lock the body
    // ---------------------------------------------------------------
    public static void maintainBodyOrientation() {
        if (!engaged) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.setYRot(bodyYaw);
        mc.player.setXRot(bodyPitch);
        mc.player.setYHeadRot(bodyHeadYaw);
        mc.player.yRotO = bodyYaw;
        mc.player.xRotO = bodyPitch;
    }

    // ---------------------------------------------------------------
    // Getters for mixins
    // ---------------------------------------------------------------
    public static boolean isEngaged()      { return engaged; }
    public static float   getCameraYaw()   { return cameraYaw; }
    public static float   getCameraPitch() { return cameraPitch; }

    // ---------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------
    private static void begin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        engaged = true;

        // Save body orientation
        bodyYaw      = mc.player.getYRot();
        bodyPitch    = mc.player.getXRot();
        bodyHeadYaw  = mc.player.getYHeadRot();

        // Init camera to current camera orientation
        var camera = mc.gameRenderer.mainCamera();
        cameraYaw   = camera.yRot();
        cameraPitch = camera.xRot();

        // Save current perspective and force 3rd-person (back) like Lunar
        savedCameraType = mc.options.getCameraType();
        if (savedCameraType == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            forcedPerspective = true;
        } else {
            forcedPerspective = false;
        }
    }

    private static void disengage() {
        if (!engaged) return;
        engaged = false;

        // Restore original perspective
        Minecraft mc = Minecraft.getInstance();
        if (forcedPerspective && savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
        }
    }
}
