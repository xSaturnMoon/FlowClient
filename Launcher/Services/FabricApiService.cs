using System.IO;
using System.Linq;
using System.Security.Cryptography;

namespace Launcher.Services
{
    public sealed class FabricApiService
    {
        private const string ProjectId = "fabric-api";

        private static readonly string RuntimeRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "runtime", "fabric-api");

        private readonly ModrinthService _modrinth = new();

        public async Task EnsureFabricApiAsync(
            string gameDirectory,
            string minecraftVersion,
            IProgress<string>? progress = null,
            CancellationToken cancellationToken = default)
        {
            var modsDir = Path.Combine(gameDirectory, "mods");
            Directory.CreateDirectory(modsDir);

            if (HasFabricApi(modsDir))
                return;

            var source = ResolveSourcePath(minecraftVersion);
            if (source == null)
            {
                progress?.Report("Downloading Fabric API…");
                source = await EnsureRuntimeJarAsync(minecraftVersion, cancellationToken);
            }

            if (source == null)
                return;

            var target = Path.Combine(modsDir, Path.GetFileName(source));
            if (!File.Exists(target))
            {
                File.Copy(source, target, overwrite: true);
            }
        }

        public static bool HasFabricApi(string modsDir)
        {
            if (!Directory.Exists(modsDir))
                return false;

            return Directory.GetFiles(modsDir, "fabric-api*.jar").Any() ||
                   Directory.GetFiles(modsDir, "fabric-api*.jar.disabled").Any();
        }

        private static string NormalizeVersion(string minecraftVersion) =>
            (minecraftVersion ?? "").Trim().TrimStart('v', 'V').Split('-', '+', ' ')[0];

        private static string? ResolveSourcePath(string minecraftVersion)
        {
            var normVersion = NormalizeVersion(minecraftVersion);
            var versionDir = Path.Combine(RuntimeRoot, normVersion);
            if (Directory.Exists(versionDir))
            {
                var existing = Directory.GetFiles(versionDir, "fabric-api*.jar").FirstOrDefault();
                if (existing != null)
                    return existing;
            }

            return null;
        }

        private async Task<string?> EnsureRuntimeJarAsync(string minecraftVersion, CancellationToken ct)
        {
            var normVersion = NormalizeVersion(minecraftVersion);
            var versionDir = Path.Combine(RuntimeRoot, normVersion);
            Directory.CreateDirectory(versionDir);

            var existing = Directory.GetFiles(versionDir, "fabric-api*.jar").FirstOrDefault();
            if (existing != null)
                return existing;

            var versions = await _modrinth.GetVersionsAsync(ProjectId, normVersion, "fabric", ct);
            var release = versions.FirstOrDefault();
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
    }
}
