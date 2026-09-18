package com.flowclient.modpanel;

import com.flowclient.mods.hud.HudElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModDescriptions {
    private static final int TEXT_COLOR = 0xFFB8BEC8;
    private static final int LINE_SPACING = 2;

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("Nametag",
                    "Keeps your nametag visible above your head in first person, so you always see your name "
                            + "the way other players do — useful for screenshots, streams, and self-awareness in multiplayer."),
            Map.entry("Zoomify",
                    "Smooth, scroll-driven zoom that magnifies your view without the jarring snap of vanilla FOV changes. "
                            + "Tune sensitivity, speed, and easing to match how you aim, explore, or record."),
            Map.entry("Keystrokes",
                    "Shows the keys you press in a clean on-screen overlay. Ideal for tutorials, PvP clips, and streams "
                            + "where viewers need to follow your inputs at a glance."),
            Map.entry("Time Changer",
                    "Overrides the time of day on your client only, letting you lock in golden hour, bright noon, or midnight "
                            + "mood without asking the server to change world time."),
            Map.entry("Weather Changer",
                    "Controls rain, snow, and thunder locally on your screen. Clear the sky for building or dial up "
                            + "atmosphere for cinematics — other players are unaffected."),
            Map.entry("Crosshair",
                    "Replaces the default crosshair with styles built for PvP, building, and general play. Customize shape, "
                            + "color, outline, and visibility so your reticle never gets lost on busy textures."),
            Map.entry("FPS Counter",
                    "A lightweight performance readout you can place anywhere on the HUD. Track frame rate in real time "
                            + "while tuning graphics, shaders, or mods."),
            Map.entry("Item Physics",
                    "Adds natural tumble and rotation to dropped items so loot spills, deaths, and inventory drops "
                            + "feel more physical and polished."),
            Map.entry("Modify F3",
                    "Restyles the debug overlay with a cleaner FlowClient layout, trimming noise and surfacing the stats "
                            + "you actually care about when profiling or troubleshooting."),
            Map.entry("Freelook",
                    "Hold a key to orbit the camera around your character without turning your body. Scout corners, "
                            + "admire builds, and capture angles vanilla third person cannot reach."),
            Map.entry("Armor HUD",
                    "Compact armor durability bars placed exactly where you want them. Know when pieces are about to break "
                            + "before a fight or a long mining session."),
            Map.entry("Voice Chat",
                    "Proximity voice built into FlowClient. Talk to nearby players naturally, with client-side volume "
                            + "controls and seamless multiplayer integration."),
            Map.entry("AllSchematics",
                    "Browse, preview, and place schematic blueprints in the world with precision tools — built for builders, "
                            + "redstone engineers, and anyone who works from saved structures."),
            Map.entry("Block Overlay",
                    "Highlights the block you are looking at with a crisp outline. Makes mining veins, lining up placements, "
                            + "and targeting specific blocks faster and less error-prone."),
            Map.entry("Scoreboard",
                    "Customizes the sidebar scoreboard with themes, anchors, and layout options that match the rest of your "
                            + "FlowClient HUD."),
            Map.entry("OtherClient",
                    "Detects FlowClient, OptiFine, Bedrock (Floodgate/Geyser), and TLauncher players and shows a badge beside "
                            + "their name in nametags and the tab list so you can tell who is on what client in multiplayer."),
            Map.entry("Flow Quiet",
                    "Automatically throttles Minecraft to a low frame rate and silences every in-game sound when you tab out "
                            + "or go AFK — perfect for leaving the game open while watching videos or doing something else."),
            Map.entry("Flow Clock",
                    "Shows the real-world time on your HUD with clean FlowClient styling. Choose 12h or 24h format, "
                            + "optional seconds, and place it anywhere on screen."),
            Map.entry("Health Bar",
                    "Shows a health bar above players, animals, and monsters in the world. Choose style, target filter, distance, and visibility options."),
            Map.entry("Block Speed",
                    "Shows your horizontal movement speed in blocks per second while walking, sprinting, or flying. "
                            + "Fully configurable HUD with position, format, smoothing, and colors."),
            Map.entry("Media HUD",
                    "Shows the song or video currently playing on Windows — title, artist, progress bar, and playback state. "
                            + "Works with Spotify, YouTube, browsers, and other apps that expose Windows media controls."),
            Map.entry("Flow Tab",
                    "Vanilla-style player list with optional heads, sorting, spacing, alignment, and screen position."),
            Map.entry("Chat Tweaks",
                    "Customize chat with timestamps, transparent background, opacity, and scale without touching vanilla files."),
            Map.entry("Shulker Tooltip",
                    "Preview shulker box contents directly in the item tooltip as a mini inventory grid."),
            Map.entry("Hitboxes",
                    "Always show entity hitboxes in the world without opening the F3 debug menu."),
            Map.entry("Chunk Borders",
                    "Highlight chunk borders around you for building, farms, and slime-chunk hunting."),
            Map.entry("Damage Indicator",
                    "Floating damage numbers when nearby entities take hits or heal."),
            Map.entry("Inventory Sort",
                    "Sort your player inventory while a container is open. Press R to merge stacks and order items by name."),
            Map.entry("JEI",
                    "Toggle Just Enough Items (embedded recipe browser). Disabling hides overlays, keybinds, and recipe UI for this session without unloading the mod."),
            Map.entry("Advancements Unlock",
                    "Keeps and unlocks Minecraft advancements locally on multiplayer servers that block or reset them. "
                            + "Progress is client-side only and shows toasts plus the advancement screen while you play."),
            Map.entry("Portal FX",
                    "Cinematic screen tint and title when you travel between dimensions — Nether fire, End void, Overworld return."),
            Map.entry("Combo Counter",
                    "Tracks consecutive hits on the same target chain and shows a growing combo readout with punchy feedback."),
            Map.entry("Trajectory Arc",
                    "Projects the flight path for bows, crossbows, pearls, snowballs, and throwables using a world-space arc preview."),
            Map.entry("Pearl Landing Marker",
                    "Highlights the exact landing spot when you hold an ender pearl, with a ground cross and short preview arc."),
            Map.entry("Elytra Flight Path",
                    "Predicts your glide path while wearing elytra, showing where you are likely to land before you commit."),
            Map.entry("Reach Ring",
                    "Draws a circle around you at melee reach distance so you can judge attack range in PvP and PvE."),
            Map.entry("Crystal/Bed Preview",
                    "Shows explosion radius while holding end crystals. Bed preview appears only in the Nether and End."),
            Map.entry("Coordinates HUD",
                    "Displays your current XYZ position and facing direction on the HUD without opening F3."),
            Map.entry("Ping HUD",
                    "Shows your multiplayer latency in milliseconds with optional color-coded quality feedback."),
            Map.entry("Block Break Progress",
                    "Shows a mining progress bar while you break blocks, with optional percentage text and HUD positioning."),
            Map.entry("Armor Durability Alert",
                    "Warns you when any armor piece drops below a durability threshold so you can repair or swap before it breaks."),
            Map.entry("Item Cooldown HUD",
                    "Displays attack and item cooldown bars so you can see when your next hit or pearl throw is ready."),
            Map.entry("Server Address HUD",
                    "Shows the current server name, brand, or IP on your HUD — useful for streamers and multi-server players."),
            Map.entry("Battery HUD",
                    "Shows your laptop battery level and connected devices (headphones, controllers) with color-coded bars on the HUD.")
    );

    private ModDescriptions() {}

    public static String get(String modName) {
        if (modName == null || modName.isBlank()) {
            return "A FlowClient module. Enable or disable it from the mod list.";
        }
        return DESCRIPTIONS.getOrDefault(
                modName,
                "A FlowClient module. Toggle it from the mod list to enable or disable its features."
        );
    }

    public static String forHudElement(HudElement element) {
        return get(element.getDisplayName())
                + " Adjust position, scale, and layout below, or open the position editor for drag-and-drop placement.";
    }

    public static int blockHeight(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return wrap(font, text, maxWidth).size() * (font.lineHeight + LINE_SPACING);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxWidth) {
        if (text == null || text.isBlank()) {
            return;
        }

        int lineY = y;
        for (String line : wrap(font, text, maxWidth)) {
            graphics.text(font, Component.literal(line), x, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight + LINE_SPACING;
        }
    }

    public static void drawSeparator(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0xFF3A3F4B);
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                lines.add(word);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }
}
