using System;
using System.Collections.Concurrent;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace Launcher.Services
{
    public class PackInfo
    {
        public string Title { get; set; } = "";
        public string Subtitle { get; set; } = "";
        public string IconPath { get; set; } = "";
        public bool IsEnabled { get; set; } = true;
        public string CleanFileName { get; set; } = "";
    }

    public static class PackMetadataService
    {
        private static readonly ConcurrentDictionary<string, PackInfo> Cache = new();
        private static readonly string CacheDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "Cache", "PackIcons");

        static PackMetadataService()
        {
            try { Directory.CreateDirectory(CacheDir); } catch { }
        }

        public static PackInfo GetResourcePackInfo(string packsDir, string rawFileName)
        {
            var isEnabled = !rawFileName.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase);
            var cleanFileName = isEnabled ? rawFileName : rawFileName[..^".disabled".Length];

            var fullPath = Path.Combine(packsDir, rawFileName);
            if (!File.Exists(fullPath) && !Directory.Exists(fullPath))
            {
                var alt = Path.Combine(packsDir, isEnabled ? cleanFileName + ".disabled" : cleanFileName);
                if (File.Exists(alt) || Directory.Exists(alt))
                {
                    fullPath = alt;
                    isEnabled = !isEnabled;
                }
            }

            var cleanTitle = CleanDisplayName(cleanFileName);
            var subtitle = cleanFileName;

            if (!File.Exists(fullPath) && !Directory.Exists(fullPath))
            {
                return new PackInfo
                {
                    Title = cleanTitle,
                    Subtitle = subtitle,
                    IconPath = "",
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            var cacheKey = $"rp_{fullPath}_{GetLastModifiedTicks(fullPath)}";
            if (Cache.TryGetValue(cacheKey, out var cached))
            {
                return new PackInfo
                {
                    Title = cached.Title,
                    Subtitle = cached.Subtitle,
                    IconPath = cached.IconPath,
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            string? iconPath = null;
            string? description = null;

            try
            {
                if (File.Exists(fullPath) && (fullPath.EndsWith(".zip", StringComparison.OrdinalIgnoreCase) || fullPath.EndsWith(".zip.disabled", StringComparison.OrdinalIgnoreCase)))
                {
                    using var zip = ZipFile.OpenRead(fullPath);

                    // 1. pack.mcmeta description
                    var mcmetaEntry = zip.GetEntry("pack.mcmeta");
                    if (mcmetaEntry != null)
                    {
                        using var s = mcmetaEntry.Open();
                        using var doc = JsonDocument.Parse(s);
                        if (doc.RootElement.TryGetProperty("pack", out var packObj) &&
                            packObj.TryGetProperty("description", out var descProp))
                        {
                            description = ParsePackDescription(descProp);
                        }
                    }

                    // 2. pack.png icon
                    var iconEntry = zip.GetEntry("pack.png") ??
                                    zip.GetEntry("icon.png") ??
                                    zip.GetEntry("assets/minecraft/textures/custom/icon.png");

                    if (iconEntry != null)
                    {
                        using var md5 = MD5.Create();
                        var hash = Convert.ToHexString(md5.ComputeHash(Encoding.UTF8.GetBytes(fullPath + "_" + iconEntry.FullName + "_v1")));
                        var cachedFile = Path.Combine(CacheDir, $"{hash}.png");
                        if (!File.Exists(cachedFile) || new FileInfo(cachedFile).Length == 0)
                        {
                            iconEntry.ExtractToFile(cachedFile, true);
                        }
                        iconPath = cachedFile;
                    }
                }
                else if (Directory.Exists(fullPath))
                {
                    var mcmetaPath = Path.Combine(fullPath, "pack.mcmeta");
                    if (File.Exists(mcmetaPath))
                    {
                        try
                        {
                            var text = File.ReadAllText(mcmetaPath);
                            using var doc = JsonDocument.Parse(text);
                            if (doc.RootElement.TryGetProperty("pack", out var packObj) &&
                                packObj.TryGetProperty("description", out var descProp))
                            {
                                description = ParsePackDescription(descProp);
                            }
                        }
                        catch { }
                    }

                    var iconFile = Path.Combine(fullPath, "pack.png");
                    if (!File.Exists(iconFile))
                        iconFile = Path.Combine(fullPath, "icon.png");

                    if (File.Exists(iconFile))
                        iconPath = iconFile;
                }
            }
            catch { }

            if (!string.IsNullOrWhiteSpace(description))
            {
                subtitle = description;
            }
            else
            {
                subtitle = GetFileSizeString(fullPath);
            }

            var info = new PackInfo
            {
                Title = cleanTitle,
                Subtitle = subtitle,
                IconPath = iconPath ?? "",
                IsEnabled = isEnabled,
                CleanFileName = cleanFileName
            };

            Cache[cacheKey] = info;
            return info;
        }

        public static PackInfo GetShaderPackInfo(string shadersDir, string rawFileName)
        {
            var isEnabled = !rawFileName.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase);
            var cleanFileName = isEnabled ? rawFileName : rawFileName[..^".disabled".Length];

            var fullPath = Path.Combine(shadersDir, rawFileName);
            if (!File.Exists(fullPath) && !Directory.Exists(fullPath))
            {
                var alt = Path.Combine(shadersDir, isEnabled ? cleanFileName + ".disabled" : cleanFileName);
                if (File.Exists(alt) || Directory.Exists(alt))
                {
                    fullPath = alt;
                    isEnabled = !isEnabled;
                }
            }

            var cleanTitle = CleanDisplayName(cleanFileName);
            var subtitle = cleanFileName;

            if (!File.Exists(fullPath) && !Directory.Exists(fullPath))
            {
                return new PackInfo
                {
                    Title = cleanTitle,
                    Subtitle = subtitle,
                    IconPath = "",
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            var cacheKey = $"sp_{fullPath}_{GetLastModifiedTicks(fullPath)}";
            if (Cache.TryGetValue(cacheKey, out var cached))
            {
                return new PackInfo
                {
                    Title = cached.Title,
                    Subtitle = cached.Subtitle,
                    IconPath = cached.IconPath,
                    IsEnabled = isEnabled,
                    CleanFileName = cleanFileName
                };
            }

            string? iconPath = null;
            string? description = null;

            try
            {
                // Check if companion .txt file exists with notes/description
                var companionTxt = Path.Combine(shadersDir, cleanFileName + ".txt");
                if (File.Exists(companionTxt))
                {
                    try
                    {
                        var firstLine = File.ReadLines(companionTxt).FirstOrDefault(l => !string.IsNullOrWhiteSpace(l));
                        if (!string.IsNullOrWhiteSpace(firstLine))
                            description = firstLine.Trim();
                    }
                    catch { }
                }

                if (File.Exists(fullPath) && (fullPath.EndsWith(".zip", StringComparison.OrdinalIgnoreCase) || fullPath.EndsWith(".zip.disabled", StringComparison.OrdinalIgnoreCase)))
                {
                    using var zip = ZipFile.OpenRead(fullPath);

                    // Check for pack.png, icon.png, or Screenshot.png
                    var iconEntry = zip.GetEntry("pack.png") ??
                                    zip.GetEntry("icon.png") ??
                                    zip.GetEntry("image/Screenshot.png") ??
                                    zip.GetEntry("Screenshot.png");

                    if (iconEntry != null)
                    {
                        using var md5 = MD5.Create();
                        var hash = Convert.ToHexString(md5.ComputeHash(Encoding.UTF8.GetBytes(fullPath + "_" + iconEntry.FullName + "_v1")));
                        var cachedFile = Path.Combine(CacheDir, $"{hash}.png");
                        if (!File.Exists(cachedFile) || new FileInfo(cachedFile).Length == 0)
                        {
                            iconEntry.ExtractToFile(cachedFile, true);
                        }
                        iconPath = cachedFile;
                    }
                }
                else if (Directory.Exists(fullPath))
                {
                    var iconFile = Path.Combine(fullPath, "pack.png");
                    if (!File.Exists(iconFile))
                        iconFile = Path.Combine(fullPath, "icon.png");

                    if (File.Exists(iconFile))
                        iconPath = iconFile;
                }
            }
            catch { }

            if (!string.IsNullOrWhiteSpace(description))
            {
                subtitle = description;
            }
            else
            {
                subtitle = GetFileSizeString(fullPath);
            }

            var info = new PackInfo
            {
                Title = cleanTitle,
                Subtitle = subtitle,
                IconPath = iconPath ?? "",
                IsEnabled = isEnabled,
                CleanFileName = cleanFileName
            };

            Cache[cacheKey] = info;
            return info;
        }

        private static string CleanDisplayName(string fileName)
        {
            var name = Path.GetFileNameWithoutExtension(fileName);
            // Strip Minecraft formatting codes (e.g. §6§l)
            name = Regex.Replace(name, "§.", "");
            // Replace underscores with clean spaces
            name = name.Replace('_', ' ').Trim();
            return string.IsNullOrWhiteSpace(name) ? fileName : name;
        }

        private static string ParsePackDescription(JsonElement elem)
        {
            try
            {
                if (elem.ValueKind == JsonValueKind.String)
                {
                    var text = elem.GetString() ?? "";
                    text = Regex.Replace(text, "§.", "");
                    return text.Trim();
                }
                else if (elem.ValueKind == JsonValueKind.Object)
                {
                    if (elem.TryGetProperty("text", out var tp))
                    {
                        var text = tp.GetString() ?? "";
                        text = Regex.Replace(text, "§.", "");
                        return text.Trim();
                    }
                }
                else if (elem.ValueKind == JsonValueKind.Array)
                {
                    var sb = new StringBuilder();
                    foreach (var item in elem.EnumerateArray())
                    {
                        if (item.ValueKind == JsonValueKind.String)
                            sb.Append(item.GetString());
                        else if (item.ValueKind == JsonValueKind.Object && item.TryGetProperty("text", out var tp))
                            sb.Append(tp.GetString());
                    }
                    var text = Regex.Replace(sb.ToString(), "§.", "");
                    return text.Trim();
                }
            }
            catch { }
            return "";
        }

        private static string GetFileSizeString(string path)
        {
            try
            {
                if (File.Exists(path))
                {
                    var fi = new FileInfo(path);
                    if (fi.Length >= 1024 * 1024)
                        return $"{(double)fi.Length / (1024 * 1024):0.1} MB";
                    else if (fi.Length >= 1024)
                        return $"{(double)fi.Length / 1024:0} KB";
                }
                else if (Directory.Exists(path))
                {
                    return "Folder pack";
                }
            }
            catch { }
            return Path.GetFileName(path);
        }

        private static long GetLastModifiedTicks(string path)
        {
            try
            {
                if (File.Exists(path)) return new FileInfo(path).LastWriteTimeUtc.Ticks;
                if (Directory.Exists(path)) return new DirectoryInfo(path).LastWriteTimeUtc.Ticks;
            }
            catch { }
            return 0;
        }
    }
}
