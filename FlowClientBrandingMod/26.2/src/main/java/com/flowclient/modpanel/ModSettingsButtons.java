package com.flowclient.modpanel;

public final class ModSettingsButtons {
    private ModSettingsButtons() {
    }

    public static ModSettingsIconButton create(int x, int y, Runnable onPress) {
        return new ModSettingsIconButton(x, y, onPress);
    }
}
