package com.flowclient.mixin;

import com.flowclient.modpanel.ModPanelKeys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void flowclient$openModPanelOnRightShift(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (ModPanelKeys.isOpenKey(event.key(), action)) {
            ModPanelKeys.onRightShiftPressed();
        }
    }
}
