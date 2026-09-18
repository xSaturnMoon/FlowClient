package com.flowclient.mods.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;

public final class DebugVisualController {
    private static DebugScreenEntryStatus savedHitboxes = DebugScreenEntryStatus.NEVER;
    private static DebugScreenEntryStatus savedChunkBorders = DebugScreenEntryStatus.NEVER;
    private static boolean hitboxesCaptured;
    private static boolean chunkBordersCaptured;

    private DebugVisualController() {
    }

    public static void tick(Minecraft client) {
        if (client == null) {
            return;
        }

        sync(client, HitboxesMod.isEnabled(), DebugScreenEntries.ENTITY_HITBOXES, true);
        sync(client, ChunkBordersMod.isEnabled(), DebugScreenEntries.CHUNK_BORDERS, false);
    }

    private static void sync(Minecraft client, boolean enabled, Identifier entry, boolean hitboxes) {
        if (enabled) {
            if (hitboxes) {
                if (!hitboxesCaptured) {
                    savedHitboxes = client.debugEntries.getStatus(entry);
                    hitboxesCaptured = true;
                }
            } else if (!chunkBordersCaptured) {
                savedChunkBorders = client.debugEntries.getStatus(entry);
                chunkBordersCaptured = true;
            }
            client.debugEntries.setStatus(entry, DebugScreenEntryStatus.ALWAYS_ON);
            return;
        }

        if (hitboxes) {
            if (hitboxesCaptured) {
                client.debugEntries.setStatus(entry, savedHitboxes);
                hitboxesCaptured = false;
            }
        } else if (chunkBordersCaptured) {
            client.debugEntries.setStatus(entry, savedChunkBorders);
            chunkBordersCaptured = false;
        }
    }
}
