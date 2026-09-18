package com.flowclient.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class UiFonts {
    public static final ResourceLocation UI = new ResourceLocation("flowclient", "ui");
    private static final Style UI_STYLE = Style.EMPTY.withFont(UI);

    private UiFonts() {
    }

    public static Component text(String value) {
        return Component.literal(value).withStyle(UI_STYLE);
    }

    public static Component text(Component value) {
        return value.copy().withStyle(UI_STYLE);
    }
}
