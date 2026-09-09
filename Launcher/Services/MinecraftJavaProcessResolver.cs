using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Management;
using System.Runtime.InteropServices;
using System.Text.RegularExpressions;

namespace Launcher.Services
{
    /// <summary>
    /// Finds and terminates Minecraft Java processes launched by FlowClient.
    /// Uses PID trees and @argfile contents because modern Minecraft no longer embeds --gameDir in the command line.
    /// </summary>
    internal static class MinecraftJavaProcessResolver
    {
        private static readonly Regex ArgFilePattern = new(@"@(""[^""]+""|\S+)", RegexOptions.Compiled);

        private static readonly string[] MinecraftMarkers =
        {
            "net.fabricmc.loader",
            "KnotClient",
            "net.minecraft.client.main.Main",
            "minecraft",
            "lwjgl",
            "FlowClient",
            "fabric-loader"
        };

        public static IReadOnlyList<Process> FindForLaunch(int rootPid, string gameDirectory)
        {
            if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return Array.Empty<Process>();

            var treePids = CollectProcessTree(rootPid);
            var markers = BuildMarkers(gameDirectory);
            var matches = new Dictionary<int, Process>();

            foreach (var info in EnumerateProcesses())
            {
                if (!IsJavaExecutable(info.Name))
                    continue;

                var inTree = treePids.Contains(info.Pid);
                var matchesGame = MatchesGameDirectory(info.CommandLine, markers, gameDirectory);
                var looksLikeMinecraft = LooksLikeMinecraft(info.CommandLine);

                if (!inTree && !matchesGame && !(looksLikeMinecraft && treePids.Contains(info.ParentPid)))
                    continue;

                try
                {
                    matches[info.Pid] = Process.GetProcessById(info.Pid);
                }
                catch
                {
                    // Process exited between enumeration and attach.
                }
            }

            return matches.Values.ToList();
        }

        public static IReadOnlyList<Process> FindByGameDirectory(string gameDirectory) =>
            FindForLaunch(-1, gameDirectory);

        public static void KillForLaunch(int rootPid, string gameDirectory)
        {
            var killed = new HashSet<int>();

            foreach (var pid in CollectProcessTree(rootPid))
                ProcessKiller.ForceKillTree(pid);

            foreach (var process in FindForLaunch(rootPid, gameDirectory))
            {
                try
                {
                    ProcessKiller.ForceKillTree(process.Id);
                }
                catch
                {
                    // Ignore processes we cannot terminate.
                }
                finally
                {
                    killed.Add(process.Id);
                    try { process.Dispose(); } catch { /* ignore */ }
                }
            }

            foreach (var process in FindByGameDirectory(gameDirectory))
            {
                if (killed.Contains(process.Id))
                    continue;

                try
                {
                    ProcessKiller.ForceKillTree(process.Id);
                }
                catch
                {
                    // Ignore processes we cannot terminate.
                }
                finally
                {
                    try { process.Dispose(); } catch { /* ignore */ }
                }
            }
        }

        public static void KillByGameDirectory(string gameDirectory, int? exceptPid = null)
        {
            foreach (var process in FindByGameDirectory(gameDirectory))
            {
                if (exceptPid.HasValue && process.Id == exceptPid.Value)
                    continue;

                try
                {
                    if (!process.HasExited)
                        process.Kill(entireProcessTree: true);
                }
                catch
                {
                    // Ignore processes we cannot terminate.
                }
                finally
                {
                    try { process.Dispose(); } catch { /* ignore */ }
                }
            }
        }

        public static IReadOnlyList<int> CollectProcessTree(int rootPid)
        {
            if (rootPid <= 0)
                return Array.Empty<int>();

            var processes = EnumerateProcesses().ToList();
            var tree = new HashSet<int> { rootPid };
            var changed = true;

            while (changed)
            {
                changed = false;
                foreach (var info in processes)
                {
                    if (tree.Contains(info.ParentPid) && tree.Add(info.Pid))
                        changed = true;
                }
            }

            return tree.ToList();
        }

        public static bool IsAnyAlive(IEnumerable<int> pids)
        {
            foreach (var pid in pids)
            {
                try
                {
                    using var process = Process.GetProcessById(pid);
                    if (!process.HasExited)
                        return true;
                }
                catch
                {
                    // Treat missing processes as not alive.
                }
            }

            return false;
        }

