package com.flowclient.mods.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Set;

public final class JeiChatFilter {
    private static final Set<String> SERVER_RECIPE_SYNC_KEYS = Set.of(
            "jei.message.server.recipe.sync.unavailable",
            "jei.message.server.recipe.sync.vanilla",
            "jei.message.server.recipe.sync.jei.missing",
            "jei.message.server.recipe.sync.error"
    );

    private JeiChatFilter() {
    }

    public static boolean isServerRecipeSyncWarning(Component message) {
        String key = getTranslationKey(message);
        return key != null && (SERVER_RECIPE_SYNC_KEYS.contains(key) || key.startsWith("jei.message.server.recipe.sync"));
    }

    public static boolean shouldSuppress(Component message) {
        return JeiDisableGuard.shouldBlock() || isServerRecipeSyncWarning(message);
    }

    public static boolean shouldSuppress(String translationKey) {
        return JeiDisableGuard.shouldBlock()
                || translationKey != null && (SERVER_RECIPE_SYNC_KEYS.contains(translationKey)
                || translationKey.startsWith("jei.message.server.recipe.sync"));
    }

    private static String getTranslationKey(Component message) {
        if (message.getContents() instanceof TranslatableContents contents) {
            return contents.getKey();
        }
        return null;
    }
}
