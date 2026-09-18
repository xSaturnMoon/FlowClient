package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientBadgeRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererMixin {

    @Inject(method = "prepareGroup", at = @At("HEAD"))
    private void flowclient$captureSeeThroughPass(
            FeatureFrameContext context,
            List<?> submits,
            boolean seeThrough,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof NameTagFeatureRenderer) {
            ClientBadgeRenderer.setSeeThroughPass(seeThrough);
        }
    }

    @Inject(method = "finishExecute", at = @At("RETURN"))
    private void flowclient$clearPendingBadges(FeatureFrameContext context, CallbackInfo ci) {
        if ((Object) this instanceof NameTagFeatureRenderer) {
            ClientBadgeRenderer.clearPendingBadges();
        }
    }
}
