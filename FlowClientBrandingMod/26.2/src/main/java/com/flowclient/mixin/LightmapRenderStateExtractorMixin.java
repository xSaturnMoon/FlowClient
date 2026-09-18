package com.flowclient.mixin;

import com.flowclient.mods.fullbright.FullbrightMod;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void flowclient$applyFullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (!FullbrightMod.isEnabled()) {
            return;
        }

        state.brightness = 12.0F;
        state.blockFactor = 12.0F;
        state.skyFactor = 12.0F;
        state.blockLightTint = LightmapRenderStateExtractor.WHITE;
        state.skyLightColor = LightmapRenderStateExtractor.WHITE;
        state.ambientColor = LightmapRenderStateExtractor.WHITE;
        state.darknessEffectScale = 0.0F;
        state.bossOverlayWorldDarkening = 0.0F;
        state.nightVisionEffectIntensity = 0.0F;
        state.needsUpdate = true;
    }
}
