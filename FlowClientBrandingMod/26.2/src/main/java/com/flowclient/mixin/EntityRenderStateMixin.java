package com.flowclient.mixin;

import com.flowclient.mods.otherclient.DetectedClient;
import com.flowclient.mods.otherclient.EntityRenderStateExt;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateExt {
    @Unique
    private DetectedClient flowclient$clientBadge;

    @Override
    public DetectedClient flowclient$getClientBadge() {
        return this.flowclient$clientBadge;
    }

    @Override
    public void flowclient$setClientBadge(DetectedClient badge) {
        this.flowclient$clientBadge = badge;
    }
}
