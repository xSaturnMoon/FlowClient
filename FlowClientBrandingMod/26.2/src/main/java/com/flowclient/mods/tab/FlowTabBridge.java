package com.flowclient.mods.tab;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;

public interface FlowTabBridge {
    PlayerTabOverlay overlay();

    Minecraft minecraft();

    List<PlayerInfo> players();

    Component header();

    Component footer();

    Component nameForDisplay(PlayerInfo info);

    void drawPingIcon(GuiGraphicsExtractor graphics, int columnWidth, int rowX, int rowY, PlayerInfo info);

    void drawHearts(int x, int y, int columnWidth, UUID playerId, GuiGraphicsExtractor graphics, int rowIndex);

    void drawObjectiveScore(
            Objective objective,
            int x,
            int y,
            int columnWidth,
            PlayerInfo info,
            GuiGraphicsExtractor graphics
    );
}
