package com.flowclient.modpanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ModIcons {
    public static final Identifier JEI = icon("jei");
    public static final Identifier VOICE_CHAT = icon("voice_chat");
    public static final Identifier FLOW_CLOCK = icon("flow_clock");
    public static final Identifier FLOW_TAB = icon("flow_tab");
    public static final Identifier OTHER_CLIENT = icon("other_client");

    private static final int ICON_SIZE = 16;

    private ModIcons() {
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("flowclient", "textures/modicons/" + name);
    }

    public static void draw(GuiGraphicsExtractor graphics, ModEntry entry, int x, int y) {
        Identifier customIcon = entry.customIcon();
        if (customIcon != null && hasTexture(customIcon)) {
            graphics.blit(customIcon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
            return;
        }

        graphics.item(new ItemStack(entry.iconItem()), x, y, ICON_SIZE);
    }

    private static boolean hasTexture(Identifier id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
