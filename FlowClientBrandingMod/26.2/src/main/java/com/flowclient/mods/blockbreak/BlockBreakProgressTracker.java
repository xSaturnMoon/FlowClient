package com.flowclient.mods.blockbreak;

import com.flowclient.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public final class BlockBreakProgressTracker {
    private BlockBreakProgressTracker() {
    }

    public static float progress(Minecraft mc) {
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (gameMode == null) {
            return 0.0f;
        }

        MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) gameMode;
        if (!accessor.flowclient$isDestroying()) {
            return 0.0f;
        }

        return Math.max(0.0f, Math.min(1.0f, accessor.flowclient$getDestroyProgress()));
    }
}
