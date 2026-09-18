package com.flowclient;

import com.flowclient.mods.otherclient.ClientPresenceAPI;
import com.flowclient.mods.otherclient.OptiFineDetector;
import com.flowclient.mods.otherclient.TLauncherDetector;
import com.flowclient.mods.fullbright.FullbrightController;
import com.flowclient.mods.quiet.FlowQuietController;
import com.flowclient.mods.voicechat.VoiceChatRuntimeController;

/**
 * Graceful cleanup when Minecraft shuts down. Do not call Runtime.halt here —
 * it triggers Minecraft's shutdown watchdog and leaves the JVM stuck.
 */
public final class FlowShutdown {
    private static volatile boolean shuttingDown;

    private FlowShutdown() {}

    public static void onClientStopping() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;

        OptiFineDetector.shutdown();
        TLauncherDetector.shutdown();
        ClientPresenceAPI.shutdown();
        VoiceChatRuntimeController.shutdownOnClientStop();
        FlowQuietController.forceDeactivate();
        FullbrightController.onClientStopping();
        FlowSessionFiles.deleteSessionFile();
    }
}
