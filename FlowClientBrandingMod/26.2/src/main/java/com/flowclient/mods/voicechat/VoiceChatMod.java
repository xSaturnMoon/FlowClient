package com.flowclient.mods.voicechat;

import com.flowclient.mods.FlowModConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class VoiceChatMod {
    private static boolean enabled;

    private VoiceChatMod() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        if (persist) {
            FlowModConfig.save();
        }
    }

    public static void toggle() {
        boolean next = !enabled;
        setEnabled(next, true);
        try {
            if (next) {
                VoiceChatJarManager.ensureInstalled();
            }

            if (!VoiceChatRuntimeController.isVoiceChatLoaded()) {
                if (next) {
                    if (VoiceChatJarManager.isInstalledInModsFolder()) {
                        throw new IllegalStateException(
                                "Simple Voice Chat is installed but did not load. Launch this instance from FlowClient and restart Minecraft.");
                    }
                    throw new IllegalStateException(
                            "Simple Voice Chat was installed. Fully close and reopen Minecraft once.");
                }
                VoiceChatNotifier.showToggleResult(next, true, null);
                return;
            }

            VoiceChatRuntimeController.applyEnabled(next);
            VoiceChatNotifier.showToggleResult(next, true, null);
        } catch (Exception ex) {
            setEnabled(!next, true);
            try {
                if (VoiceChatRuntimeController.isVoiceChatLoaded()) {
                    VoiceChatRuntimeController.applyEnabled(!next);
                }
            } catch (Exception ignored) {
            }
            VoiceChatNotifier.showToggleResult(next, false, ex.getMessage());
        }
    }
}
