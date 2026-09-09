using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace Launcher.Services
{
    internal static class ProcessKiller
    {
        public static void ForceKillTree(int pid)
        {
            if (pid <= 0)
                return;

            try
            {
                using var process = Process.GetProcessById(pid);
                if (!process.HasExited)
                    process.Kill(entireProcessTree: true);
            }
            catch
            {
                // Process may already be gone.
            }

            if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return;

            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "taskkill",
                    Arguments = $"/F /T /PID {pid}",
                    CreateNoWindow = true,
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };

                using var taskkill = Process.Start(psi);
                taskkill?.WaitForExit(5000);
            }
            catch
            {
                // Ignore taskkill failures.
            }
        }

        public static bool IsAlive(int pid)
        {
            if (pid <= 0)
                return false;

            try
            {
                using var process = Process.GetProcessById(pid);
                return !process.HasExited;
            }
            catch
            {
                return false;
            }
        }

        public static HashSet<int> SnapshotAllJavaPids()
        {
            var pids = new HashSet<int>();
            foreach (var name in new[] { "javaw", "java" })
            {
                Process[] processes;
                try { processes = Process.GetProcessesByName(name); }
                catch { continue; }

                foreach (var process in processes)
                {
                    try { pids.Add(process.Id); }
                    catch { }
                    finally { process.Dispose(); }
                }
            }

            return pids;
        }

        public static List<int> GetJavaPidsStartedSince(DateTime startedAtUtc)
        {
            var result = new List<int>();
            var cutoff = startedAtUtc.ToLocalTime().AddSeconds(-3);

            foreach (var name in new[] { "javaw", "java" })
            {
                Process[] processes;
                try { processes = Process.GetProcessesByName(name); }
                catch { continue; }

                foreach (var process in processes)
                {
                    try
                    {
                        if (process.StartTime >= cutoff)
                            result.Add(process.Id);
                    }
                    catch
                    {
                        // Some system processes deny StartTime access.
                    }
                    finally
                    {
                        process.Dispose();
                    }
                }
            }

            return result;
        }

        public static bool AnyJavaAliveSince(DateTime startedAtUtc)
        {
            foreach (var pid in GetJavaPidsStartedSince(startedAtUtc))
            {
                if (IsAlive(pid))
                    return true;
            }

            return false;
        }

        public static void KillAllJavaSince(DateTime startedAtUtc)
        {
            foreach (var pid in GetJavaPidsStartedSince(startedAtUtc))
                ForceKillTree(pid);
        }

        public static void KillLikelyMinecraftOrphans()
        {
            if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return;

            foreach (var name in new[] { "javaw", "java" })
            {
                Process[] processes;
                try { processes = Process.GetProcessesByName(name); }
                catch { continue; }

                foreach (var process in processes)
                {
                    try
                    {
                        if (LooksLikeMinecraftProcess(process))
                            ForceKillTree(process.Id);
                    }
                    catch
                    {
                        // Ignore inaccessible processes.
                    }
                    finally
                    {
                        process.Dispose();
                    }
                }
            }
        }

        private static bool LooksLikeMinecraftProcess(Process process)
        {
            try
            {
                var path = process.MainModule?.FileName ?? "";
                if (path.Contains("java-runtime", StringComparison.OrdinalIgnoreCase) ||
                    path.Contains("minecraft", StringComparison.OrdinalIgnoreCase) ||
                    path.Contains("Eclipse Adoptium", StringComparison.OrdinalIgnoreCase) ||
                    path.Contains("Adoptium", StringComparison.OrdinalIgnoreCase))
                {
                    return process.WorkingSet64 > 80_000_000;
                }
            }
            catch
            {
                // Access denied for MainModule on some processes.
            }

            return false;
        }
    }
}
