package com.flowclient.compat;

import net.minecraft.resources.ResourceLocation;

public final class FlowIds {
    private FlowIds() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation("flowclient", path);
    }

    public static ResourceLocation id(String ns, String path) {
        return new ResourceLocation(ns, path);
    }
}
