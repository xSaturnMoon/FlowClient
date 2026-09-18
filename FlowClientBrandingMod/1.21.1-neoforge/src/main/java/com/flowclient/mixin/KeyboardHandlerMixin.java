package com.flowclient.mixin;

import com.flowclient.modpanel.ModPanelKeys;
import com.flowclient.mods.schematics.AllSchematicsKeys;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void flowclient$openModPanelOnRightShift(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (ModPanelKeys.isOpenKey(key, action)) {
            ModPanelKeys.onRightShiftPressed();
        }
        if (AllSchematicsKeys.isMenuKey(key, action)) {
            AllSchematicsKeys.onMenuKeyPressed();
        }
    }
}
