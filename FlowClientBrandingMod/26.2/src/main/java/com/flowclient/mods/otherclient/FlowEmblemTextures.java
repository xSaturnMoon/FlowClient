package com.flowclient.mods.otherclient;

import com.flowclient.FlowClientMod;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class FlowEmblemTextures {
    private FlowEmblemTextures() {}

    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(FlowClientMod.MOD_ID, "flow_emblem_texture"),
                new SimpleReloadListener<Void>() {
                    @Override
                    protected Void prepare(PreparableReloadListener.SharedState state) {
                        return null;
                    }

                    @Override
                    protected void apply(Void unused, PreparableReloadListener.SharedState state) {
                        load();
                    }
                }
        );
    }

    public static void load() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        TextureManager textures = client.getTextureManager();
        textures.registerForNextReload(FlowEmblem.TEXTURE);
        textures.registerAndLoad(FlowEmblem.TEXTURE, new SimpleTexture(FlowEmblem.TEXTURE));
    }

    public static void ensureLoaded() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        TextureManager textures = client.getTextureManager();
        if (textures.getTexture(FlowEmblem.TEXTURE) == textures.getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE)) {
            load();
        }
    }
}
