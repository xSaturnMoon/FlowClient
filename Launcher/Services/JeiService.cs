using System.IO;
using System.Security.Cryptography;
using System.Text.Json.Nodes;

namespace Launcher.Services
{
    public sealed class JeiService
    {
        private const string ProjectId = "jei";

        private static readonly string RuntimeRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "runtime", "jei");

        private readonly ModrinthService _modrinth = new();

        public async Task EnsureJeiAsync(
            string gameDirectory,
            string minecraftVersion,
            string loader = "Fabric",
            IProgress<string>? progress = null,
            CancellationToken cancellationToken = default)
        {
            var source = ResolveSourcePath(minecraftVersion, loader);
            if (source == null)
            {
                progress?.Report("Downloading Just Enough Items (JEI)…");
                source = await EnsureRuntimeJarAsync(minecraftVersion, loader, cancellationToken);
            }

            if (source == null)
                return;

            InstallToInstance(gameDirectory, source);
        }

        public void RemoveJeiMod(string gameDirectory)
        {
            var modsDir = Path.Combine(gameDirectory, "mods");
            if (!Directory.Exists(modsDir))
                return;

            foreach (var file in Directory.GetFiles(modsDir, "jei*.jar"))
            {
                try { File.Delete(file); } catch { }
            }
        }

        public static bool IsJeiEnabled(string gameDirectory)
        {
            var configPath = Path.Combine(gameDirectory, "config", "flowclient-mods.json");
            if (!File.Exists(configPath))
                return true;

            try
            {
                var json = JsonNode.Parse(File.ReadAllText(configPath))?.AsObject();
                return json?["jei"]?.GetValue<bool>() ?? true;
            }
            catch
            {
                return true;
            }
        }

        private static string NormalizeLoader(string loader)
        {
            var l = (loader ?? "fabric").Trim().ToLowerInvariant();
            if (l.Contains("neo")) return "neoforge";
            if (l.Contains("forge")) return "forge";
            if (l.Contains("quilt")) return "quilt";
            return "fabric";
        }

        private static string NormalizeVersion(string minecraftVersion) =>
            (minecraftVersion ?? "").Trim().TrimStart('v', 'V').Split('-', '+', ' ')[0];

        private static string? ResolveSourcePath(string minecraftVersion, string loader)
        {
            var normLoader = NormalizeLoader(loader);
            var normVersion = NormalizeVersion(minecraftVersion);

            var versionDir = Path.Combine(RuntimeRoot, $"{normLoader}-{normVersion}");
            if (Directory.Exists(versionDir))
            {
                var existing = Directory.GetFiles(versionDir, "jei*.jar").FirstOrDefault();
                if (existing != null)
                    return existing;
            }

            var legacyDir = Path.Combine(RuntimeRoot, normVersion);
            if (Directory.Exists(legacyDir))
            {
                var existingLegacy = Directory.GetFiles(legacyDir, "jei*.jar").FirstOrDefault();
                if (existingLegacy != null)
                    return existingLegacy;
            }

            return null;
        }

        private async Task<string?> EnsureRuntimeJarAsync(string minecraftVersion, string loader, CancellationToken ct)
        {
            var normLoader = NormalizeLoader(loader);
            var normVersion = NormalizeVersion(minecraftVersion);
            var versionDir = Path.Combine(RuntimeRoot, $"{normLoader}-{normVersion}");
            Directory.CreateDirectory(versionDir);

            var existing = Directory.GetFiles(versionDir, "jei*.jar").FirstOrDefault();
            if (existing != null)
                return existing;

            var versions = await _modrinth.GetVersionsAsync(ProjectId, normVersion, normLoader, ct);
            var release = versions.FirstOrDefault();
            if (release == null && normLoader == "quilt")
            {
                versions = await _modrinth.GetVersionsAsync(ProjectId, normVersion, "fabric", ct);
                release = versions.FirstOrDefault();
            }

            if (release == null)
                return null;

            var file = release.Files.FirstOrDefault(f => f.Primary) ?? release.Files.FirstOrDefault();
            if (file == null || string.IsNullOrWhiteSpace(file.Url))
                return null;

            var bytes = await _modrinth.DownloadFileAsync(file.Url, ct);
            var target = Path.Combine(versionDir, file.Filename);
            await File.WriteAllBytesAsync(target, bytes, ct);
            return target;
        }

        private static void InstallToInstance(string gameDirectory, string sourceJar)
        {
            var modsDir = Path.Combine(gameDirectory, "mods");
            Directory.CreateDirectory(modsDir);

            var target = Path.Combine(modsDir, Path.GetFileName(sourceJar));
            if (!ShouldCopy(sourceJar, target))
                return;

            RemoveOtherJeiJars(modsDir, target);
            File.Copy(sourceJar, target, overwrite: true);
        }

        private static void RemoveOtherJeiJars(string modsDir, string keepTargetPath)
        {
            if (!Directory.Exists(modsDir))
                return;

            var keepName = Path.GetFileName(keepTargetPath);
            foreach (var file in Directory.GetFiles(modsDir, "jei*.jar"))
            {
                var name = Path.GetFileName(file);
                if (name.Equals(keepName, StringComparison.OrdinalIgnoreCase))
                    continue;

                try { File.Delete(file); } catch { }
            }
        }

        private static bool ShouldCopy(string source, string target)
        {
            if (!File.Exists(target))
                return true;

            if (!File.Exists(source))
                return false;

            return !string.Equals(ComputeSha256(source), ComputeSha256(target), StringComparison.OrdinalIgnoreCase);
        }

        private static string ComputeSha256(string path)
        {
            using var stream = File.OpenRead(path);
            var hash = SHA256.HashData(stream);
            return Convert.ToHexString(hash);
        }
    }
}
