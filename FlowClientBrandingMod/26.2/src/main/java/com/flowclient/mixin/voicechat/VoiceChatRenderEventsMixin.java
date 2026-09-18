package com.flowclient.mixin.voicechat;

import com.flowclient.mods.voicechat.VoiceChatDisableGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.voicechat.voice.client.RenderEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderEvents.class, remap = false)
public abstract class VoiceChatRenderEventsMixin {
    @Inject(method = "onRenderHUD", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockHud(GuiGraphicsExtractor graphics, float partialTicks, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }

    @Inject(method = "onRenderName", at = @At("HEAD"), cancellable = true, remap = false)
    private void flowclient$blockNameplate(EntityRenderState state, CameraRenderState cameraState, PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        VoiceChatDisableGuard.cancelVoid(ci);
    }
}
