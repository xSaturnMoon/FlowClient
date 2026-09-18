package com.flowclient.mixin;

import com.flowclient.modpanel.ModPanelKeys;
import com.flowclient.mods.quiet.FlowQuietController;
import com.flowclient.mods.schematics.AllSchematicsKeys;
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
        if (action != 0) {
            FlowQuietController.notifyInput();
        }
        if (ModPanelKeys.isOpenKey(event.key(), action)) {
            ModPanelKeys.onRightShiftPressed();
        }
        if (AllSchematicsKeys.isMenuKey(event, action)) {
            AllSchematicsKeys.onMenuKeyPressed();
        }
    }
}
