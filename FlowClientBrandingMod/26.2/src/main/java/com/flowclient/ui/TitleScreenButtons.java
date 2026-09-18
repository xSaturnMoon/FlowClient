package com.flowclient.ui;

import com.flowclient.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class TitleScreenButtons {
    private TitleScreenButtons() {
    }

    public static void replaceSingleplayer(TitleScreen screen) {
        ScreenAccessor accessor = (ScreenAccessor) screen;

        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof Button button) || !isSingleplayer(button)) {
                continue;
            }

            int width = button.getWidth();
            int height = Math.max(button.getHeight(), FlowSidebarButton.HEIGHT);
            int x = button.getX();
            int y = button.getY() - (height - button.getHeight()) / 2;

            accessor.flowclient$removeWidget(button);
            accessor.flowclient$addRenderableWidget(new FlowSidebarButton(
                    x,
                    y,
                    width,
                    height,
                    button.getMessage(),
                    () -> Minecraft.getInstance().gui.setScreen(new SelectWorldScreen(screen))
            ));
            return;
        }
    }

    public static boolean isSingleplayer(Button button) {
        Component message = button.getMessage();
        if (message.getContents() instanceof TranslatableContents contents) {
            return "menu.singleplayer".equals(contents.getKey());
        }
        return false;
    }
}
