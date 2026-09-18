package com.flowclient.mods.otherclient;

import java.util.Comparator;

/**
 * Clients that OtherClient can surface for a player.
 * {@link #priority} is used when multiple signals match.
 */
public enum DetectedClient {
    LOADING   ("...",           0xAAAAAA, "...", 0,   "loading"),
    FLOWCLIENT("FlowClient",    0x00D4FF, "\uE000",  100, "flowclient"),
    OPTIFINE  ("OptiFine",      0xFF8F00, "\uE001",  50,  "optifine"),
    BEDROCK   ("Bedrock",       0x77B255, "\uE003",  45,  "bedrock"),
    TLAUNCHER ("TLauncher",     0xE67E22, "\uE004",  42,  "tlauncher"),
    VANILLA   ("Vanilla",       0x78909C, "VA",  1,   "vanilla");

    public final String displayName;
    public final int    color;
    public final String badge;
    public final int    priority;
    public final String clientId;

    DetectedClient(String displayName, int color, String badge, int priority, String clientId) {
        this.displayName = displayName;
        this.color       = color;
        this.badge       = badge;
        this.priority    = priority;
        this.clientId    = clientId;
    }

    public static DetectedClient fromClientId(String id) {
        if (id == null || id.isBlank()) return VANILLA;
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        for (DetectedClient client : values()) {
            if (client.clientId.equals(normalized)) return client;
        }
        return VANILLA;
    }

    public static final Comparator<DetectedClient> BY_PRIORITY =
            Comparator.comparingInt((DetectedClient c) -> c.priority).reversed();

    /** True for actual Minecraft clients/mod loaders shown in tab/nametag badges. */
    public boolean isGameClient() {
        return this == FLOWCLIENT || this == OPTIFINE || this == BEDROCK || this == TLAUNCHER;
    }
}
