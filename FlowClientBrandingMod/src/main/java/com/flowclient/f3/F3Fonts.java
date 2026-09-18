package com.flowclient.f3;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class F3Fonts {
    public static final Identifier F3 = Identifier.fromNamespaceAndPath("flowclient", "f3");
    private static final FontDescription F3_FONT = new FontDescription.Resource(F3);
    private static final Style F3_STYLE = Style.EMPTY.withFont(F3_FONT);

    private F3Fonts() {
    }

    public static Component text(String value) {
        return Component.literal(value).withStyle(F3_STYLE);
    }
}
