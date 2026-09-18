package com.flowclient.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class UiFonts {
    public static final Identifier UI = Identifier.fromNamespaceAndPath("flowclient", "ui");
    private static final FontDescription UI_FONT = new FontDescription.Resource(UI);
    private static final Style UI_STYLE = Style.EMPTY.withFont(UI_FONT);

    private UiFonts() {
    }

    public static Component text(String value) {
        return Component.literal(value).withStyle(UI_STYLE);
    }

    public static Component text(Component value) {
        return value.copy().withStyle(UI_STYLE);
    }
}
