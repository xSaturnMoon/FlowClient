using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace Launcher.Services
{
    public sealed class StorageBreakdownInfo
    {
        public long UpdateCacheBytes { get; set; }
        public long TempFilesBytes { get; set; }
        public long LogsAndCrashesBytes { get; set; }

        public long TotalBytes => UpdateCacheBytes + TempFilesBytes + LogsAndCrashesBytes;

        public string FormattedTotal => StorageMaintenanceService.FormatBytes(TotalBytes);
        public string FormattedUpdateCache => StorageMaintenanceService.FormatBytes(UpdateCacheBytes);
        public string FormattedTempFiles => StorageMaintenanceService.FormatBytes(TempFilesBytes);
        public string FormattedLogsAndCrashes => StorageMaintenanceService.FormatBytes(LogsAndCrashesBytes);
    }

    public sealed class StorageCleanupResult
    {
        public bool Success { get; set; } = true;
        public long BytesFreed { get; set; }
        public int FilesDeleted { get; set; }
        public int DirectoriesDeleted { get; set; }
        public string FormattedBytesFreed => StorageMaintenanceService.FormatBytes(BytesFreed);
        public string Message { get; set; } = string.Empty;
    }

    public static class StorageMaintenanceService
    {
        private static readonly string FlowAppData = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "FlowLauncher");

        private static readonly string FlowUpdatesDir = Path.Combine(FlowAppData, "updates");

        private static readonly string MinecraftDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");

        private static readonly string MinecraftCrashReportsDir = Path.Combine(MinecraftDir, "crash-reports");
        private static readonly string MinecraftLogsDir = Path.Combine(MinecraftDir, "logs");

        public static StorageBreakdownInfo GetStorageBreakdown()
        {
            var info = new StorageBreakdownInfo();

            // 1. Update archives in FlowLauncher/updates
            if (Directory.Exists(FlowUpdatesDir))
            {
                info.UpdateCacheBytes += GetDirectorySizeSafe(FlowUpdatesDir);
            }

            // 2. Temp files in AppData/Local/Temp matching Flow* or flow*
            var tempDir = Path.GetTempPath();
            if (Directory.Exists(tempDir))
            {
                try
                {
                    foreach (var file in Directory.EnumerateFiles(tempDir, "FlowClient*.zip"))
                    {
                        try { info.TempFilesBytes += new FileInfo(file).Length; } catch { }
                    }
                    foreach (var file in Directory.EnumerateFiles(tempDir, "FlowLauncher*.zip"))
                    {
                        try { info.TempFilesBytes += new FileInfo(file).Length; } catch { }
                    }

                    foreach (var dir in Directory.EnumerateDirectories(tempDir, "flow*"))
                    {
                        info.TempFilesBytes += GetDirectorySizeSafe(dir);
                    }
                    foreach (var dir in Directory.EnumerateDirectories(tempDir, "Flow*"))
                    {
                        info.TempFilesBytes += GetDirectorySizeSafe(dir);
                    }
                }
                catch { }
            }

            // 3. Old logs & crash reports
            if (Directory.Exists(MinecraftCrashReportsDir))
            {
                info.LogsAndCrashesBytes += GetDirectorySizeSafe(MinecraftCrashReportsDir);
            }
            if (Directory.Exists(MinecraftLogsDir))
            {
                try
                {
                    foreach (var gz in Directory.EnumerateFiles(MinecraftLogsDir, "*.log.gz"))
                    {
                        try { info.LogsAndCrashesBytes += new FileInfo(gz).Length; } catch { }
                    }
                }
                catch { }
            }
            if (Directory.Exists(FlowAppData))
            {
                try
                {
                    foreach (var file in Directory.EnumerateFiles(FlowAppData, "*.log.*"))
                    {
                        try { info.LogsAndCrashesBytes += new FileInfo(file).Length; } catch { }
                    }
                }
                catch { }
            }

            return info;
        }

        public static StorageCleanupResult CleanTempAndCacheFiles()
        {
            var result = new StorageCleanupResult();
            long totalBytes = 0;
            int fileCount = 0;
            int dirCount = 0;

            // 1. Clean FlowUpdatesDir
            if (Directory.Exists(FlowUpdatesDir))
            {
                try
                {
                    foreach (var f in Directory.EnumerateFiles(FlowUpdatesDir, "*.*"))
                    {
                        try
                        {
                            var len = new FileInfo(f).Length;
                            File.Delete(f);
                            totalBytes += len;
                            fileCount++;
                        }
                        catch { }
                    }

                    var stagingDir = Path.Combine(FlowUpdatesDir, "staging");
                    if (Directory.Exists(stagingDir))
                    {
                        try
                        {
                            var dirSize = GetDirectorySizeSafe(stagingDir);
                            Directory.Delete(stagingDir, recursive: true);
                            totalBytes += dirSize;
                            dirCount++;
                        }
                        catch { }
                    }
                }
                catch { }
            }

            // 2. Clean %TEMP% Flow packages
            var tempDir = Path.GetTempPath();
            if (Directory.Exists(tempDir))
            {
                try
                {
                    var zipPatterns = new[] { "FlowClient*.zip", "FlowLauncher*.zip" };
                    foreach (var pattern in zipPatterns)
                    {
                        foreach (var f in Directory.EnumerateFiles(tempDir, pattern))
                        {
                            try
                            {
                                var len = new FileInfo(f).Length;
                                File.Delete(f);
                                totalBytes += len;
                                fileCount++;
                            }
                            catch { }
                        }
                    }

                    var dirPrefixes = new[] { "flowclient-*", "flowtex" };
                    foreach (var prefix in dirPrefixes)
                    {
                        foreach (var d in Directory.EnumerateDirectories(tempDir, prefix))
                        {
                            try
                            {
                                var sz = GetDirectorySizeSafe(d);
                                Directory.Delete(d, recursive: true);
                                totalBytes += sz;
                                dirCount++;
                            }
                            catch { }
                        }
                    }
                }
                catch { }
            }

            result.BytesFreed = totalBytes;
            result.FilesDeleted = fileCount;
            result.DirectoriesDeleted = dirCount;

            if (totalBytes > 0)
            {
                result.Message = $"✓ Liberati con successo {FormatBytes(totalBytes)} ({fileCount} file rimossi)!";
            }
            else
            {
                result.Message = "Nessun file temporaneo da rimuovere (spazio già ottimizzato).";
            }

            return result;
        }

        public static StorageCleanupResult CleanOldLogsAndCrashes()
        {
            var result = new StorageCleanupResult();
            long totalBytes = 0;
            int fileCount = 0;

            // Clean Minecraft crash reports
            if (Directory.Exists(MinecraftCrashReportsDir))
            {
                try
                {
                    foreach (var f in Directory.EnumerateFiles(MinecraftCrashReportsDir, "*.txt"))
                    {
                        try
                        {
                            var len = new FileInfo(f).Length;
                            File.Delete(f);
                            totalBytes += len;
                            fileCount++;
                        }
                        catch { }
                    }
                }
                catch { }
            }

            // Clean archived compressed logs (.log.gz) in .minecraft/logs
            if (Directory.Exists(MinecraftLogsDir))
            {
                try
                {
                    foreach (var f in Directory.EnumerateFiles(MinecraftLogsDir, "*.log.gz"))
                    {
                        try
                        {
                            var len = new FileInfo(f).Length;
                            File.Delete(f);
                            totalBytes += len;
                            fileCount++;
                        }
                        catch { }
                    }
                }
                catch { }
            }

            // Clean archived FlowLauncher logs (*.log.*)
            if (Directory.Exists(FlowAppData))
            {
                try
                {
                    foreach (var f in Directory.EnumerateFiles(FlowAppData, "*.log.*"))
                    {
                        try
                        {
                            var len = new FileInfo(f).Length;
                            File.Delete(f);
                            totalBytes += len;
                            fileCount++;
                        }
                        catch { }
                    }
                }
                catch { }
            }

            result.BytesFreed = totalBytes;
            result.FilesDeleted = fileCount;

            if (totalBytes > 0)
            {
                result.Message = $"✓ Rimossi {fileCount} report e log storici ({FormatBytes(totalBytes)} liberati)!";
            }
            else
            {
                result.Message = "Nessun report o log storico da eliminare.";
            }

            return result;
        }

        public static long GetDirectorySizeSafe(string dirPath)
        {
            if (!Directory.Exists(dirPath)) return 0;
            long size = 0;
            try
            {
                var dir = new DirectoryInfo(dirPath);
                foreach (var file in dir.EnumerateFiles("*", SearchOption.AllDirectories))
                {
                    try { size += file.Length; } catch { }
                }
            }
            catch { }
            return size;
        }

        public static string FormatBytes(long bytes)
        {
            if (bytes <= 0) return "0 MB";
            if (bytes < 1024 * 1024)
            {
                return $"{bytes / 1024.0:F1} KB";
            }
            if (bytes < 1024L * 1024L * 1024L)
            {
                return $"{bytes / (1024.0 * 1024.0):F1} MB";
            }
            return $"{bytes / (1024.0 * 1024.0 * 1024.0):F2} GB";
        }
    }
}
