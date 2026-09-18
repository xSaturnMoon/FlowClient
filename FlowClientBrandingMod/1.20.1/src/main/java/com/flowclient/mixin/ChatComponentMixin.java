package com.flowclient.mixin;

import com.flowclient.mods.chat.ChatTweaksMod;
import com.flowclient.mods.chat.ChatTweaksSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component flowclient$prependTimestamp(Component message) {
        if (!ChatTweaksMod.isEnabled() || !ChatTweaksSettings.get().timestamps()) {
            return message;
        }
        return Component.literal("[" + LocalTime.now().format(TIME_FORMAT) + "] ").append(message);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
            )
    )
    private void flowclient$hideChatBackground(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (ChatTweaksMod.isEnabled() && ChatTweaksSettings.get().hideBackground()) {
            return;
        }
        graphics.fill(x1, y1, x2, y2, color);
    }
}
