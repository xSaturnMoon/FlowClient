using System.Diagnostics;
using System.IO;

namespace Launcher.Services
{
    public sealed class SharedGameSettingsService
    {
        private static readonly string SharedDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "shared");

        private static readonly string SharedOptionsPath = Path.Combine(SharedDir, "options.txt");

        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private static readonly string MinecraftSavesPath = Path.Combine(MinecraftRoot, "saves");
        private static readonly string MinecraftServersPath = Path.Combine(MinecraftRoot, "servers.dat");

        private static readonly string FlowVersionsRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "FlowVersions");

        public void ApplyToInstance(string gameDirectory)
        {
            Directory.CreateDirectory(gameDirectory);
            Directory.CreateDirectory(MinecraftRoot);

            EnsureSharedOptionsInitialized();
            EnsureMinecraftSavesInitialized();
            EnsureMinecraftServersInitialized();

            if (File.Exists(SharedOptionsPath))
                File.Copy(SharedOptionsPath, Path.Combine(gameDirectory, "options.txt"), overwrite: true);

            LinkSavesToMinecraft(gameDirectory);
            LinkConfigToShared(gameDirectory);
            ApplyServersToInstance(gameDirectory);
        }

        public void SaveFromInstance(string gameDirectory)
        {
            SaveOptionsFromInstance(gameDirectory);
            SaveServersFromInstance(gameDirectory);
        }

        public void TrackProcessExit(Process process, string gameDirectory)
        {
            var gameDir = gameDirectory;
            process.EnableRaisingEvents = true;
            process.Exited += (_, _) => SaveFromInstance(gameDir);
        }

        private static void SaveOptionsFromInstance(string gameDirectory)
        {
            var instanceOptions = Path.Combine(gameDirectory, "options.txt");
            if (!File.Exists(instanceOptions))
                return;

            Directory.CreateDirectory(SharedDir);
            File.Copy(instanceOptions, SharedOptionsPath, overwrite: true);
        }

        private static void ApplyServersToInstance(string gameDirectory)
        {
            if (IsMinecraftRoot(gameDirectory))
                return;

            if (!File.Exists(MinecraftServersPath))
                return;

            File.Copy(MinecraftServersPath, Path.Combine(gameDirectory, "servers.dat"), overwrite: true);
        }

        private static void SaveServersFromInstance(string gameDirectory)
        {
            if (IsMinecraftRoot(gameDirectory))
                return;

            var instanceServers = Path.Combine(gameDirectory, "servers.dat");
            if (!File.Exists(instanceServers))
                return;

            Directory.CreateDirectory(MinecraftRoot);
            File.Copy(instanceServers, MinecraftServersPath, overwrite: true);
        }

        private static void LinkSavesToMinecraft(string gameDirectory)
        {
            if (IsMinecraftRoot(gameDirectory))
                return;

            Directory.CreateDirectory(MinecraftSavesPath);

            var instanceSaves = Path.Combine(gameDirectory, "saves");
            if (IsReparsePoint(instanceSaves))
            {
                var target = TryGetJunctionTarget(instanceSaves);
                if (string.Equals(
                        Path.GetFullPath(target ?? ""),
                        Path.GetFullPath(MinecraftSavesPath),
                        StringComparison.OrdinalIgnoreCase))
                    return;

                TryRemoveDirectory(instanceSaves);
            }

            if (Directory.Exists(instanceSaves))
            {
                MergeWorldDirectories(instanceSaves, MinecraftSavesPath);
                TryRemoveDirectory(instanceSaves);
            }

            if (!CreateDirectoryJunction(instanceSaves, MinecraftSavesPath) && !Directory.Exists(instanceSaves))
                Directory.CreateDirectory(instanceSaves);
        }

        private static void LinkConfigToShared(string gameDirectory)
        {
            if (IsMinecraftRoot(gameDirectory))
                return;

            var sharedConfigPath = Path.Combine(SharedDir, "config");
            Directory.CreateDirectory(sharedConfigPath);

            var instanceConfig = Path.Combine(gameDirectory, "config");
            if (IsReparsePoint(instanceConfig))
            {
                var target = TryGetJunctionTarget(instanceConfig);
                if (string.Equals(
                        Path.GetFullPath(target ?? ""),
                        Path.GetFullPath(sharedConfigPath),
                        StringComparison.OrdinalIgnoreCase))
                    return;

                TryRemoveDirectory(instanceConfig);
            }

            if (Directory.Exists(instanceConfig))
            {
                CopyDirectory(instanceConfig, sharedConfigPath);
                TryRemoveDirectory(instanceConfig);
            }

            if (!CreateDirectoryJunction(instanceConfig, sharedConfigPath) && !Directory.Exists(instanceConfig))
                Directory.CreateDirectory(instanceConfig);
        }

        private void EnsureSharedOptionsInitialized()
        {
            if (File.Exists(SharedOptionsPath))
                return;

            var newest = FindNewestFile("options.txt");
            if (newest == null)
                return;

            Directory.CreateDirectory(SharedDir);
            File.Copy(newest, SharedOptionsPath, overwrite: true);
        }

        private static void EnsureMinecraftSavesInitialized()
        {
            Directory.CreateDirectory(MinecraftSavesPath);
            if (HasWorldFolders(MinecraftSavesPath))
                return;

            var newest = FindNewestSavesDirectory();
            if (newest == null)
                return;

            MergeWorldDirectories(newest, MinecraftSavesPath);
        }

        private static void EnsureMinecraftServersInitialized()
        {
            if (File.Exists(MinecraftServersPath))
                return;

            var newest = FindNewestFile("servers.dat");
            if (newest == null)
                return;

            Directory.CreateDirectory(MinecraftRoot);
            File.Copy(newest, MinecraftServersPath, overwrite: true);
        }

        private static string? FindNewestFile(string fileName)
        {
            string? bestPath = null;
            var bestTime = DateTime.MinValue;

            void Consider(string path)
            {
                if (!File.Exists(path))
                    return;

                var time = File.GetLastWriteTimeUtc(path);
                if (time <= bestTime)
                    return;

                bestTime = time;
                bestPath = path;
            }

            Consider(Path.Combine(MinecraftRoot, fileName));

            if (!Directory.Exists(FlowVersionsRoot))
                return bestPath;

            foreach (var dir in Directory.GetDirectories(FlowVersionsRoot))
                Consider(Path.Combine(dir, fileName));

            return bestPath;
        }

        private static string? FindNewestSavesDirectory()
        {
            string? bestPath = null;
            var bestTime = DateTime.MinValue;

            void Consider(string path)
            {
                if (!Directory.Exists(path) || !HasWorldFolders(path))
                    return;

                var time = Directory.GetLastWriteTimeUtc(path);
                if (time <= bestTime)
                    return;

                bestTime = time;
                bestPath = path;
            }

            Consider(MinecraftSavesPath);

            if (!Directory.Exists(FlowVersionsRoot))
                return bestPath;

            foreach (var dir in Directory.GetDirectories(FlowVersionsRoot))
                Consider(Path.Combine(dir, "saves"));

            return bestPath;
        }

        private static bool HasWorldFolders(string savesPath)
        {
            if (!Directory.Exists(savesPath))
                return false;

            try
            {
                return Directory.EnumerateDirectories(savesPath).Any();
            }
            catch
            {
                return false;
            }
        }

        private static void MergeWorldDirectories(string source, string target)
        {
            if (!Directory.Exists(source))
                return;

            Directory.CreateDirectory(target);

            foreach (var worldDir in Directory.GetDirectories(source))
            {
                var name = Path.GetFileName(worldDir);
                if (string.IsNullOrEmpty(name))
                    continue;

                var dest = Path.Combine(target, name);
                if (Directory.Exists(dest))
                    continue;

                try
                {
                    Directory.Move(worldDir, dest);
                }
                catch
                {
                    try
                    {
                        CopyDirectory(worldDir, dest);
                        TryRemoveDirectory(worldDir);
                    }
                    catch { }
                }
            }
        }

        private static void CopyDirectory(string source, string target)
        {
            Directory.CreateDirectory(target);

            foreach (var file in Directory.GetFiles(source))
                File.Copy(file, Path.Combine(target, Path.GetFileName(file)), overwrite: false);

            foreach (var dir in Directory.GetDirectories(source))
            {
                var name = Path.GetFileName(dir);
                if (!string.IsNullOrEmpty(name))
                    CopyDirectory(dir, Path.Combine(target, name));
            }
        }

        private static bool IsMinecraftRoot(string gameDirectory) =>
            string.Equals(
                Path.GetFullPath(gameDirectory),
                Path.GetFullPath(MinecraftRoot),
                StringComparison.OrdinalIgnoreCase);

        private static bool IsReparsePoint(string path)
        {
            try
            {
                if (!Directory.Exists(path))
                    return false;

                return (File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0;
            }
            catch
            {
                return false;
            }
        }

        private static string? TryGetJunctionTarget(string junctionPath)
        {
            try
            {
                var info = new DirectoryInfo(junctionPath);
                if (info.LinkTarget != null)
                    return info.LinkTarget;

                if (info.Parent == null)
                    return null;

                return Path.GetFullPath(Path.Combine(info.Parent.FullName, info.Name));
            }
            catch
            {
                return null;
            }
        }

        private static bool CreateDirectoryJunction(string junctionPath, string targetPath)
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = $"/c mklink /J \"{junctionPath}\" \"{targetPath}\"",
                    CreateNoWindow = true,
                    UseShellExecute = false
                };

                using var process = Process.Start(psi);
                process?.WaitForExit();
                return process?.ExitCode == 0 && Directory.Exists(junctionPath);
            }
            catch
            {
                return false;
            }
        }

        private static void TryRemoveDirectory(string path)
        {
            try
            {
                if (Directory.Exists(path))
                    Directory.Delete(path, recursive: true);
            }
            catch { }
        }
    }
}
