package com.flowclient.mods.tab;

import com.flowclient.compat.FlowGfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public final class FlowTabRenderer {
    private static final int MAX_ROWS_PER_COLUMN = 20;
    private static final int ROW_HEIGHT = 9;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_OFFSET = 9;
    private static final int PING_MARGIN = 11;
    private static final int PANEL_MARGIN = 10;
    private static final int VANILLA_TOP_Y = 10;
    private static final int PANEL_BG = 0x80000000;
    private static final int HEADER_FOOTER_BG = 0x80000000;
    private static final int TEXT_WRAP_PADDING = 50;

    private FlowTabRenderer() {
    }

    public static void render(FlowTabBridge bridge, GuiGraphics graphics, int screenWidth, Scoreboard scoreboard, Objective objective) {
        FlowTabSettings settings = FlowTabSettings.get();
        Minecraft minecraft = bridge.minecraft();
        Font font = minecraft.font;
        List<PlayerInfo> players = resolvePlayers(bridge.players(), settings.sort());
        if (players.isEmpty()) {
            return;
        }

        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean showPing = minecraft.getConnection() != null;
        Layout layout = Layout.compute(font, bridge, players, settings, screenWidth, showPing);
        int panelX = resolvePanelX(screenWidth, layout.panelWidth(), settings.position());
        int panelY = resolvePanelY(screenHeight, layout.panelHeight(), settings.position(), layout.vanillaTopY());

        drawPanel(graphics, font, minecraft, bridge, players, settings, layout, panelX, panelY, showPing);
    }

    private static void drawPanel(
            GuiGraphics graphics,
            Font font,
            Minecraft minecraft,
            FlowTabBridge bridge,
            List<PlayerInfo> players,
            FlowTabSettings settings,
            Layout layout,
            int panelX,
            int panelY,
            boolean showPing
    ) {
        int rowBg = minecraft.options.getBackgroundColor(0x20FFFFFF);
        int cursorY = panelY;

        if (!layout.headerLines().isEmpty()) {
            int headerHeight = layout.headerLines().size() * ROW_HEIGHT;
            graphics.fill(panelX - 1, cursorY - 1, panelX + layout.panelWidth() + 1, cursorY + headerHeight, HEADER_FOOTER_BG);
            for (FormattedCharSequence line : layout.headerLines()) {
                int lineX = panelX + (layout.panelWidth() - font.width(line)) / 2;
                FlowGfx.text(graphics, font, line, lineX, cursorY, -1, true);
                cursorY += ROW_HEIGHT;
            }
            cursorY++;
        }

        int gridTop = cursorY;
        int gridHeight = layout.gridHeight();
        graphics.fill(panelX - 1, gridTop - 1, panelX + layout.panelWidth() + 1, gridTop + gridHeight, PANEL_BG);

        int gridStartX = panelX + (layout.panelWidth() - layout.gridWidth()) / 2;

        for (int index = 0; index < players.size(); index++) {
            PlayerInfo info = players.get(index);
            int column = index / layout.rowsPerColumn();
            int row = index % layout.rowsPerColumn();
            int rowX = gridStartX + column * (layout.columnWidth() + layout.columnGap());
            int rowY = gridTop + row * layout.rowStep();

            graphics.fill(rowX, rowY, rowX + layout.columnWidth(), rowY + 8, rowBg);

            Component name = bridge.nameForDisplay(info);
            drawPlayerRow(graphics, font, minecraft, bridge, settings, info, name, rowX, rowY, layout.columnWidth(), showPing);
        }

        cursorY = gridTop + gridHeight + 1;

        if (!layout.footerLines().isEmpty()) {
            int footerHeight = layout.footerLines().size() * ROW_HEIGHT;
            graphics.fill(panelX - 1, cursorY - 1, panelX + layout.panelWidth() + 1, cursorY + footerHeight, HEADER_FOOTER_BG);
            for (FormattedCharSequence line : layout.footerLines()) {
                int lineX = panelX + (layout.panelWidth() - font.width(line)) / 2;
                FlowGfx.text(graphics, font, line, lineX, cursorY, -1, true);
                cursorY += ROW_HEIGHT;
            }
        }
    }

    private static void drawPlayerRow(
            GuiGraphics graphics,
            Font font,
            Minecraft minecraft,
            FlowTabBridge bridge,
            FlowTabSettings settings,
            PlayerInfo info,
            Component name,
            int rowX,
            int rowY,
            int columnWidth,
            boolean showPing
    ) {
        int nameWidth = font.width(name);
        int headSpace = settings.showHeads() ? HEAD_OFFSET : 0;
        int pingSpace = showPing ? PING_MARGIN : 0;
        int textBlockWidth = headSpace + nameWidth;

        int contentStart = switch (settings.nameAlign()) {
            case LEFT -> rowX;
            case RIGHT -> rowX + columnWidth - textBlockWidth - pingSpace;
            case CENTER -> rowX + Math.max(0, (columnWidth - textBlockWidth - pingSpace) / 2);
        };

        int drawX = contentStart;
        if (settings.showHeads()) {
            drawHead(graphics, minecraft, info, drawX, rowY);
            drawX += HEAD_OFFSET;
        }

        FlowGfx.text(graphics, font, name, drawX, rowY, -1, true);

        if (showPing) {
            bridge.drawPingIcon(graphics, columnWidth, rowX, rowY, info);
        }
    }

    private static void drawHead(GuiGraphics graphics, Minecraft minecraft, PlayerInfo info, int x, int y) {
        boolean upsideDown = false;
        if (minecraft.level != null) {
            Player player = minecraft.level.getPlayerByUUID(info.getProfile().getId());
            if (player != null) {
                String name = player.getName().getString();
                upsideDown = "Dinnerbone".equalsIgnoreCase(name) || "Grumm".equalsIgnoreCase(name);
            }
        }

        ResourceLocation skin = info.getSkinLocation();
        drawPlayerFace(graphics, skin, x, y, HEAD_SIZE, upsideDown);
    }

    private static void drawPlayerFace(GuiGraphics graphics, ResourceLocation skin, int x, int y, int size, boolean upsideDown) {
        if (upsideDown) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + size / 2.0F, y + size / 2.0F, 0.0F);
            pose.scale(1.0F, -1.0F, 1.0F);
            pose.translate(-(x + size / 2.0F), -(y + size / 2.0F), 0.0F);
            blitFace(graphics, skin, x, y, size);
            pose.popPose();
        } else {
            blitFace(graphics, skin, x, y, size);
        }
    }

    private static void blitFace(GuiGraphics graphics, ResourceLocation skin, int x, int y, int size) {
        graphics.blit(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    private static int rowContentWidth(Font font, Component name, boolean showHeads, boolean showPing) {
        int width = font.width(name);
        if (showHeads) {
            width += HEAD_OFFSET;
        }
        if (showPing) {
            width += PING_MARGIN;
        }
        return width;
    }

    private static List<PlayerInfo> resolvePlayers(List<PlayerInfo> players, FlowTabSort sort) {
        if (sort == FlowTabSort.VANILLA) {
            return players;
        }

        List<PlayerInfo> sorted = new ArrayList<>(players);
        sorted.sort(comparatorFor(sort));
        return sorted;
    }

    private static Comparator<PlayerInfo> comparatorFor(FlowTabSort sort) {
        return switch (sort) {
            case NAME_ASC -> Comparator.comparing(p -> p.getProfile().getName(), String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing((PlayerInfo p) -> p.getProfile().getName(), String.CASE_INSENSITIVE_ORDER).reversed();
            case PING_LOW -> Comparator.comparingInt(PlayerInfo::getLatency);
            case PING_HIGH -> Comparator.comparingInt(PlayerInfo::getLatency).reversed();
            case VANILLA -> Comparator.comparingInt(p -> 0);
        };
    }

    private static int resolvePanelX(int screenWidth, int panelWidth, FlowTabPosition position) {
        return switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> PANEL_MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - panelWidth - PANEL_MARGIN;
            default -> (screenWidth - panelWidth) / 2;
        };
    }

    private static int resolvePanelY(int screenHeight, int panelHeight, FlowTabPosition position, int vanillaTopY) {
        return switch (position) {
            case BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - panelHeight - PANEL_MARGIN;
            case CENTER -> (screenHeight - panelHeight) / 2;
            default -> vanillaTopY;
        };
    }

    private record Layout(
            int columns,
            int rowsPerColumn,
            int columnWidth,
            int columnGap,
            int rowStep,
            int gridWidth,
            int gridHeight,
            int panelWidth,
            int panelHeight,
            List<FormattedCharSequence> headerLines,
            List<FormattedCharSequence> footerLines
    ) {
        static Layout compute(
                Font font,
                FlowTabBridge bridge,
                List<PlayerInfo> players,
                FlowTabSettings settings,
                int screenWidth,
                boolean showPing
        ) {
            int playerCount = players.size();
            int columns = 1;
            int rowsPerColumn = playerCount;
            while (rowsPerColumn > MAX_ROWS_PER_COLUMN) {
                columns++;
                rowsPerColumn = (playerCount + columns - 1) / columns;
            }

            int columnWidth = 0;
            for (PlayerInfo info : players) {
                columnWidth = Math.max(
                        columnWidth,
                        rowContentWidth(font, bridge.nameForDisplay(info), settings.showHeads(), showPing)
                );
            }
            columnWidth = Math.min(columnWidth, screenWidth - TEXT_WRAP_PADDING);

            int columnGap = settings.columnGap();
            int rowStep = ROW_HEIGHT + settings.rowSpacing();
            int gridWidth = columns * columnWidth + Math.max(0, columns - 1) * columnGap;
            int gridHeight = rowsPerColumn * ROW_HEIGHT + Math.max(0, rowsPerColumn - 1) * settings.rowSpacing();
            int panelWidth = gridWidth;

            List<FormattedCharSequence> headerLines = List.of();
            if (bridge.header() != null) {
                headerLines = font.split(bridge.header(), screenWidth - TEXT_WRAP_PADDING);
                for (FormattedCharSequence line : headerLines) {
                    panelWidth = Math.max(panelWidth, font.width(line));
                }
            }

            List<FormattedCharSequence> footerLines = List.of();
            if (bridge.footer() != null) {
                footerLines = font.split(bridge.footer(), screenWidth - TEXT_WRAP_PADDING);
                for (FormattedCharSequence line : footerLines) {
                    panelWidth = Math.max(panelWidth, font.width(line));
                }
            }

            int headerBlock = headerLines.isEmpty() ? 0 : headerLines.size() * ROW_HEIGHT + 1;
            int footerBlock = footerLines.isEmpty() ? 0 : footerLines.size() * ROW_HEIGHT;
            int panelHeight = headerBlock + gridHeight + 1 + footerBlock;

            return new Layout(
                    columns,
                    rowsPerColumn,
                    columnWidth,
                    columnGap,
                    rowStep,
                    gridWidth,
                    gridHeight,
                    panelWidth,
                    panelHeight,
                    headerLines,
                    footerLines
            );
        }

        int vanillaTopY() {
            return VANILLA_TOP_Y;
        }
    }
}
