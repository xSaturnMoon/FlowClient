package com.flowclient.mods;

import com.flowclient.mods.jei.JeiMod;
import com.flowclient.mods.voicechat.VoiceChatMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hides embedded-mod keybind categories from Options → Controls when the mod toggle is OFF.
 */
public final class ModKeybindCategoryFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-keybinds");

    /** Verified in jei.jar: InternalKeyMappings$CategoryBuilderFactory uses namespace "jei". */
    public static final String JEI_NAMESPACE = "jei";

    /** Verified in voicechat.jar: KeyEvents.registerKeyBinds uses Identifier("voicechat", "voicechat"). */
    public static final String VOICECHAT_NAMESPACE = "voicechat";

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
        LOGGER.info("[flowclient-keybinds] SVC key category uses namespace '{}' (voicechat:voicechat)",
                VOICECHAT_NAMESPACE);
    }

    public static boolean shouldHideKeyMapping(KeyMapping mapping) {
        return shouldHideCategory(mapping.getCategory());
    }

    public static boolean shouldHideCategory(KeyMapping.Category category) {
        Identifier id = category.id();
        String namespace = id.getNamespace();

        if (JEI_NAMESPACE.equals(namespace) && !JeiMod.isEnabled()) {
            return true;
        }
        if (VOICECHAT_NAMESPACE.equals(namespace) && !VoiceChatMod.isEnabled()) {
            return true;
        }
        return false;
    }
}
