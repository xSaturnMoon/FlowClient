package com.flowclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends "(FlowClient)" to the client brand string shown in F3 and the loading screen.
 * Vanilla Fabric result: "Minecraft (26.2) (118 mods)"
 * Our result:            "Minecraft (26.2) (118 mods) (FlowClient)"
 */
@Mixin(ClientBrandRetriever.class)
public class ClientBrandMixin {
    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true)
    private static void flowclient$appendBrand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " (FlowClient)");
    }
}
