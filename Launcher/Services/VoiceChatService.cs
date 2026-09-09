using System.IO;
using System.Security.Cryptography;
using System.Text.Json.Nodes;

namespace Launcher.Services
{
    public sealed class VoiceChatService
    {
        private const string ProjectId = "simple-voice-chat";
        private const string BundledJar26 = "voicechat-fabric-2.6.21+26.2.jar";

        private static readonly string RuntimeRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "runtime", "voicechat");

        private readonly ModrinthService _modrinth = new();

        public async Task EnsureVoiceChatAsync(
            string gameDirectory,
            string minecraftVersion,
            IProgress<string>? progress = null,
            CancellationToken cancellationToken = default)
        {
            var source = ResolveSourcePath(minecraftVersion);
            if (source == null)
            {
                progress?.Report("Downloading Simple Voice Chat…");
                source = await EnsureRuntimeJarAsync(minecraftVersion, cancellationToken);
            }

            if (source == null)
                return;

            InstallToInstance(gameDirectory, source);
        }

        public static bool IsVoiceChatEnabled(string gameDirectory)
        {
            var configPath = Path.Combine(gameDirectory, "config", "flowclient-mods.json");
            if (!File.Exists(configPath))
                return false;

            try
            {
                var json = JsonNode.Parse(File.ReadAllText(configPath))?.AsObject();
                return json?["voiceChat"]?.GetValue<bool>() ?? false;
            }
            catch
            {
                return false;
            }
        }

        private static string? ResolveSourcePath(string minecraftVersion)
        {
            foreach (var fileName in GetCandidateJarFiles(minecraftVersion))
            {
                var bundled = Path.Combine(AppContext.BaseDirectory, "Assets", "Mods", fileName);
                if (File.Exists(bundled))
                    return bundled;

                var runtime = Path.Combine(RuntimeRoot, NormalizeVersion(minecraftVersion), fileName);
                if (File.Exists(runtime))
                    return runtime;
            }

            var versionDir = Path.Combine(RuntimeRoot, NormalizeVersion(minecraftVersion));
            if (!Directory.Exists(versionDir))
                return null;

            return Directory.GetFiles(versionDir, "voicechat*.jar").FirstOrDefault();
        }

        private async Task<string?> EnsureRuntimeJarAsync(string minecraftVersion, CancellationToken ct)
        {
            var versionDir = Path.Combine(RuntimeRoot, NormalizeVersion(minecraftVersion));
            Directory.CreateDirectory(versionDir);

            var existing = Directory.GetFiles(versionDir, "voicechat*.jar").FirstOrDefault();
            if (existing != null)
                return existing;

            var versions = await _modrinth.GetVersionsAsync(ProjectId, NormalizeVersion(minecraftVersion), "fabric", ct);
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

        private static void InstallToInstance(string gameDirectory, string sourceJar)
        {
            var modsDir = Path.Combine(gameDirectory, "mods");
            Directory.CreateDirectory(modsDir);

            var target = Path.Combine(modsDir, Path.GetFileName(sourceJar));
            if (!ShouldCopy(sourceJar, target))
                return;

            RemoveOtherVoiceChatJars(modsDir, target);
            File.Copy(sourceJar, target, overwrite: true);
        }

        private static void RemoveOtherVoiceChatJars(string modsDir, string keepTargetPath)
        {
            if (!Directory.Exists(modsDir))
                return;

            var keepName = Path.GetFileName(keepTargetPath);
            foreach (var file in Directory.GetFiles(modsDir, "voicechat*.jar"))
            {
                var name = Path.GetFileName(file);
                if (name.Equals(keepName, StringComparison.OrdinalIgnoreCase))
                    continue;

                try { File.Delete(file); } catch { }
            }
        }

        private static IEnumerable<string> GetCandidateJarFiles(string minecraftVersion)
        {
            if (Version.TryParse(NormalizeVersion(minecraftVersion), out var version) && version.Major >= 26)
                yield return BundledJar26;
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

        private static string NormalizeVersion(string minecraftVersion) =>
            minecraftVersion.Split('-', '+', ' ')[0];
    }
}
