package com.flowclient.mixin;

import com.flowclient.mods.chat.ChatTweaksMod;
import com.flowclient.mods.chat.ChatTweaksSettings;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
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
            method = "addPlayerMessage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component flowclient$prependTimestamp(Component message) {
        if (!ChatTweaksMod.isEnabled() || !ChatTweaksSettings.get().timestamps()) {
            return message;
        }
        return Component.literal("[" + LocalTime.now().format(TIME_FORMAT) + "] ").append(message);
    }

    @Redirect(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$DisplayMode;foreground:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean flowclient$hideChatBackground(ChatComponent.DisplayMode mode) {
        if (ChatTweaksMod.isEnabled() && ChatTweaksSettings.get().hideBackground()) {
            return true;
        }
        return mode.foreground;
    }
}
