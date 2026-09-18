package com.flowclient.mods.jei;

import com.flowclient.mods.FlowModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JeiMod {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-jei");
    private static boolean enabled = false;

    private JeiMod() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value, boolean persist) {
        enabled = value;
        LOGGER.info("[flowclient-jei] JEI {}, flag={} (persist={}, config={})",
                value ? "enabled" : "disabled",
                value,
                persist,
                FlowModConfig.configPath());
        JeiRuntimeController.applyEnabled(value);
        if (persist) {
            FlowModConfig.save();
            FlowModConfig.flushSave();
            LOGGER.info("[flowclient-jei] persisted jei={} to {}", enabled, FlowModConfig.configPath());
        }
    }

    public static void toggle() {
        setEnabled(!enabled, true);
    }
}
