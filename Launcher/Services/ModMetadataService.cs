using System;
using System.Collections.Concurrent;
using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace Launcher.Services
{
    public class ModInfo
    {
        public string Title { get; set; } = "";
        public string Subtitle { get; set; } = "";
        public string IconPath { get; set; } = "";
        public bool IsEnabled { get; set; } = true;
        public string CleanFileName { get; set; } = "";
    }

    public static class ModMetadataService
    {
        private static readonly ConcurrentDictionary<string, ModInfo> Cache = new();
        private static readonly string CacheDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "Cache", "ModIcons");

        static ModMetadataService()
        {
            try { Directory.CreateDirectory(CacheDir); } catch { }
        }

        public static ModInfo GetModInfo(string modsDir, string rawFileName, string? knownIconUrl = null)
        {
            var isEnabled = true;
            var cleanFileName = rawFileName;
            if (rawFileName.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase))
            {
                isEnabled = false;
                cleanFileName = rawFileName[..^".disabled".Length];
            }

            var actualPath = Path.Combine(modsDir, rawFileName);
            if (!File.Exists(actualPath))
            {
                var alt = Path.Combine(modsDir, isEnabled ? cleanFileName + ".disabled" : cleanFileName);
                if (File.Exists(alt))
                {
                    actualPath = alt;
                    isEnabled = !isEnabled;
                }
            }

            if (!File.Exists(actualPath))
            {
                return new ModInfo
                {
                    Title = Path.GetFileNameWithoutExtension(cleanFileName),
                    Subtitle = cleanFileName,
                    IconPath = knownIconUrl ?? "",
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            var fi = new FileInfo(actualPath);
            var cacheKey = $"{fi.FullName}_{fi.Length}_{fi.LastWriteTimeUtc.Ticks}";
            if (Cache.TryGetValue(cacheKey, out var cached))
            {
                return new ModInfo
                {
                    Title = cached.Title,
                    Subtitle = cached.Subtitle,
                    IconPath = cached.IconPath,
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            var info = ExtractMetadata(actualPath, cleanFileName, knownIconUrl);
            info.IsEnabled = isEnabled;
            info.CleanFileName = cleanFileName;
            Cache[cacheKey] = info;
            return info;
        }

        private static ModInfo ExtractMetadata(string jarPath, string cleanFileName, string? knownIconUrl)
        {
            var fallbackTitle = Path.GetFileNameWithoutExtension(cleanFileName);
            var subtitle = cleanFileName;
            string? iconPath = knownIconUrl;

            // Handle FlowClient branding mod
            if (cleanFileName.Contains("flowclient", StringComparison.OrdinalIgnoreCase))
            {
                return new ModInfo
                {
                    Title = "FlowClient Branding",
                    Subtitle = cleanFileName,
                    IconPath = "pack://application:,,,/flow.ico",
                    CleanFileName = cleanFileName
                };
            }

            try
            {
                using var zip = ZipFile.OpenRead(jarPath);
                string? title = null;
                string? iconEntryName = null;

                // 1. Fabric mod
                var fabricEntry = zip.GetEntry("fabric.mod.json");
                if (fabricEntry != null)
                {
                    using var s = fabricEntry.Open();
                    using var doc = JsonDocument.Parse(s);
                    var root = doc.RootElement;
                    if (root.TryGetProperty("name", out var nameProp))
                    {
                        title = nameProp.GetString();
                    }
                    if (root.TryGetProperty("icon", out var iconProp))
                    {
                        if (iconProp.ValueKind == JsonValueKind.String)
                        {
                            iconEntryName = iconProp.GetString();
                        }
                        else if (iconProp.ValueKind == JsonValueKind.Object)
                        {
                            foreach (var p in iconProp.EnumerateObject())
                            {
                                iconEntryName = p.Value.GetString();
                                break;
                            }
                        }
                    }
                }

                // 2. Quilt mod
                if (title == null)
                {
                    var quiltEntry = zip.GetEntry("quilt.mod.json");
                    if (quiltEntry != null)
                    {
                        using var s = quiltEntry.Open();
                        using var doc = JsonDocument.Parse(s);
                        if (doc.RootElement.TryGetProperty("quilt_loader", out var ql) &&
                            ql.TryGetProperty("metadata", out var meta))
                        {
                            if (meta.TryGetProperty("name", out var np)) title = np.GetString();
                            if (meta.TryGetProperty("icon", out var ip)) iconEntryName = ip.GetString();
                        }
                    }
                }

                // 3. Forge / NeoForge mods.toml
                if (title == null)
                {
                    var forgeEntry = zip.GetEntry("META-INF/mods.toml");
                    if (forgeEntry != null)
                    {
                        using var reader = new StreamReader(forgeEntry.Open());
                        string? line;
                        while ((line = reader.ReadLine()) != null)
                        {
                            line = line.Trim();
                            if (line.StartsWith("displayName", StringComparison.OrdinalIgnoreCase) && line.Contains('='))
                            {
                                title = line.Split('=', 2)[1].Trim(' ', '"', '\'');
                            }
                            else if (line.StartsWith("logoFile", StringComparison.OrdinalIgnoreCase) && line.Contains('='))
                            {
                                iconEntryName = line.Split('=', 2)[1].Trim(' ', '"', '\'');
                            }
                        }
                    }
                }

                // Fallback icon scan inside zip
                if (string.IsNullOrEmpty(iconEntryName))
                {
                    if (zip.GetEntry("icon.png") != null) iconEntryName = "icon.png";
                    else if (zip.GetEntry("logo.png") != null) iconEntryName = "logo.png";
                    else if (zip.GetEntry("pack.png") != null) iconEntryName = "pack.png";
                    else if (zip.GetEntry("assets/icon.png") != null) iconEntryName = "assets/icon.png";
                }

                // Extract icon to disk cache if found
                if (!string.IsNullOrEmpty(iconEntryName))
                {
                    iconEntryName = iconEntryName.TrimStart('/', '\\');
                    var entry = zip.GetEntry(iconEntryName);
                    if (entry != null)
                    {
                        using var md5 = MD5.Create();
                        var hash = Convert.ToHexString(md5.ComputeHash(Encoding.UTF8.GetBytes(jarPath + "_" + iconEntryName)));
                        var cachedFile = Path.Combine(CacheDir, $"{hash}.png");
                        if (!File.Exists(cachedFile))
                        {
                            entry.ExtractToFile(cachedFile, true);
                        }
                        iconPath = cachedFile;
                    }
                }

                return new ModInfo
                {
                    Title = string.IsNullOrWhiteSpace(title) ? fallbackTitle : title,
                    Subtitle = subtitle,
                    IconPath = iconPath ?? "",
                    CleanFileName = cleanFileName
                };
            }
            catch
            {
                return new ModInfo
                {
                    Title = fallbackTitle,
                    Subtitle = subtitle,
                    IconPath = iconPath ?? "",
                    CleanFileName = cleanFileName
                };
            }
        }
    }
}
