using System.IO;
using System.Security.Cryptography;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class FlowClientBrandingService
    {
        private static readonly string RuntimeDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "runtime");

        public void RemoveBrandingMod(string gameDirectory)
        {
            var modsDir = Path.Combine(gameDirectory, "mods");
            if (!Directory.Exists(modsDir))
                return;

            foreach (var file in Directory.GetFiles(modsDir, "flowclient-branding*.jar"))
            {
                try { File.Delete(file); } catch { }
            }
        }

        public void RemoveBrandingModFromAllInstances(InstanceService instances)
        {
            foreach (var instance in instances.GetAll())
            {
                var gameDir = ResolveGameDirectory(instance);
                RemoveBrandingMod(gameDir);
            }
        }

        private static string ResolveGameDirectory(MinecraftInstance instance)
        {
            var loader = instance.Loader?.Trim() ?? "Vanilla";
            var isVanilla = loader.Equals("Vanilla", StringComparison.OrdinalIgnoreCase);
            var hasFlowMods = instance.Mods.Count > 0;

            if (isVanilla && !hasFlowMods)
            {
                return Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    ".minecraft");
            }

            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "FlowLauncher", "FlowVersions", instance.Id);
        }

        public void EnsureBrandingMod(string gameDirectory, string minecraftVersion, string loader)
        {
            var source = ResolveSourcePath(minecraftVersion, loader);
            if (!File.Exists(source))
                return;

            var modsDir = Path.Combine(gameDirectory, "mods");
            Directory.CreateDirectory(modsDir);

            RemoveOtherFlowClientJars(modsDir, source);

            var target = Path.Combine(modsDir, Path.GetFileName(source));
            if (ShouldCopy(source, target))
                File.Copy(source, target, overwrite: true);
        }

        public void EnsureFabricBrandingMod(string gameDirectory, string minecraftVersion) =>
            EnsureBrandingMod(gameDirectory, minecraftVersion, "Fabric");

        public static string ResolveModFileName(string minecraftVersion, string loader) =>
            ResolveModFileName(minecraftVersion);

        public static string ResolveModFileName(string minecraftVersion) =>
            "flowclient-branding-26.2-1.0.0.jar";

        private static string ResolveSourcePath(string minecraftVersion, string loader)
        {
            var fileName = ResolveModFileName(minecraftVersion, loader);
            var bundled = Path.Combine(AppContext.BaseDirectory, "Assets", "Mods", fileName);
            if (File.Exists(bundled))
                return bundled;

            var runtime = Path.Combine(RuntimeDir, fileName);
            if (File.Exists(runtime))
                return runtime;

            return bundled;
        }

        private static IEnumerable<string> GetCandidateModFiles(string minecraftVersion, string loader)
        {
            yield return "flowclient-branding-26.2-1.0.0.jar";
        }

        private static void RemoveOtherFlowClientJars(string modsDir, string keepSourcePath)
        {
            if (!Directory.Exists(modsDir))
                return;

            var keepName = Path.GetFileName(keepSourcePath);
            foreach (var file in Directory.GetFiles(modsDir, "flowclient-branding*.jar"))
            {
                var name = Path.GetFileName(file);
                if (name.Equals(keepName, StringComparison.OrdinalIgnoreCase))
                    continue;

                File.Delete(file);
            }
        }

        private static bool ShouldCopy(string source, string target)
        {
            if (!File.Exists(target))
                return true;

            if (!File.Exists(source))
                return false;

            var sourceInfo = new FileInfo(source);
            var targetInfo = new FileInfo(target);

            if (targetInfo.Length == sourceInfo.Length
                && string.Equals(ComputeSha256(source), ComputeSha256(target), StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }

            return true;
        }

        private static string ComputeSha256(string path)
        {
            using var stream = File.OpenRead(path);
            var hash = SHA256.HashData(stream);
            return Convert.ToHexString(hash);
        }
    }
}
