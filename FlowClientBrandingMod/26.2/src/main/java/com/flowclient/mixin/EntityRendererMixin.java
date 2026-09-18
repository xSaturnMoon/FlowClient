package com.flowclient.mixin;

import com.flowclient.mods.otherclient.ClientBadgeHelper;
import com.flowclient.mods.otherclient.ClientBadgeRenderer;
import com.flowclient.mods.otherclient.ClientDetector;
import com.flowclient.mods.otherclient.DetectedClient;
import com.flowclient.mods.otherclient.EntityRenderStateExt;
import com.flowclient.mods.otherclient.OtherClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;nameTag:Lnet/minecraft/network/chat/Component;",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void flowclient$attachClientBadge(T entity, S state, float tickProgress, double maxDistance, double maxScoreDistance, CallbackInfo ci) {
        if (!OtherClientMod.isEnabled() || !(entity instanceof Player player) || state.nameTag == null) {
            return;
        }

        DetectedClient primary = resolveFlowBadge(player);
        if (primary != null) {
            net.minecraft.network.chat.Component badgeComponent;
            if (primary.isGameClient()) {
                badgeComponent = net.minecraft.network.chat.Component.empty()
                        .append(net.minecraft.network.chat.Component.literal(primary.badge).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("flowclient", "badge")))))
                        .append(net.minecraft.network.chat.Component.literal(" "));
            } else {
                badgeComponent = net.minecraft.network.chat.Component.literal("[" + primary.badge + "] ")
                        .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(primary.color)));
            }
            state.nameTag = net.minecraft.network.chat.Component.empty().append(badgeComponent).append(state.nameTag);
        }
    }

    private static DetectedClient resolveFlowBadge(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
            return DetectedClient.FLOWCLIENT;
        }

        ClientBadgeHelper.triggerDetection(player);
        DetectedClient primary = ClientDetector.getPrimary(player.getUUID());
        return primary.isGameClient() ? primary : null;
    }
}
