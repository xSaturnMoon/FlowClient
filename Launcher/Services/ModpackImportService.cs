using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text.Json;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class ModpackImportResult
    {
        public bool Success { get; init; }
        public string Message { get; init; } = "";
        public MinecraftInstance? Instance { get; init; }
    }

    public sealed class ModpackImportService
    {
        private readonly InstanceService _instances = new();

        public ModpackImportResult Import(string archivePath)
        {
            if (!File.Exists(archivePath))
                return Fail("File not found.");

            var ext = Path.GetExtension(archivePath);
            if (!ext.Equals(".mrpack", StringComparison.OrdinalIgnoreCase)
                && !ext.Equals(".zip", StringComparison.OrdinalIgnoreCase))
            {
                return Fail("Unsupported format. Use .mrpack or .zip modpack archives.");
            }

            try
            {
                using var archive = ZipFile.OpenRead(archivePath);
                var indexEntry = archive.GetEntry("modrinth.index.json");
                if (indexEntry == null)
                    return ImportLegacyZip(archive, archivePath);

                using var indexStream = indexEntry.Open();
                using var doc = JsonDocument.Parse(indexStream);
                var root = doc.RootElement;

                var name = root.TryGetProperty("name", out var nameEl)
                    ? nameEl.GetString()
                    : Path.GetFileNameWithoutExtension(archivePath);
                if (string.IsNullOrWhiteSpace(name))
                    name = "Imported modpack";

                var mcVersion = ResolveMinecraftVersion(root);
                if (string.IsNullOrWhiteSpace(mcVersion))
                    return Fail("Could not detect Minecraft version from modpack.");

                var (loader, loaderVersion) = ResolveLoader(root);
                var instance = _instances.Create(name.Trim(), mcVersion, loader);
                instance.LoaderVersion = loaderVersion;
                _instances.Save(instance);

                var gameDir = _instances.GetInstanceDirectory(instance.Id);
                ExtractIndexedFiles(archive, root, gameDir);
                _instances.SyncModsFromDisk(instance);
                _instances.Save(instance);

                return new ModpackImportResult
                {
                    Success = true,
                    Message = $"Imported \"{instance.Name}\" ({mcVersion} · {loader}).",
                    Instance = instance
                };
            }
            catch (Exception ex)
            {
                return Fail($"Import failed: {ex.Message}");
            }
        }

        private ModpackImportResult ImportLegacyZip(ZipArchive archive, string archivePath)
        {
            var name = Path.GetFileNameWithoutExtension(archivePath);
            if (string.IsNullOrWhiteSpace(name))
                name = "Imported modpack";

            var hasMods = archive.Entries.Any(e =>
                e.FullName.Replace('\\', '/').StartsWith("mods/", StringComparison.OrdinalIgnoreCase));
            if (!hasMods)
                return Fail("This archive is not a supported modpack. Expected modrinth.index.json or a mods/ folder.");

            var instance = _instances.Create(name, "1.20.1", "Fabric");
            var gameDir = _instances.GetInstanceDirectory(instance.Id);
            ExtractAllSafe(archive, gameDir);
            _instances.SyncModsFromDisk(instance);
            _instances.Save(instance);

            return new ModpackImportResult
            {
                Success = true,
                Message = $"Imported \"{instance.Name}\". Review version and loader in settings before playing.",
                Instance = instance
            };
        }

        private void ExtractIndexedFiles(ZipArchive archive, JsonElement root, string gameDir)
        {
            if (!root.TryGetProperty("files", out var files) || files.ValueKind != JsonValueKind.Array)
                return;

            var entries = archive.Entries
                .ToDictionary(e => NormalizeEntryPath(e.FullName), e => e, StringComparer.OrdinalIgnoreCase);

            foreach (var file in files.EnumerateArray())
            {
                if (!file.TryGetProperty("path", out var pathEl))
                    continue;

                var relativePath = pathEl.GetString()?.Replace('\\', '/');
                if (string.IsNullOrWhiteSpace(relativePath))
                    continue;

                var dest = Path.Combine(gameDir, relativePath.Replace('/', Path.DirectorySeparatorChar));
                Directory.CreateDirectory(Path.GetDirectoryName(dest)!);

                if (entries.TryGetValue(relativePath, out var entry))
                {
                    entry.ExtractToFile(dest, overwrite: true);
                    continue;
                }

                var fileName = Path.GetFileName(relativePath);
                var fallback = archive.Entries.FirstOrDefault(e =>
                    e.Name.Equals(fileName, StringComparison.OrdinalIgnoreCase));
                fallback?.ExtractToFile(dest, overwrite: true);
            }
        }

        private static void ExtractAllSafe(ZipArchive archive, string destination)
        {
            foreach (var entry in archive.Entries)
            {
                if (string.IsNullOrEmpty(entry.Name))
                    continue;

                var relative = NormalizeEntryPath(entry.FullName);
                if (relative.StartsWith("../", StringComparison.Ordinal) || relative.Contains("/../", StringComparison.Ordinal))
                    continue;

                var dest = Path.GetFullPath(Path.Combine(destination, relative.Replace('/', Path.DirectorySeparatorChar)));
                var root = Path.GetFullPath(destination);
                if (!dest.StartsWith(root, StringComparison.OrdinalIgnoreCase))
                    continue;

                Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
                entry.ExtractToFile(dest, overwrite: true);
            }
        }

        private static string? ResolveMinecraftVersion(JsonElement root)
        {
            if (root.TryGetProperty("dependencies", out var deps)
                && deps.TryGetProperty("minecraft", out var mc))
            {
                var version = mc.GetString();
                if (!string.IsNullOrWhiteSpace(version))
                    return version;
            }

            if (root.TryGetProperty("game", out var game) && game.GetString() == "minecraft"
                && root.TryGetProperty("versionId", out var versionId))
            {
                return versionId.GetString();
            }

            return null;
        }

        private static (string Loader, string LoaderVersion) ResolveLoader(JsonElement root)
        {
            if (!root.TryGetProperty("dependencies", out var deps))
                return ("Fabric", "");

            if (deps.TryGetProperty("fabric-loader", out var fabric))
                return ("Fabric", fabric.GetString() ?? "");
            if (deps.TryGetProperty("quilt-loader", out var quilt))
                return ("Quilt", quilt.GetString() ?? "");
            if (deps.TryGetProperty("neoforge", out var neo))
                return ("NeoForge", neo.GetString() ?? "");
            if (deps.TryGetProperty("forge", out var forge))
                return ("Forge", forge.GetString() ?? "");

            return ("Vanilla", "");
        }

        private static string NormalizeEntryPath(string path) =>
            path.Replace('\\', '/').TrimStart('/');

        private static ModpackImportResult Fail(string message) =>
            new() { Success = false, Message = message };
    }
}
