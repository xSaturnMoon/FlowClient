package com.flowclient.branding.mixin;

import com.flowclient.branding.FlowClientBrandingClient;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void flowclient$appendBranding(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;

        for (var child : screen.children()) {
            if (!(child instanceof StringWidget widget)) {
                continue;
            }

            Component message = widget.getMessage();
            if (message == null) {
                continue;
            }

            String text = message.getString();
            if (!text.contains("Minecraft") || text.contains("FlowClient")) {
                continue;
            }

            widget.setMessage(Component.literal(text + FlowClientBrandingClient.BRANDING_SUFFIX));
        }
    }
}
