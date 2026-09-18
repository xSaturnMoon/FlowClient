package com.flowclient.mods;

import com.flowclient.mods.jei.JeiMod;
import com.flowclient.mods.voicechat.VoiceChatMod;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Hides embedded-mod keybind categories from Options → Controls when the mod toggle is OFF.
 */
public final class ModKeybindCategoryFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-keybinds");

    /** Verified in jei.jar: InternalKeyMappings$CategoryBuilderFactory uses namespace "jei". */
    public static final String JEI_NAMESPACE = "jei";

    /** Verified in voicechat.jar: KeyEvents.registerKeyBinds uses category "key.categories.voicechat". */
    public static final String VOICECHAT_CATEGORY = "key.categories.voicechat";

    private static boolean loggedCategories;

    private ModKeybindCategoryFilter() {
    }

    public static void logKnownCategoriesOnce() {
        if (loggedCategories) {
            return;
        }
        loggedCategories = true;
        LOGGER.info("[flowclient-keybinds] JEI key categories use namespace '{}' (e.g. jei:overlays, jei:search, jei:recipe.gui)",
                JEI_NAMESPACE);
        LOGGER.info("[flowclient-keybinds] SVC key category uses '{}'", VOICECHAT_CATEGORY);
    }

    public static boolean shouldHideKeyMapping(KeyMapping mapping) {
        return shouldHideCategory(mapping.getCategory());
    }

    public static boolean shouldHideCategory(String category) {
        if (category == null) {
            return false;
        }

        String lower = category.toLowerCase(Locale.ROOT);
        if (lower.contains(JEI_NAMESPACE) && !JeiMod.isEnabled()) {
            return true;
        }
        return (VOICECHAT_CATEGORY.equals(category) || lower.contains("voicechat")) && !VoiceChatMod.isEnabled();
    }
}
