package com.flowclient.modpanel;

import com.flowclient.compat.FlowGfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ModIcons {
    public static final ResourceLocation JEI = icon("jei");
    public static final ResourceLocation VOICE_CHAT = icon("voice_chat");
    public static final ResourceLocation FLOW_CLOCK = icon("flow_clock");
    public static final ResourceLocation FLOW_TAB = icon("flow_tab");
    public static final ResourceLocation OTHER_CLIENT = icon("other_client");

    private static final int ICON_SIZE = 16;

    private ModIcons() {
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("flowclient", "textures/modicons/" + name);
    }

    public static void draw(GuiGraphics graphics, ModEntry entry, int x, int y) {
        ResourceLocation customIcon = entry.customIcon();
        if (customIcon != null && hasTexture(customIcon)) {
            FlowGfx.blit(graphics, customIcon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
            return;
        }

        FlowGfx.item(graphics, new ItemStack(entry.iconItem()), x, y, ICON_SIZE);
    }

    private static boolean hasTexture(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
