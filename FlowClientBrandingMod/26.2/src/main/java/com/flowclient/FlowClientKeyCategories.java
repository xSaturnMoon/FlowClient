package com.flowclient;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class FlowClientKeyCategories {
    public static final KeyMapping.Category FLOWCLIENT = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FlowClientMod.MOD_ID, "flowclient")
    );

    private FlowClientKeyCategories() {
    }
}
