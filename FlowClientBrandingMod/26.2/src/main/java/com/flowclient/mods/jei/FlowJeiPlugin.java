package com.flowclient.mods.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRuntimeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-jei");

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("flowclient", "jei_integration");
    }

    @Override
    public void registerRuntime(IRuntimeRegistration registration) {
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        LOGGER.info("[flowclient-jei] JEI runtime available, syncing Flow toggle (enabled={})", JeiMod.isEnabled());
        JeiRuntimeController.syncFromFlowConfig();
    }

    @Override
    public void onRuntimeUnavailable() {
        LOGGER.info("[flowclient-jei] JEI runtime unavailable");
    }
}
