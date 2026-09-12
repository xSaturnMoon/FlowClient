using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Management;
using System.Runtime.InteropServices;

namespace Launcher.Services
{
    public class LauncherSystemInfo
    {
        public string LauncherVersion  { get; set; } = "1.0.0";
        public string Channel          { get; set; } = "Stable";
        public string MinecraftDir     { get; set; } = "—";
        public string JavaPath         { get; set; } = "—";
        public string JavaVersion      { get; set; } = "—";
        public string RamTotal         { get; set; } = "—";
        public string GpuName          { get; set; } = "—";
        public string CpuName          { get; set; } = "—";
        public string OsName           { get; set; } = "—";
        public string OsVersion        { get; set; } = "—";
    }

    public class LauncherInfoService
    {
        private static LauncherSystemInfo? _cached;
        private static readonly object _lock = new();

        public LauncherSystemInfo Gather()
        {
            if (_cached != null) return _cached;

            lock (_lock)
            {
                if (_cached != null) return _cached;

                var info = new LauncherSystemInfo
                {
                    MinecraftDir = Path.Combine(
                        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                        ".minecraft"),
                    OsName = RuntimeInformation.OSDescription,
                    OsVersion = Environment.OSVersion.VersionString
                };

                try
                {
                    var ver = typeof(LauncherInfoService).Assembly.GetName().Version;
                    if (ver != null) info.LauncherVersion = $"{ver.Major}.{ver.Minor}.{ver.Build}";
                }
                catch { }

                try
                {
                    var ram = GC.GetGCMemoryInfo().TotalAvailableMemoryBytes;
                    info.RamTotal = $"{ram / (1024 * 1024 * 1024.0):0.#} GB";
                }
                catch { }

                info.JavaPath = FindJava() ?? "Not detected";
                info.JavaVersion = TryJavaVersion(info.JavaPath);
                info.GpuName = TryGpuName();
                info.CpuName = TryCpuName();

                if (!Directory.Exists(info.MinecraftDir))
                    info.MinecraftDir += " (not found)";

                _cached = info;
                return info;
            }
        }

        private static string? FindJava()
        {
            var javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                var exe = Path.Combine(javaHome, "bin", "java.exe");
                if (File.Exists(exe)) return exe;
            }

            try
            {
                var pathDirs = (Environment.GetEnvironmentVariable("PATH") ?? "")
                    .Split(';', StringSplitOptions.RemoveEmptyEntries);
                foreach (var dir in pathDirs)
                {
                    var exe = Path.Combine(dir.Trim(), "java.exe");
                    if (File.Exists(exe)) return exe;
                }
            }
            catch { }

            return null;
        }

        private static string TryJavaVersion(string javaPath)
        {
            if (javaPath == "Non rilevato" || !File.Exists(javaPath)) return "—";
            try
            {
                var psi = new ProcessStartInfo(javaPath, "-version")
                {
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using var p = Process.Start(psi);
                var output = p?.StandardError.ReadToEnd() ?? "";
                p?.WaitForExit(3000);
                var line = output.Split('\n').FirstOrDefault()?.Trim();
                return string.IsNullOrEmpty(line) ? "—" : line;
            }
            catch { return "—"; }
        }

        private static string TryCpuName()
        {
            try
            {
                if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) return "—";
                using var searcher = new ManagementObjectSearcher("SELECT Name FROM Win32_Processor");
                foreach (var obj in searcher.Get())
                {
                    var name = obj["Name"]?.ToString()?.Trim();
                    if (!string.IsNullOrWhiteSpace(name)) return name;
                }
            }
            catch { }
            return "—";
        }

        private static string TryGpuName()
        {
            try
            {
                if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) return "—";
                using var searcher = new ManagementObjectSearcher("SELECT Name FROM Win32_VideoController");
                foreach (var obj in searcher.Get())
                {
                    var name = obj["Name"]?.ToString();
                    if (!string.IsNullOrWhiteSpace(name)) return name;
                }
            }
            catch { }
            return "—";
        }
    }
}
