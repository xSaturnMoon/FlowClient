using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.Installer.Forge;
using CmlLib.Core.Installer.Forge.Installers;
using CmlLib.Core.Installer.NeoForge;
using CmlLib.Core.Installer.NeoForge.Installers;
using CmlLib.Core.ModLoaders.FabricMC;
using CmlLib.Core.ModLoaders.QuiltMC;
using CmlLib.Core.ProcessBuilder;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class LaunchServerTarget
    {
        public string Host { get; init; } = "";
        public int Port { get; init; } = 25565;
        public string? Name { get; init; }
    }

    public sealed class LaunchResult
    {
        public bool Success { get; init; }
        public string Message { get; init; } = "";
        public Process? Process { get; init; }
    }

    public sealed class MinecraftLaunchService
    {
        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private readonly InstanceService _instances = new();
        private readonly MinecraftAuthService _auth = new();
        private readonly SessionKeepAliveService _session = SessionKeepAliveService.Instance;
        private readonly FlowClientBrandingService _branding = new();
        private readonly VoiceChatService _voiceChat = new();
        private readonly SharedGameSettingsService _sharedSettings = new();
        private readonly GlobalContentService _globalContent = new();
        private readonly ServerHistoryService _serverHistory = ServerHistoryService.Instance;

        public async Task<LaunchResult> LaunchAsync(
            string instanceId,
            IProgress<string>? progress = null,
            CancellationToken cancellationToken = default,
            LaunchServerTarget? server = null)
        {
            var instance = _instances.GetById(instanceId);
            if (instance == null)
                return Fail("Installation not found.");

            var account = _auth.LoadSavedAccount();
            if (account == null || string.IsNullOrEmpty(account.MinecraftToken))
                return Fail("Sign in with Microsoft before launching.");

            try
            {
                progress?.Report("Refreshing session…");
                await _session.EnsureFreshSessionAsync(cancellationToken);
                account = _auth.LoadSavedAccount();
                if (account == null)
                    return Fail("Could not load account session.");

                Directory.CreateDirectory(MinecraftRoot);
                var gameDir = Path.GetFullPath(ResolveGameDirectory(instance));
                Directory.CreateDirectory(gameDir);
                Directory.CreateDirectory(Path.Combine(gameDir, "mods"));
                _sharedSettings.ApplyToInstance(gameDir);
                _globalContent.SyncToGameDirectory(gameDir);

                if (LauncherSettingsService.Instance.Current.EnableFlowClientMod && ShouldInjectBrandingMod(instance))
                {
                    _branding.EnsureBrandingMod(gameDir, instance.MinecraftVersion, instance.Loader);
                }
                else
                {
                    _branding.RemoveBrandingMod(gameDir);
                }

                if (IsFabricInstance(instance) && LauncherSettingsService.Instance.Current.EnableVoiceChatMod)
                {
                    await _voiceChat.EnsureVoiceChatAsync(
                        gameDir,
                        instance.MinecraftVersion,
                        progress,
                        cancellationToken);
                }

                var mcPath = new MinecraftPath(MinecraftRoot);
                var launcher = new MinecraftLauncher(mcPath);

                launcher.FileProgressChanged += (_, e) =>
                {
                    if (IsDownloadEvent(e.EventType))
                        progress?.Report($"{e.EventType}: {e.Name}");
                };
                launcher.ByteProgressChanged += (_, e) =>
                {
                    if (e.TotalBytes <= 0) return;
                    var pct = (int)(e.ProgressedBytes * 100 / e.TotalBytes);
                    progress?.Report($"Downloading… {pct}%");
                };

                var mcVersion = instance.MinecraftVersion.Trim();
                var versionName = await ResolveVersionNameAsync(launcher, instance, progress, cancellationToken);

                if (!versionName.Equals(mcVersion, StringComparison.OrdinalIgnoreCase))
                    await EnsureVersionReadyAsync(launcher, mcVersion, progress, cancellationToken);

                await EnsureVersionReadyAsync(launcher, versionName, progress, cancellationToken);

                var javaPath = ResolveInstanceJavaPath(instance)
                    ?? await EnsureJavaRuntimeAsync(launcher, versionName, progress, cancellationToken);

                var session = new MSession(
                    account.MinecraftUsername,
                    account.MinecraftToken,
                    FormatUuidForLaunch(account.MinecraftUuid))
                {
                    UserType = "msa"
                };

                var launchOptions = new MLaunchOption
                {
                    Session = session,
                    Path = mcPath,
                    MaximumRamMb = Math.Clamp(instance.RamMb, 1024, 32768),
                    ScreenWidth = instance.Width > 0 ? instance.Width : 1920,
                    ScreenHeight = instance.Height > 0 ? instance.Height : 1080,
                    FullScreen = instance.Fullscreen,
                    GameLauncherName = "FlowClient",
                    ArgumentDictionary = new Dictionary<string, string>
                    {
                        ["game_directory"] = gameDir
                    }
                };

                launchOptions.JavaPath = javaPath;

                if (!string.IsNullOrWhiteSpace(instance.JvmArgs))
                {
                    launchOptions.ExtraJvmArguments = new[]
                    {
                        MArgument.FromCommandLine(instance.JvmArgs.Trim())
                    };
                }

                var flowHome = AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
                var flowJvmArgs = $"-Dflow.client.home={flowHome}";
                launchOptions.ExtraJvmArguments = launchOptions.ExtraJvmArguments == null
                    ? new[] { MArgument.FromCommandLine(flowJvmArgs) }
                    : launchOptions.ExtraJvmArguments.Concat(new[] { MArgument.FromCommandLine(flowJvmArgs) }).ToArray();

                if (server != null && !string.IsNullOrWhiteSpace(server.Host))
                {
                    launchOptions.ServerIp = server.Host.Trim();
                    launchOptions.ServerPort = server.Port is > 0 and <= 65535 ? server.Port : 25565;
                }

                progress?.Report(server != null
                    ? $"Connecting to {server.Host}…"
                    : "Starting Minecraft…");
                var beforeJavaPids = MinecraftSessionService.SnapshotJavaPids();
                var process = await launcher.BuildProcessAsync(versionName, launchOptions, cancellationToken);
                process.StartInfo.Environment["FLOW_CLIENT_HOME"] = AppContext.BaseDirectory;

                process.Start();
                process.EnableRaisingEvents = true;
                _sharedSettings.TrackProcessExit(process, gameDir);
                var serverLabel = server != null
                    ? (string.IsNullOrWhiteSpace(server.Name) ? $"{server.Host}:{server.Port}" : server.Name)
                    : null;
                MinecraftProcessTracker.Instance.SetLaunchMetadata(
                    instance.MinecraftVersion,
                    instance.Loader,
                    instance.RamMb,
                    serverLabel);
                MinecraftProcessTracker.Instance.SetProcess(process, instanceId, gameDir, beforeJavaPids);

                _instances.MarkPlayed(instanceId);
                if (server != null && !string.IsNullOrWhiteSpace(server.Host))
                    _serverHistory.RecordLaunch(server.Host, server.Port, server.Name);

                return new LaunchResult
                {
                    Success = true,
                    Message = "Minecraft started.",
                    Process = process
                };
            }
            catch (OperationCanceledException)
            {
                return Fail("Launch cancelled.");
            }
            catch (Exception ex)
            {
                return Fail(ex.Message);
            }
        }

        private static string ResolveGameDirectory(MinecraftInstance instance)
        {
            var loader = instance.Loader?.Trim() ?? "Vanilla";
            var isVanilla = loader.Equals("Vanilla", StringComparison.OrdinalIgnoreCase);
            var hasFlowMods = instance.Mods.Count > 0;

            if (isVanilla && !hasFlowMods)
                return MinecraftRoot;

            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "FlowLauncher", "FlowVersions", instance.Id);
        }

        private static async Task EnsureVersionReadyAsync(
            MinecraftLauncher launcher,
            string versionName,
            IProgress<string>? progress,
            CancellationToken cancellationToken)
        {
            progress?.Report($"Preparing {versionName}…");
            await launcher.InstallAsync(versionName, cancellationToken);
            await launcher.GetAllVersionsAsync(cancellationToken);
        }

        private static async Task<string> EnsureJavaRuntimeAsync(
            MinecraftLauncher launcher,
            string versionName,
            IProgress<string>? progress,
            CancellationToken cancellationToken)
        {
            var version = await launcher.GetVersionAsync(versionName, cancellationToken);

            for (var attempt = 0; attempt < 5; attempt++)
            {
                var bundledJava = launcher.GetJavaPath(version);
                if (IsBundledMinecraftJava(bundledJava))
                    return bundledJava!;

                progress?.Report(attempt == 0
                    ? "Downloading Java runtime…"
                    : $"Retrying Java download ({attempt + 1}/5)…");

                await launcher.InstallAsync(versionName, cancellationToken);
                await launcher.GetAllVersionsAsync(cancellationToken);
                version = await launcher.GetVersionAsync(versionName, cancellationToken);
            }

            throw new InvalidOperationException(
                "Could not download the Minecraft Java runtime for this version.\n\n" +
                "FlowClient requires the official Mojang Java bundled with Minecraft. " +
                "System Java installations (for example Eclipse Adoptium) are not used.\n\n" +
                "Check your internet connection and try again.");
        }

        private static bool IsBundledMinecraftJava(string? path)
        {
            if (!IsExistingFile(path))
                return false;

            var normalized = path!.Replace('/', '\\');
            return normalized.Contains("\\.minecraft\\runtime\\", StringComparison.OrdinalIgnoreCase);
        }

        private static bool IsExistingFile(string? path) =>
            !string.IsNullOrWhiteSpace(path) && File.Exists(path);

        private static string? FindSystemJava()
        {
            var javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                foreach (var name in new[] { "javaw.exe", "java.exe" })
                {
                    var exe = Path.Combine(javaHome, "bin", name);
                    if (File.Exists(exe))
                        return exe;
                }
            }

            try
            {
                var pathDirs = (Environment.GetEnvironmentVariable("PATH") ?? "")
                    .Split(';', StringSplitOptions.RemoveEmptyEntries);
                foreach (var dir in pathDirs)
                {
                    foreach (var name in new[] { "javaw.exe", "java.exe" })
                    {
                        var exe = Path.Combine(dir.Trim(), name);
                        if (File.Exists(exe))
                            return exe;
                    }
                }
            }
            catch
            {
                // Ignore PATH parsing issues and fall through.
            }

            return null;
        }

        private async Task<string> ResolveVersionNameAsync(
            MinecraftLauncher launcher,
            MinecraftInstance instance,
            IProgress<string>? progress,
            CancellationToken cancellationToken)
        {
            var mcVersion = instance.MinecraftVersion;
            var loader = instance.Loader?.Trim() ?? "Vanilla";

            if (loader.Equals("Vanilla", StringComparison.OrdinalIgnoreCase))
                return mcVersion;

            if (loader.Equals("Fabric", StringComparison.OrdinalIgnoreCase))
            {
                var existing = FindInstalledLoaderVersion(
                    mcVersion,
                    launcher.MinecraftPath.Versions,
                    name => name.Contains("fabric", StringComparison.OrdinalIgnoreCase));
                if (existing != null)
                {
                    progress?.Report($"Using existing Fabric profile ({existing})…");
                    return existing;
                }

                progress?.Report($"Installing Fabric profile for {mcVersion}…");
                try
                {
                    var fabricInstaller = new FabricInstaller(new HttpClient());
                    var versionName = await fabricInstaller.Install(mcVersion, launcher.MinecraftPath);
                    await launcher.GetAllVersionsAsync(cancellationToken);
                    return versionName;
                }
                catch (Exception ex)
                {
                    throw LoaderInstallFailed("Fabric", mcVersion, ex);
                }
            }

            if (loader.Equals("Forge", StringComparison.OrdinalIgnoreCase))
            {
                var existing = FindInstalledLoaderVersion(
                    mcVersion,
                    launcher.MinecraftPath.Versions,
                    name => name.Contains("forge", StringComparison.OrdinalIgnoreCase)
                            && !name.Contains("neoforge", StringComparison.OrdinalIgnoreCase));
                if (existing != null)
                {
                    progress?.Report($"Using existing Forge profile ({existing})…");
                    return existing;
                }

                progress?.Report($"Installing Forge for {mcVersion}…");
                var forgeInstaller = new ForgeInstaller(launcher);
                var forgeOptions = new ForgeInstallOptions
                {
                    SkipIfAlreadyInstalled = true,
                    CancellationToken = cancellationToken
                };

                string versionName;
                if (!string.IsNullOrWhiteSpace(instance.LoaderVersion))
                {
                    versionName = await forgeInstaller.Install(
                        mcVersion,
                        instance.LoaderVersion.Trim(),
                        forgeOptions);
                }
                else
                {
                    versionName = await forgeInstaller.Install(mcVersion, forgeOptions);
                }

                await launcher.GetAllVersionsAsync(cancellationToken);
                progress?.Report($"Forge profile ready ({versionName})…");
                return versionName;
            }

            if (loader.Equals("NeoForge", StringComparison.OrdinalIgnoreCase))
            {
                var existing = FindInstalledLoaderVersion(
                    mcVersion,
                    launcher.MinecraftPath.Versions,
                    name => name.Contains("neoforge", StringComparison.OrdinalIgnoreCase));
                if (existing != null)
                {
                    progress?.Report($"Using existing NeoForge profile ({existing})…");
                    return existing;
                }

                progress?.Report($"Installing NeoForge for {mcVersion}…");
                var neoForgeInstaller = new NeoForgeInstaller(launcher);
                var installOptions = new NeoForgeInstallOptions
                {
                    SkipIfAlreadyInstalled = true,
                    CancellationToken = cancellationToken
                };

                string versionName;
                if (!string.IsNullOrWhiteSpace(instance.LoaderVersion))
                {
                    versionName = await neoForgeInstaller.Install(
                        mcVersion,
                        instance.LoaderVersion.Trim(),
                        installOptions);
                }
                else
                {
                    versionName = await neoForgeInstaller.Install(mcVersion, installOptions);
                }

                await launcher.GetAllVersionsAsync(cancellationToken);
                progress?.Report($"NeoForge profile ready ({versionName})…");
                return versionName;
            }

            if (loader.Equals("Quilt", StringComparison.OrdinalIgnoreCase))
            {
                var existing = FindInstalledLoaderVersion(
                    mcVersion,
                    launcher.MinecraftPath.Versions,
                    name => name.Contains("quilt", StringComparison.OrdinalIgnoreCase));
                if (existing != null)
                {
                    progress?.Report($"Using existing Quilt profile ({existing})…");
                    return existing;
                }

                progress?.Report($"Installing Quilt for {mcVersion}…");
                var quiltInstaller = new QuiltInstaller(new HttpClient());
                string versionName;
                if (!string.IsNullOrWhiteSpace(instance.LoaderVersion))
                {
                    versionName = await quiltInstaller.Install(
                        mcVersion,
                        instance.LoaderVersion.Trim(),
                        launcher.MinecraftPath);
                }
                else
                {
                    versionName = await quiltInstaller.Install(mcVersion, launcher.MinecraftPath);
                }

                await launcher.GetAllVersionsAsync(cancellationToken);
                progress?.Report($"Quilt profile ready ({versionName})…");
                return versionName;
            }

            throw new InvalidOperationException($"Loader \"{loader}\" is not supported.");
        }

        private static string? FindInstalledLoaderVersion(
            string mcVersion,
            string versionsDir,
            Func<string, bool> matchesLoader)
        {
            if (!Directory.Exists(versionsDir))
                return null;

            string? best = null;
            foreach (var dir in Directory.GetDirectories(versionsDir))
            {
                var name = Path.GetFileName(dir);
                if (!matchesLoader(name))
                    continue;
                if (!name.Contains(mcVersion, StringComparison.OrdinalIgnoreCase))
                    continue;

                var json = Path.Combine(dir, $"{name}.json");
                if (!File.Exists(json))
                    continue;

                if (best == null || string.Compare(name, best, StringComparison.OrdinalIgnoreCase) > 0)
                    best = name;
            }

            return best;
        }

        private static bool IsDownloadEvent(object? eventType)
        {
            var name = eventType?.ToString() ?? "";
            return name.Contains("download", StringComparison.OrdinalIgnoreCase)
                   || name.Contains("install", StringComparison.OrdinalIgnoreCase);
        }

        private static bool IsFabricInstance(MinecraftInstance instance) =>
            (instance.Loader?.Trim() ?? "Vanilla").Equals("Fabric", StringComparison.OrdinalIgnoreCase);

        private static bool ShouldInjectBrandingMod(MinecraftInstance instance)
        {
            if (IsFabricInstance(instance))
                return true;

            var loader = instance.Loader?.Trim() ?? "Vanilla";
            return loader.Equals("NeoForge", StringComparison.OrdinalIgnoreCase)
                && instance.MinecraftVersion.StartsWith("1.21.1", StringComparison.Ordinal);
        }

        private static string? ResolveInstanceJavaPath(MinecraftInstance instance)
        {
            if (!string.IsNullOrWhiteSpace(instance.JavaPath) && File.Exists(instance.JavaPath))
                return instance.JavaPath;

            return null;
        }

        private static string FormatUuidForLaunch(string uuid)
        {
            if (string.IsNullOrWhiteSpace(uuid)) return uuid;
            var clean = uuid.Replace("-", "");
            if (clean.Length != 32) return uuid;
            return $"{clean[..8]}-{clean[8..12]}-{clean[12..16]}-{clean[16..20]}-{clean[20..]}";
        }

        private static LaunchResult Fail(string message) =>
            new() { Success = false, Message = message };

        private static InvalidOperationException LoaderInstallFailed(string loader, string mcVersion, Exception ex) =>
            new(
                $"Could not install {loader} for Minecraft {mcVersion}.\n\n" +
                "This loader may not support that Minecraft version. Try another version.\n\n" +
                $"Details: {ex.Message}",
                ex);
    }
}
