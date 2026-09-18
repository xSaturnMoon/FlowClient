package com.flowclient.mods.otherclient;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

public final class ClientBadgeHelper {
    private ClientBadgeHelper() {}

    public static void triggerDetection(UUID uuid, String username, PlayerInfo info) {
        if (!OtherClientMod.isEnabled() || !isValidUsername(username)) return;
        ClientDetector.inspectLocal(uuid, username, info);
    }

    public static void triggerDetection(Player player) {
        if (!OtherClientMod.isEnabled() || player == null) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() != null
                ? mc.getConnection().getPlayerInfo(player.getUUID())
                : null;
        triggerDetection(player.getUUID(), player.getGameProfile().getName(), info);
    }

    public static Component decorateTabName(UUID uuid, String username, Component currentName) {
        if (!OtherClientMod.isEnabled()) return currentName;
        triggerDetection(uuid, username, net.minecraft.client.Minecraft.getInstance().getConnection() != null
                ? net.minecraft.client.Minecraft.getInstance().getConnection().getPlayerInfo(uuid)
                : null);
        return appendBadge(uuid, currentName);
    }

    public static Component createTabListName(PlayerInfo info, Component baseName) {
        if (!OtherClientMod.isEnabled() || info == null) {
            return baseName;
        }

        GameProfile profile = info.getProfile();
        String username = cleanUsername(profile.getName());
        triggerDetection(profile.getId(), username, info);
        return appendBadge(profile.getId(), baseName != null ? baseName : Component.literal(username));
    }

    public static Component appendBadge(UUID uuid, Component baseName) {
        DetectedClient primary = resolvePrimaryBadge(uuid);
        if (primary == null || baseName == null) {
            return baseName;
        }

        Component badgeComponent;
        if (primary.isGameClient()) {
            badgeComponent = Component.literal(primary.badge).withStyle(
                    Style.EMPTY.withFont(new ResourceLocation("flowclient", "badge"))
            );
        } else {
            badgeComponent = Component.literal("[" + primary.badge + "] ")
                    .withStyle(Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(primary.color)));
        }
        return Component.empty().append(badgeComponent).append(Component.literal(" ")).append(baseName);
    }

    public static Component decorateDisplayName(Player player, Component currentName) {
        if (!OtherClientMod.isEnabled() || player == null) return currentName;
        triggerDetection(player);
        return currentName;
    }

    public static DetectedClient resolvePrimaryBadge(UUID uuid) {
        if (!OtherClientMod.isEnabled()) return null;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return DetectedClient.FLOWCLIENT;
        }

        DetectedClient primary = ClientDetector.getPrimary(uuid);
        return primary.isGameClient() ? primary : null;
    }

    public static List<DetectedClient> resolveBadges(UUID uuid) {
        List<DetectedClient> clients = ClientDetector.getClients(uuid);
        if (clients.size() == 1 && clients.get(0) == DetectedClient.LOADING) {
            return List.of();
        }

        return clients.stream()
                .filter(c -> c != DetectedClient.LOADING && c != DetectedClient.VANILLA && c.isGameClient())
                .max(DetectedClient.BY_PRIORITY)
                .map(List::of)
                .orElse(List.of());
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        String stripped = stripFormatting(username).trim();
        if (stripped.isEmpty() || stripped.length() > 16) return false;
        return stripped.matches("^(?:[\\*\\+\\.][a-zA-Z0-9_]{1,16}|[a-zA-Z0-9_]{1,16})$");
    }

    public static String stripFormatting(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public static String cleanUsername(String username) {
        return stripFormatting(username).trim();
    }
}
