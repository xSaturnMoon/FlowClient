package com.flowclient.mixin;



import com.flowclient.mods.otherclient.ClientBadgeRenderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollection;

import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.minecraft.network.chat.Component;

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(SubmitNodeCollection.class)

public abstract class SubmitNodeCollectionMixin {



    @Unique

    private Component flowclient$submittingNameTag;



    @Inject(

            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",

            at = @At("HEAD")

    )

    private void flowclient$captureSubmittingNameTag(

            PoseStack poseStack,

            Vec3 attachment,

            int yOffset,

            Component text,

            boolean discrete,

            int lightCoords,

            CameraRenderState camera,

            CallbackInfo ci

    ) {

        this.flowclient$submittingNameTag = text;

    }



    @ModifyVariable(

            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",

            at = @At(value = "STORE"),

            ordinal = 0

    )

    private float flowclient$offsetNametagForBadge(float textX) {

        return textX + ClientBadgeRenderer.badgeTextOffset(this.flowclient$submittingNameTag);

    }

}

