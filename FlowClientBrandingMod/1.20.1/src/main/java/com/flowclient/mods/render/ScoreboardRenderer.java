package com.flowclient.mods.render;

import com.flowclient.compat.FlowGfx;

import com.flowclient.mods.hud.HudElement;
import com.flowclient.mods.hud.HudLayout;
import com.flowclient.mods.hud.HudLayoutManager;
import com.flowclient.mods.hud.HudRenderHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreboardRenderer {
    private static final Comparator<Score> SCORE_DISPLAY_ORDER = Comparator
            .comparingInt(Score::getScore)
            .reversed()
            .thenComparing(Score::getOwner, String.CASE_INSENSITIVE_ORDER);
    private static final int LINE_HEIGHT = 9;
    private static final int HORIZONTAL_PADDING = 3;
    private static final int BORDER_INSET = 2;

    private ScoreboardRenderer() {
    }

    public static int getWidth(Font font, boolean preview) {
        DisplayData data = buildDisplayData(Minecraft.getInstance(), font, preview);
        return data == null ? 80 : data.width();
    }

    public static int getHeight(Font font, boolean preview) {
        DisplayData data = buildDisplayData(Minecraft.getInstance(), font, preview);
        return data == null ? 28 : data.height();
    }

    public static int resolveX(Minecraft mc, Font font, int width, int height, boolean preview) {
        ScoreboardSettings settings = ScoreboardSettings.get();
        int screenW = mc.getWindow().getGuiScaledWidth();
        return switch (settings.anchor()) {
            case RIGHT -> screenW - width - settings.sideMargin();
            case LEFT -> settings.sideMargin();
            case CUSTOM -> {
                int rawX = HudLayoutManager.get().layout(HudElement.SCOREBOARD).getRawX();
                yield rawX != HudLayout.UNSET ? rawX : screenW - width - settings.sideMargin();
            }
        };
    }

    public static void seedCustomLayoutFromCurrentAnchor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            return;
        }

        ScoreboardSettings settings = ScoreboardSettings.get();
        Font font = mc.font;
        int width = getWidth(font, false);
        int height = getHeight(font, false);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = switch (settings.anchor()) {
            case RIGHT -> screenW - width - settings.sideMargin();
            case LEFT -> settings.sideMargin();
            case CUSTOM -> {
                int rawX = HudLayoutManager.get().layout(HudElement.SCOREBOARD).getRawX();
                yield rawX != HudLayout.UNSET ? rawX : screenW - width - settings.sideMargin();
            }
        };
        int y = HudLayoutManager.get().resolveY(HudElement.SCOREBOARD, width, height);
        HudLayoutManager.get().setPositionQuiet(HudElement.SCOREBOARD, x, y);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font) {
        render(mc, graphics, font, false);
    }

    public static void render(Minecraft mc, GuiGraphics graphics, Font font, boolean preview) {
        if (!preview && !ScoreboardMod.isEnabled()) {
            return;
        }

        DisplayData data = buildDisplayData(mc, font, preview);
        if (data == null) {
            return;
        }

        int x = resolveX(mc, font, data.width(), data.height(), preview);
        int y = HudLayoutManager.get().resolveY(HudElement.SCOREBOARD, data.width(), data.height());
        float scale = HudLayoutManager.get().resolveScale(HudElement.SCOREBOARD);

        HudRenderHelper.withLayout(graphics, x, y, scale, () -> data.draw(graphics, font, x, y));
    }

    private static DisplayData buildDisplayData(Minecraft mc, Font font, boolean preview) {
        ScoreboardSettings settings = ScoreboardSettings.get();
        if (preview) {
            return previewData(font, settings);
        }

        Objective objective = resolveObjective(mc);
        if (objective == null) {
            return null;
        }

        return fromObjective(font, objective, settings);
    }

    private static Objective resolveObjective(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective teamObjective = null;
        PlayerTeam playerTeam = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
        if (playerTeam != null) {
            ChatFormatting color = playerTeam.getColor();
            if (color != null && color.getId() >= 0) {
                teamObjective = scoreboard.getDisplayObjective(3 + color.getId());
            }
        }

        return teamObjective != null ? teamObjective : scoreboard.getDisplayObjective(1);
    }

    private static DisplayData previewData(Font font, ScoreboardSettings settings) {
        int scoreWidth = settings.hideNumbers() ? 0 : font.width("12");
        DisplayEntry[] entries = {
                new DisplayEntry(
                        Component.literal("Scoreboard preview"),
                        settings.hideNumbers() ? Component.empty() : Component.literal("12"),
                        scoreWidth
                ),
                new DisplayEntry(Component.literal("Drag in Position Editor"), Component.empty(), 0)
        };
        return createDisplayData(font, Component.literal("FlowClient"), entries, settings);
    }

    private static DisplayData fromObjective(Font font, Objective objective, ScoreboardSettings settings) {
        Scoreboard scoreboard = objective.getScoreboard();

        DisplayEntry[] entries = scoreboard.getPlayerScores(objective).stream()
                .sorted(SCORE_DISPLAY_ORDER)
                .limit(15L)
                .map(score -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
                    Component name = trimScoreboardLine(
                            PlayerTeam.formatNameForTeam(team, Component.literal(score.getOwner())),
                            settings
                    );
                    if (settings.hideNumbers()) {
                        return new DisplayEntry(name, Component.empty(), 0);
                    }

                    Component scoreString = Component.literal(String.valueOf(score.getScore()));
                    return new DisplayEntry(name, scoreString, font.width(scoreString));
                })
                .toArray(DisplayEntry[]::new);

        if (entries.length == 0) {
            return null;
        }

        Component title = trimScoreboardLine(objective.getDisplayName(), settings);
        return createDisplayData(font, title, entries, settings);
    }

    private static DisplayData createDisplayData(
            Font font,
            Component title,
            DisplayEntry[] entries,
            ScoreboardSettings settings
    ) {
        int titleWidth = lineWidth(font, title, settings);
        int contentWidth = titleWidth;
        int spacerWidth = font.width(": ");

        for (DisplayEntry entry : entries) {
            int lineWidth = lineWidth(font, entry.name(), settings);
            if (!settings.hideNumbers() && entry.scoreWidth() > 0) {
                lineWidth += spacerWidth + entry.scoreWidth();
            }
            contentWidth = Math.max(contentWidth, lineWidth);
        }

        int width = contentWidth + HORIZONTAL_PADDING * 2 + BORDER_INSET * 2;
        int height = entries.length * LINE_HEIGHT + LINE_HEIGHT + 2;
        return new DisplayData(width, height, title, titleWidth, entries, contentWidth, settings);
    }

    private record DisplayEntry(Component name, Component score, int scoreWidth) {
    }

    private record DisplayData(
            int width,
            int height,
            Component title,
            int titleWidth,
            DisplayEntry[] entries,
            int contentWidth,
            ScoreboardSettings settings
    ) {
        void draw(GuiGraphics graphics, Font font, int x, int y) {
            int contentLeft = x + BORDER_INSET + HORIZONTAL_PADDING;
            int contentCenterX = contentLeft + this.contentWidth / 2;
            int contentRight = x + this.width - BORDER_INSET - HORIZONTAL_PADDING;
            int bottom = y + this.height;
            int headerY = bottom - this.entries.length * LINE_HEIGHT;

            if (this.settings.backgroundOpacity() > 0) {
                int bodyColor = backgroundArgb(this.settings.backgroundOpacity());
                int headerColor = backgroundArgb(Math.min(100, this.settings.backgroundOpacity() + 15));
                graphics.fill(x, headerY - LINE_HEIGHT - 1, x + this.width, headerY - 1, headerColor);
                graphics.fill(x, headerY - 1, x + this.width, bottom, bodyColor);
            }

            drawLine(
                    graphics,
                    font,
                    this.title,
                    contentCenterX,
                    headerY - LINE_HEIGHT,
                    this.settings.titleColor().argb(),
                    false,
                    this.settings.centerText()
            );

            for (int i = 0; i < this.entries.length; i++) {
                DisplayEntry entry = this.entries[i];
                int lineY = bottom - (this.entries.length - i) * LINE_HEIGHT;
                boolean useTeamColors = this.settings.useTeamColors();
                int nameColor = useTeamColors ? -1 : this.settings.textColor().argb();
                Component displayName = entry.name();
                if (this.settings.centerText()) {
                    drawLine(graphics, font, displayName, contentCenterX, lineY, nameColor, useTeamColors, true);
                } else {
                    FormattedCharSequence seq = displayName.getVisualOrderText();
                    FlowGfx.text(graphics, font, seq, contentLeft, lineY, nameColor, false);
                }
                if (!this.settings.hideNumbers() && entry.scoreWidth() > 0) {
                    FlowGfx.text(graphics, 
                            font,
                            entry.score(),
                            contentRight - entry.scoreWidth(),
                            lineY,
                            this.settings.scoreColor().argb(),
                            false
                    );
                }
            }
        }

        private static void drawLine(
                GuiGraphics graphics,
                Font font,
                Component text,
                int centerX,
                int y,
                int color,
                boolean preserveStyle,
                boolean centered
        ) {
            FormattedCharSequence sequence = text.getVisualOrderText();
            int drawX = centered ? centerX - font.width(sequence) / 2 : centerX;
            if (preserveStyle && color == -1) {
                FlowGfx.text(graphics, font, sequence, drawX, y, -1, false);
            } else {
                FlowGfx.text(graphics, font, sequence, drawX, y, color, false);
            }
        }

        private static int backgroundArgb(int opacityPercent) {
            int alpha = (opacityPercent * 255 / 100) << 24;
            return alpha | 0x000000;
        }
    }

    private static int lineWidth(Font font, Component text, ScoreboardSettings settings) {
        return font.width(trimScoreboardLine(text, settings).getVisualOrderText());
    }

    private static Component trimScoreboardLine(Component component, ScoreboardSettings settings) {
        if (!settings.hideNumbers() && !settings.centerText()) {
            return component;
        }

        ComponentContents contents = trimContents(component.getContents(), settings);
        List<Component> siblings = trimSiblings(component.getSiblings(), settings);
        if (contents == component.getContents() && siblings.equals(component.getSiblings())) {
            return component;
        }

        MutableComponent rebuilt = MutableComponent.create(contents).withStyle(component.getStyle());
        for (Component sibling : siblings) {
            rebuilt.append(sibling);
        }
        return rebuilt;
    }

    private static List<Component> trimSiblings(List<Component> siblings, ScoreboardSettings settings) {
        List<Component> trimmed = new ArrayList<>(siblings.size());
        for (Component sibling : siblings) {
            trimmed.add(trimScoreboardLine(sibling, settings));
        }

        while (!trimmed.isEmpty() && isBlankComponent(trimmed.get(trimmed.size() - 1))) {
            trimmed.remove(trimmed.size() - 1);
        }

        if (settings.centerText()) {
            while (!trimmed.isEmpty() && isBlankComponent(trimmed.get(0))) {
                trimmed.remove(0);
            }
        }

        return trimmed;
    }

    private static ComponentContents trimContents(ComponentContents contents, ScoreboardSettings settings) {
        if (contents instanceof LiteralContents literal) {
            String text = literal.text();
            String normalized = settings.centerText() ? text.strip() : text.stripTrailing();
            if (normalized.equals(text)) {
                return contents;
            }
            return normalized.isEmpty() ? LiteralContents.EMPTY : new LiteralContents(normalized);
        }
        return contents;
    }

    private static boolean isBlankComponent(Component component) {
        return component.getString().isBlank();
    }
}
