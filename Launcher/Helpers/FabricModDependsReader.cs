using System.IO;
using System.IO.Compression;
using System.Text.Json;

namespace Launcher.Helpers
{
    public static class FabricModDependsReader
    {
        private static readonly HashSet<string> IgnoredModIds = new(StringComparer.OrdinalIgnoreCase)
        {
            "minecraft",
            "fabricloader",
            "fabric-loader",
            "java",
            "forge",
            "neoforge",
            "quilt_loader",
            "quilt-loader"
        };

        public static IReadOnlyList<string> ReadRequiredModIds(byte[] jarBytes)
        {
            try
            {
                using var stream = new MemoryStream(jarBytes);
                using var zip = new ZipArchive(stream, ZipArchiveMode.Read);

                var entry = zip.GetEntry("fabric.mod.json");
                if (entry == null)
                    return Array.Empty<string>();

                using var reader = new StreamReader(entry.Open());
                using var json = JsonDocument.Parse(reader.ReadToEnd());
                if (!json.RootElement.TryGetProperty("depends", out var depends) || depends.ValueKind != JsonValueKind.Object)
                    return Array.Empty<string>();

                var ids = new List<string>();
                foreach (var property in depends.EnumerateObject())
                {
                    if (!IgnoredModIds.Contains(property.Name))
                        ids.Add(property.Name);
                }

                return ids;
            }
            catch
            {
                return Array.Empty<string>();
            }
        }
    }
}
