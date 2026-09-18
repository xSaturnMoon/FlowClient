package com.flowclient.mods.otherclient;

import com.flowclient.FlowClientMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class FlowEmblemTextures {
    private FlowEmblemTextures() {}

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(FlowClientMod.MOD_ID, "flow_emblem_texture");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                load();
            }
        });
    }

    public static void load() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        TextureManager textures = client.getTextureManager();
        textures.register(FlowEmblem.TEXTURE, new SimpleTexture(FlowEmblem.TEXTURE));
    }

    public static void ensureLoaded() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        TextureManager textures = client.getTextureManager();
        if (textures.getTexture(FlowEmblem.TEXTURE, null) == null) {
            load();
        }
    }
}
