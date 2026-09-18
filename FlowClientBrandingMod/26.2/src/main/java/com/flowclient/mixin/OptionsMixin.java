package com.flowclient.mixin;

import com.flowclient.mods.quiet.FlowQuietController;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void flowclient$restoreFpsBeforeSave(CallbackInfo ci) {
        FlowQuietController.prepareOptionsSave();
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void flowclient$reapplyQuietFpsAfterSave(CallbackInfo ci) {
        FlowQuietController.restoreQuietFpsIfNeeded();
    }
}
