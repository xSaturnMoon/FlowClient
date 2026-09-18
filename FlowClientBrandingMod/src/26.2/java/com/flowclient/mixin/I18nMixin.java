package com.flowclient.mixin;

import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(I18n.class)
public class I18nMixin {
    @Inject(method = "get(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private static void flowclient$onI18nGet(String key, Object[] args, CallbackInfoReturnable<String> cir) {
        if (key != null && key.startsWith("modmenu.mods.")) {
            cir.setReturnValue(cir.getReturnValue() + " (FlowClient)");
        } else if (key != null && key.equals("menu.modded")) {
            // Vanilla fallback if ModMenu is not formatting it
            cir.setReturnValue(cir.getReturnValue() + " (FlowClient)");
        }
    }
}
