using System;
using System.Collections.Generic;

namespace Launcher.Helpers
{
    public static class LoaderBranding
    {
        private static readonly Dictionary<string, string> IconUris = new(StringComparer.OrdinalIgnoreCase)
        {
            ["Vanilla"] = "pack://application:,,,/Assets/Loaders/vanilla.png",
            ["Fabric"] = "pack://application:,,,/Assets/Loaders/fabric.png",
            ["Forge"] = "pack://application:,,,/Assets/Loaders/forge.png",
            ["NeoForge"] = "pack://application:,,,/Assets/Loaders/neoforge.png",
            ["Quilt"] = "pack://application:,,,/Assets/Loaders/quilt.png"
        };

        public static string BuildFolderId(string version, string loader)
        {
            var v = string.IsNullOrWhiteSpace(version) ? "unknown" : version.Trim();
            var l = string.IsNullOrWhiteSpace(loader) ? "VANILLA" : loader.Trim().ToUpperInvariant();
            return $"{v}-{l}";
        }

        public static string? GetIconUri(string loader) =>
            IconUris.TryGetValue(loader, out var uri) ? uri : null;

        public static bool IsLegacyGuidId(string id) =>
            id.Length == 32 && id.All(static c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
    }
}
