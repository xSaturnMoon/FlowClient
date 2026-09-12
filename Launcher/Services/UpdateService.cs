using System.IO;
using System.IO.Compression;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Reflection;
using System.Text.Json;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class UpdateService
    {
        private static readonly HttpClient Http = new()
        {
            Timeout = TimeSpan.FromMinutes(5)
        };

        private static readonly JsonSerializerOptions JsonOptions = new()
        {
            PropertyNameCaseInsensitive = true
        };

        private static readonly string UserManifestPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "update.json");

        private static readonly string UpdatesDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "updates");

        static UpdateService()
        {
            Http.DefaultRequestHeaders.UserAgent.ParseAdd("FlowLauncher/1.0");
            Http.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }

        public string GetCurrentVersion()
        {
            var assembly = Assembly.GetExecutingAssembly();
            var infoVersion = assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion;
            if (!string.IsNullOrWhiteSpace(infoVersion))
            {
                var plus = infoVersion.IndexOf('+');
                return plus >= 0 ? infoVersion[..plus] : infoVersion;
            }

            var ver = assembly.GetName().Version;
            if (ver == null)
                return "1.0.0";

            return ver.Revision > 0
                ? $"{ver.Major}.{ver.Minor}.{ver.Build}.{ver.Revision}"
                : $"{ver.Major}.{ver.Minor}.{ver.Build}";
        }

        public async Task<UpdateCheckResult> CheckAsync(CancellationToken ct = default)
        {
            var current = GetCurrentVersion();

            try
            {
                var manifest = await LoadManifestAsync(ct);
                UpdateCheckResult? githubResult = null;

                if (!string.IsNullOrWhiteSpace(manifest.GitHubRepository))
                {
                    githubResult = await TryCheckGitHubAsync(manifest.GitHubRepository, current, ct);
                    if (githubResult is { Success: true, UpdateAvailable: true })
                        return githubResult;
                }

                var latest = NormalizeVersion(manifest.Version);
                var available = IsNewer(latest, current);
                var downloadUrl = manifest.DownloadUrl;

                if (!available && githubResult is { Success: true })
                {
                    latest = githubResult.LatestVersion ?? latest;
                    available = githubResult.UpdateAvailable;
                    downloadUrl ??= githubResult.DownloadUrl;
                }

                return new UpdateCheckResult
                {
                    Success = true,
                    UpdateAvailable = available,
                    CurrentVersion = current,
                    LatestVersion = latest,
                    ReleaseNotes = manifest.ReleaseNotes ?? githubResult?.ReleaseNotes,
                    DownloadUrl = downloadUrl ?? githubResult?.DownloadUrl,
                    Channel = manifest.Channel ?? githubResult?.Channel,
                    Message = available
                        ? $"Update available: {latest}"
                        : "You're on the latest version."
                };
            }
            catch (Exception ex)
            {
                return new UpdateCheckResult
                {
                    Success = false,
                    UpdateAvailable = false,
                    CurrentVersion = current,
                    Message = $"Couldn't check for updates. {ex.Message}"
                };
            }
        }

        public static void CleanOldUpdates(string? keepZip = null)
        {
            try
            {
                if (!Directory.Exists(UpdatesDirectory))
                    return;

                foreach (var file in Directory.EnumerateFiles(UpdatesDirectory, "*.*"))
                {
                    if (keepZip != null && string.Equals(file, keepZip, StringComparison.OrdinalIgnoreCase))
                        continue;
                    try { File.Delete(file); } catch { }
                }

                var stagingDir = Path.Combine(UpdatesDirectory, "staging");
                if (Directory.Exists(stagingDir))
                {
                    try { Directory.Delete(stagingDir, recursive: true); } catch { }
                }
            }
            catch { }
        }

        public async Task<string> DownloadAsync(
            string downloadUrl,
            IProgress<double>? progress = null,
            CancellationToken ct = default)
        {
            Directory.CreateDirectory(UpdatesDirectory);

            // Clean any previous downloaded updates so disk space is not wasted
            CleanOldUpdates();

            var fileName = $"FlowClient-{Guid.NewGuid():N}.zip";
            var destPath = Path.Combine(UpdatesDirectory, fileName);

            using var response = await Http.GetAsync(downloadUrl, HttpCompletionOption.ResponseHeadersRead, ct);
            response.EnsureSuccessStatusCode();

            var total = response.Content.Headers.ContentLength ?? -1;
            await using var input = await response.Content.ReadAsStreamAsync(ct);
            await using var output = File.Create(destPath);

            var buffer = new byte[81920];
            long readTotal = 0;
            int read;
            while ((read = await input.ReadAsync(buffer, ct)) > 0)
            {
                await output.WriteAsync(buffer.AsMemory(0, read), ct);
                readTotal += read;
                if (total > 0)
                    progress?.Report(readTotal * 100.0 / total);
            }

            progress?.Report(100);
            return destPath;
        }

        public string PrepareInstall(string zipPath)
        {
            if (!File.Exists(zipPath))
                throw new FileNotFoundException("Update package not found.", zipPath);

            // Clean any other old zips, keeping only current
            CleanOldUpdates(keepZip: zipPath);

            var stagingDir = Path.Combine(UpdatesDirectory, "staging");
            if (Directory.Exists(stagingDir))
                Directory.Delete(stagingDir, recursive: true);

            ZipFile.ExtractToDirectory(zipPath, stagingDir, overwriteFiles: true);

            try
            {
                var stagedUpdateJson = Path.Combine(stagingDir, "Assets", "update.json");
                var targetVer = "1.0.3";
                if (File.Exists(stagedUpdateJson))
                {
                    using var doc = JsonDocument.Parse(File.ReadAllText(stagedUpdateJson));
                    if (doc.RootElement.TryGetProperty("version", out var vp))
                        targetVer = vp.GetString() ?? targetVer;
                }
                AppVersionInfoService.RecordInstall(targetVer, DateTime.Now);
            }
            catch { }

            var installDir = GetInstallDir();
            Directory.CreateDirectory(installDir);

            var exePath = Path.Combine(installDir, "FlowClient.exe");
            var scriptPath = Path.Combine(UpdatesDirectory, "apply-update.cmd");
            var script = $"""
                @echo off
                timeout /t 2 /nobreak >nul
                xcopy /E /Y /I "{stagingDir}\*" "{installDir}"
                start "" "{exePath}"
                rmdir /S /Q "{stagingDir}" >nul 2>&1
                del /Q "{UpdatesDirectory}\*.zip" >nul 2>&1
                del "%~f0"
                """;

            File.WriteAllText(scriptPath, script);
            return scriptPath;
        }

        /// <summary>
        /// Returns the canonical install directory: %LOCALAPPDATA%\Programs\FlowClient
        /// This is where the Inno Setup installer places the application.
        /// </summary>
        public static string GetInstallDir()
        {
            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "Programs", "FlowClient");
        }

        public string ResolveFlowClientBatPath()
        {
            // After installation the EXE lives directly in GetInstallDir().
            // We keep the bat fallback for legacy / dev runs.
            var installDir = GetInstallDir();
            var batInInstall = Path.Combine(installDir, "FlowClient.bat");
            if (File.Exists(batInInstall)) return batInInstall;

            var baseDir = AppContext.BaseDirectory;
            var candidates = new[]
            {
                Path.Combine(baseDir, "FlowClient.bat"),
                Path.GetFullPath(Path.Combine(baseDir, "..", "FlowClient.bat")),
                Path.GetFullPath(Path.Combine(baseDir, "..", "..", "FlowClient.bat"))
            };

            foreach (var candidate in candidates)
            {
                if (File.Exists(candidate))
                    return candidate;
            }

            return Path.GetFullPath(Path.Combine(baseDir, "..", "FlowClient.bat"));
        }


        private async Task<UpdateManifest> LoadManifestAsync(CancellationToken ct)
        {
            var bundled = await ReadManifestFileAsync(Path.Combine(AppContext.BaseDirectory, "Assets", "update.json"), ct)
                ?? new UpdateManifest { Version = GetCurrentVersion(), Channel = "Stable" };

            UpdateManifest manifest = bundled;

            if (File.Exists(UserManifestPath))
            {
                var user = await ReadManifestFileAsync(UserManifestPath, ct);
                if (user != null)
                    manifest = MergeManifests(manifest, user);
            }

            var manifestUrl = manifest.ManifestUrl;
            if (!string.IsNullOrWhiteSpace(manifestUrl))
            {
                try
                {
                    var remoteJson = await Http.GetStringAsync(manifestUrl, ct);
                    var remote = JsonSerializer.Deserialize<UpdateManifest>(remoteJson, JsonOptions);
                    if (remote != null)
                        manifest = MergeManifests(manifest, remote);
                }
                catch (Exception ex)
                {
                    throw new InvalidOperationException($"Couldn't download update manifest from {manifestUrl}. {ex.Message}", ex);
                }
            }

            return manifest;
        }

        private static UpdateManifest MergeManifests(UpdateManifest baseManifest, UpdateManifest overlay)
        {
            return new UpdateManifest
            {
                Version = string.IsNullOrWhiteSpace(overlay.Version) ? baseManifest.Version : overlay.Version,
                Channel = string.IsNullOrWhiteSpace(overlay.Channel) ? baseManifest.Channel : overlay.Channel,
                ReleaseNotes = overlay.ReleaseNotes ?? baseManifest.ReleaseNotes,
                ManifestUrl = overlay.ManifestUrl ?? baseManifest.ManifestUrl,
                GitHubRepository = overlay.GitHubRepository ?? baseManifest.GitHubRepository,
                DownloadUrl = overlay.DownloadUrl ?? baseManifest.DownloadUrl
            };
        }

        private static async Task<UpdateManifest?> ReadManifestFileAsync(string path, CancellationToken ct)
        {
            if (!File.Exists(path))
                return null;

            await using var stream = File.OpenRead(path);
            return await JsonSerializer.DeserializeAsync<UpdateManifest>(stream, JsonOptions, ct);
        }

        private async Task<UpdateCheckResult?> TryCheckGitHubAsync(string repository, string current, CancellationToken ct)
        {
            try
            {
                var url = $"https://api.github.com/repos/{repository}/releases/latest";
                using var response = await Http.GetAsync(url, ct);
                if (!response.IsSuccessStatusCode)
                    return null;

                await using var stream = await response.Content.ReadAsStreamAsync(ct);
                using var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
                var root = doc.RootElement;

                var tag = root.GetProperty("tag_name").GetString() ?? current;
                var latest = NormalizeVersion(tag);
                var notes = root.TryGetProperty("body", out var bodyEl) ? bodyEl.GetString() : null;
                var download = FindGitHubAssetUrl(root)
                    ?? (root.TryGetProperty("html_url", out var urlEl) ? urlEl.GetString() : null);
                var available = IsNewer(latest, current);

                return new UpdateCheckResult
                {
                    Success = true,
                    UpdateAvailable = available,
                    CurrentVersion = current,
                    LatestVersion = latest,
                    ReleaseNotes = notes,
                    DownloadUrl = download,
                    Message = available
                        ? $"Update available: {latest}"
                        : "You're on the latest version."
                };
            }
            catch
            {
                return null;
            }
        }

        private static string? FindGitHubAssetUrl(JsonElement releaseRoot)
        {
            if (!releaseRoot.TryGetProperty("assets", out var assets))
                return null;

            foreach (var asset in assets.EnumerateArray())
            {
                if (!asset.TryGetProperty("browser_download_url", out var urlEl))
                    continue;

                var name = asset.TryGetProperty("name", out var nameEl) ? nameEl.GetString() : null;
                if (name != null && name.EndsWith(".zip", StringComparison.OrdinalIgnoreCase))
                    return urlEl.GetString();
            }

            return null;
        }

        private static string NormalizeVersion(string raw)
        {
            var trimmed = raw.Trim();
            return trimmed.StartsWith('v') || trimmed.StartsWith('V') ? trimmed[1..] : trimmed;
        }

        private static bool IsNewer(string latest, string current)
        {
            if (Version.TryParse(latest, out var latestVer) && Version.TryParse(current, out var currentVer))
                return latestVer > currentVer;

            return !string.Equals(latest, current, StringComparison.OrdinalIgnoreCase);
        }
    }
}
