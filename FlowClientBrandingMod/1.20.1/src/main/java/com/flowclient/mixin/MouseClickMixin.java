package com.flowclient.mixin;

import com.flowclient.mods.keystrokes.CpsTracker;
import com.flowclient.mods.keystrokes.KeystrokesMod;
import com.flowclient.mods.quiet.FlowQuietController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseClickMixin {

    @Inject(method = "onPress", at = @At("HEAD"))
    private void flowclient$trackClicks(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action != 0) {
            FlowQuietController.notifyInput();
        }
        if (!KeystrokesMod.isEnabled()) {
            return;
        }
        // action 1 = press, 0 = release
        if (action != 1) {
            return;
        }

        if (button == 0) {
            CpsTracker.recordLeft();
        } else if (button == 1) {
            CpsTracker.recordRight();
        }
    }
}