        private static void KillPid(int pid, HashSet<int> killed)
        {
            if (!killed.Add(pid))
                return;

            try
            {
                using var process = Process.GetProcessById(pid);
                if (!process.HasExited)
                    process.Kill(entireProcessTree: false);
            }
            catch
            {
                // Ignore processes we cannot terminate.
            }
        }

        private static bool IsJavaExecutable(string name) =>
            name.Equals("java.exe", StringComparison.OrdinalIgnoreCase) ||
            name.Equals("javaw.exe", StringComparison.OrdinalIgnoreCase);

        private static bool LooksLikeMinecraft(string commandLine)
        {
            if (string.IsNullOrWhiteSpace(commandLine))
                return false;

            return MinecraftMarkers.Any(marker =>
                commandLine.Contains(marker, StringComparison.OrdinalIgnoreCase));
        }

        private static bool MatchesGameDirectory(string commandLine, IReadOnlyList<string> markers, string gameDirectory)
        {
            if (string.IsNullOrWhiteSpace(commandLine))
                return false;

            if (markers.Any(marker => commandLine.Contains(marker, StringComparison.OrdinalIgnoreCase)))
                return true;

            foreach (Match match in ArgFilePattern.Matches(commandLine))
            {
                var raw = match.Groups[1].Value.Trim('"');
                if (!File.Exists(raw))
                    continue;

                try
                {
                    var content = File.ReadAllText(raw);
                    if (markers.Any(marker => content.Contains(marker, StringComparison.OrdinalIgnoreCase)))
                        return true;
                }
                catch
                {
                    // Ignore unreadable arg files.
                }
            }

            return false;
        }

        private static List<string> BuildMarkers(string gameDirectory)
        {
            var fullPath = Path.GetFullPath(gameDirectory);
            var normalized = fullPath.Replace('\\', '/');
            var shortPath = TryGetShortPath(fullPath);
            var markers = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
            {
                fullPath,
                normalized,
                $"--gameDir {fullPath}",
                $"--gameDir {normalized}",
                $"--gameDir \"{fullPath}\"",
                $"--gameDir \"{normalized}\"",
                $"game_directory={fullPath}",
                $"game_directory={normalized}"
            };

            if (!string.IsNullOrWhiteSpace(shortPath))
            {
                markers.Add(shortPath);
                markers.Add(shortPath.Replace('\\', '/'));
            }

            var folderName = Path.GetFileName(fullPath.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar));
            if (!string.IsNullOrWhiteSpace(folderName))
            {
                markers.Add(folderName);
                markers.Add($"FlowVersions\\{folderName}");
                markers.Add($"FlowVersions/{folderName}");
                markers.Add($"FlowLauncher\\FlowVersions\\{folderName}");
                markers.Add($"FlowLauncher/FlowVersions/{folderName}");
            }

            return markers.Where(m => !string.IsNullOrWhiteSpace(m)).ToList();
        }

        private static string? TryGetShortPath(string path)
        {
            try
            {
                if (!Directory.Exists(path) && !File.Exists(path))
                    return null;

                var buffer = new char[512];
                var length = GetShortPathName(path, buffer, buffer.Length);
                if (length <= 0 || length >= buffer.Length)
                    return null;

                return new string(buffer, 0, length);
            }
            catch
            {
                return null;
            }
        }

        [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetShortPathName(string lpszLongPath, char[] lpszShortPath, int cchBuffer);

        private readonly record struct ProcessInfo(int Pid, int ParentPid, string Name, string CommandLine);

        private static IEnumerable<ProcessInfo> EnumerateProcesses()
        {
            if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                yield break;

            List<ProcessInfo> results;
            try
            {
                results = new List<ProcessInfo>();
                using var searcher = new ManagementObjectSearcher(
                    "SELECT ProcessId, ParentProcessId, Name, CommandLine FROM Win32_Process");

                foreach (var obj in searcher.Get())
                {
                    if (obj is not ManagementObject mo)
                        continue;

                    var pid = Convert.ToInt32(mo["ProcessId"]);
                    var parentPid = Convert.ToInt32(mo["ParentProcessId"]);
                    var name = mo["Name"]?.ToString() ?? "";
                    var commandLine = mo["CommandLine"]?.ToString() ?? "";
                    results.Add(new ProcessInfo(pid, parentPid, name, commandLine));
                }
            }
            catch
            {
                yield break;
            }

            foreach (var info in results)
                yield return info;
        }
    }
}
