package com.flowclient.modpanel;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

public final class ModSettingRow<T> {
    private final String title;
    private final Function<T, String> valueText;
    private final Consumer<T> onClick;
    private final Consumer<T> resetAction;
    private final boolean header;

    private ModSettingRow(
            String title,
            Function<T, String> valueText,
            Consumer<T> onClick,
            Consumer<T> resetAction,
            boolean header
    ) {
        this.title = title;
        this.valueText = valueText;
        this.onClick = onClick;
        this.resetAction = resetAction;
        this.header = header;
    }

    public static <T> ModSettingRow<T> header(String title) {
        return new ModSettingRow<>(title, null, null, null, true);
    }

    public static <T> ModSettingRow<T> of(
            String title,
            Function<T, String> valueText,
            Consumer<T> onClick,
            Consumer<T> resetAction
    ) {
        return new ModSettingRow<>(title, valueText, onClick, resetAction, false);
    }

    public boolean header() {
        return this.header;
    }

    public String title() {
        return this.title;
    }

    public Component label(T settings) {
        return Component.literal(this.title + ": " + this.valueText.apply(settings));
    }

    public void click(T settings) {
        if (this.onClick != null) {
            this.onClick.accept(settings);
        }
    }

    public void reset(T settings) {
        if (this.resetAction != null) {
            this.resetAction.accept(settings);
        }
    }
}
