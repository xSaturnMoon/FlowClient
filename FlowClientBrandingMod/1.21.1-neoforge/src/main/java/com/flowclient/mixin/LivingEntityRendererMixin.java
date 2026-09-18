package com.flowclient.mixin;

import com.flowclient.mods.NametagMod;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
    private void flowclient$forcePlayerNametag(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (NametagMod.shouldForceNametag(entity)) {
            cir.setReturnValue(true);
        }
    }
}
