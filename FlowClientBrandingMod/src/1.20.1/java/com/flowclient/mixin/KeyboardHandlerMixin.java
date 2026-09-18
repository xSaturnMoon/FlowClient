package com.flowclient.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.flowclient.modpanel.ModPanelKeys;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void flowclient$openModPanelOnRightShift(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (ModPanelKeys.isOpenKey(key, action)) {
            ModPanelKeys.onRightShiftPressed();
        }
    }
}
