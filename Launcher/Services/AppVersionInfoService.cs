using System;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Launcher.Services
{
    public sealed class InstallTrackingInfo
    {
        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("downloadedAt")]
        public DateTime DownloadedAt { get; set; }
    }

    public static class AppVersionInfoService
    {
        private static readonly CultureInfo ItalianCulture = new("it-IT");

        private static readonly string TrackingFilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "install_tracking.json");

        public static string GetCurrentVersion()
        {
            try
            {
                var asm = Assembly.GetExecutingAssembly();
                var infoVer = asm.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion;
                if (!string.IsNullOrWhiteSpace(infoVer))
                {
                    var plus = infoVer.IndexOf('+');
                    return plus >= 0 ? infoVer[..plus] : infoVer;
                }
                var v = asm.GetName().Version;
                if (v != null) return $"{v.Major}.{v.Minor}.{v.Build}";
            }
            catch { }
            return "1.0.3";
        }

        public static string GetReleaseDateString()
        {
            try
            {
                var updateJsonPath = Path.Combine(AppContext.BaseDirectory, "Assets", "update.json");
                if (File.Exists(updateJsonPath))
                {
                    var text = File.ReadAllText(updateJsonPath);
                    using var doc = JsonDocument.Parse(text);
                    if (doc.RootElement.TryGetProperty("releaseDate", out var prop))
                    {
                        var str = prop.GetString();
                        if (!string.IsNullOrWhiteSpace(str)) return str;
                    }
                }
            }
            catch { }

            try
            {
                var asmPath = typeof(AppVersionInfoService).Assembly.Location;
                if (File.Exists(asmPath))
                {
                    var date = File.GetLastWriteTime(asmPath);
                    return date.ToString("dd MMMM yyyy", ItalianCulture);
                }
            }
            catch { }

            return DateTime.Now.ToString("dd MMMM yyyy", ItalianCulture);
        }

        public static DateTime GetDownloadOrInstallDate()
        {
            var currentVer = GetCurrentVersion();

            try
            {
                if (File.Exists(TrackingFilePath))
                {
                    var json = File.ReadAllText(TrackingFilePath);
                    var info = JsonSerializer.Deserialize<InstallTrackingInfo>(json);
                    if (info != null && info.Version == currentVer && info.DownloadedAt != default)
                    {
                        return info.DownloadedAt;
                    }
                }
            }
            catch { }

            DateTime detectedDate;
            try
            {
                var exePath = Path.Combine(AppContext.BaseDirectory, "FlowClient.exe");
                if (File.Exists(exePath))
                {
                    var ct = File.GetCreationTime(exePath);
                    detectedDate = (ct > DateTime.MinValue && ct <= DateTime.Now.AddHours(1)) ? ct : DateTime.Now;
                }
                else
                {
                    detectedDate = DateTime.Now;
                }
            }
            catch
            {
                detectedDate = DateTime.Now;
            }

            RecordInstall(currentVer, detectedDate);
            return detectedDate;
        }

        public static string GetDownloadOrInstallDateString()
        {
            var date = GetDownloadOrInstallDate();
            return date.ToString("dd/MM/yyyy 'alle' HH:mm", ItalianCulture);
        }

        public static void RecordInstall(string version, DateTime? timestamp = null)
        {
            try
            {
                var dir = Path.GetDirectoryName(TrackingFilePath);
                if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);

                var data = new InstallTrackingInfo
                {
                    Version = version,
                    DownloadedAt = timestamp ?? DateTime.Now
                };
                var json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(TrackingFilePath, json);
            }
            catch { }
        }
    }
}