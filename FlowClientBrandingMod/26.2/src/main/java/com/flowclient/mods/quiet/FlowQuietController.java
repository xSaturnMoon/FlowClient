package com.flowclient.mods.quiet;

import net.minecraft.client.Minecraft;

public final class FlowQuietController {
    private static final int TICKS_PER_SECOND = 20;

    private static int idleTicks;
    private static boolean quietActive;
    private static boolean quietFromUnfocus;
    private static boolean quietFromAfk;
    private static int appliedBackgroundFps = -1;

    private FlowQuietController() {
    }

    public static boolean isQuietActive() {
        return quietActive;
    }

    public static boolean isAfkActive() {
        if (!FlowQuietMod.isEnabled()) {
            return false;
        }

        FlowQuietSettings settings = FlowQuietSettings.get();
        if (!settings.isAfkEnabled()) {
            return false;
        }

        return idleTicks >= settings.getAfkTimeoutSeconds() * TICKS_PER_SECOND;
    }

    public static boolean isAfkQuiet() {
        return quietActive && quietFromAfk;
    }

    public static long getIdleMillis() {
        return idleTicks * 50L;
    }

    public static void notifyInput() {
        idleTicks = 0;
    }

    public static void onClientStarted(Minecraft client) {
        if (!FlowQuietMod.isEnabled()) {
            resetQuietState(client);
            return;
        }

        repairPersistedMaxFps(client);
    }

    private static void resetQuietState(Minecraft client) {
        quietActive = false;
        quietFromUnfocus = false;
        quietFromAfk = false;
        appliedBackgroundFps = -1;
        idleTicks = 0;
        FlowQuietAudio.forceUnmute(client);
        restoreStuckFps(client);
    }

    private static void restoreStuckFps(Minecraft client) {
        if (client == null || client.options == null) {
            return;
        }

        FlowQuietSettings settings = FlowQuietSettings.get();
        int current = client.options.framerateLimit().get();
        int preserved = settings.getPreservedMaxFps();
        if (current <= settings.getBackgroundFps() && preserved > current) {
            client.options.framerateLimit().set(preserved);
        }
    }

    public static void tick(Minecraft client) {
        if (client == null || !FlowQuietMod.isEnabled()) {
            if (quietActive) {
                deactivate(client);
            }
            return;
        }

        FlowQuietSettings settings = FlowQuietSettings.get();

        if (!quietActive) {
            rememberCurrentMaxFps(client, settings);
        }

        trackMovementInput(client);

        if (idleTicks < Integer.MAX_VALUE - 1) {
            idleTicks++;
        }

        boolean unfocusQuiet = settings.isUnfocusedEnabled() && !client.getWindow().isFocused();
        boolean afkQuiet = settings.isAfkEnabled() && isAfkActive();
        boolean shouldQuiet = unfocusQuiet || afkQuiet;

        if (shouldQuiet && !quietActive) {
            quietFromUnfocus = unfocusQuiet;
            quietFromAfk = afkQuiet;
            activate(client, settings);
        } else if (!shouldQuiet && quietActive) {
            deactivate(client);
        } else if (quietActive) {
            quietFromUnfocus = unfocusQuiet;
            quietFromAfk = afkQuiet;
            ensureBackgroundFps(client, settings);
        }
    }

    public static void forceDeactivate() {
        deactivate(Minecraft.getInstance());
    }

    public static void prepareOptionsSave() {
        if (!quietActive) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        int restore = FlowQuietSettings.get().getPreservedMaxFps();
        if (restore > FlowQuietSettings.get().getBackgroundFps()) {
            client.options.framerateLimit().set(restore);
        }
    }

    public static void restoreQuietFpsIfNeeded() {
        if (!quietActive) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        ensureBackgroundFps(client, FlowQuietSettings.get());
    }

    private static void trackMovementInput(Minecraft client) {
        if (client.options == null) {
            return;
        }

        var options = client.options;
        if (options.keyUp.isDown()
                || options.keyDown.isDown()
                || options.keyLeft.isDown()
                || options.keyRight.isDown()
                || options.keyJump.isDown()
                || options.keyShift.isDown()
                || options.keySprint.isDown()
                || options.keyAttack.isDown()
                || options.keyUse.isDown()) {
            notifyInput();
        }
    }

    private static void rememberCurrentMaxFps(Minecraft client, FlowQuietSettings settings) {
        if (client == null || client.options == null) {
            return;
        }

        int current = client.options.framerateLimit().get();
        if (current > settings.getBackgroundFps()) {
            settings.rememberMaxFps(current);
        }
    }

    private static void repairPersistedMaxFps(Minecraft client) {
        if (client == null || client.options == null || quietActive || !FlowQuietMod.isEnabled()) {
            return;
        }

        FlowQuietSettings settings = FlowQuietSettings.get();
        int current = client.options.framerateLimit().get();
        int preserved = settings.getPreservedMaxFps();
        if (current == settings.getBackgroundFps() && preserved > current) {
            client.options.framerateLimit().set(preserved);
        }
    }

    private static void activate(Minecraft client, FlowQuietSettings settings) {
        if (client == null || client.options == null || quietActive) {
            return;
        }

        rememberCurrentMaxFps(client, settings);
        quietActive = true;
        ensureBackgroundFps(client, settings);
        FlowQuietAudio.mute(client);
    }

    private static void ensureBackgroundFps(Minecraft client, FlowQuietSettings settings) {
        int target = settings.getBackgroundFps();
        if (appliedBackgroundFps == target && client.options.framerateLimit().get() == target) {
            return;
        }

        client.options.framerateLimit().set(target);
        appliedBackgroundFps = target;
    }

    private static void deactivate(Minecraft client) {
        if (!quietActive) {
            return;
        }

        quietActive = false;
        quietFromUnfocus = false;
        quietFromAfk = false;
        appliedBackgroundFps = -1;

        if (client != null && client.options != null) {
            int restore = FlowQuietSettings.get().getPreservedMaxFps();
            client.options.framerateLimit().set(restore);
            FlowQuietSettings.get().rememberMaxFps(restore);
            FlowQuietSettings.get().persist();
            FlowQuietAudio.unmute(client);
        } else {
            FlowQuietAudio.forceUnmute(null);
        }
    }
}
