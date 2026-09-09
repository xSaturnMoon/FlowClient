using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace Launcher.Services
{
    public sealed class GameLogSource
    {
        public string Id { get; init; } = "";
        public string Label { get; init; } = "";
        public string GameDirectory { get; init; } = "";
    }

    public sealed class LatestLogEntry
    {
        public GameLogSource Source { get; init; } = null!;
        public string Path { get; init; } = "";
        public DateTime ModifiedUtc { get; init; }

        public string ModifiedText => ModifiedUtc.ToLocalTime().ToString("dd MMM yyyy · HH:mm");
    }

    public sealed class CrashReportInfo
    {
        public string FileName { get; init; } = "";
        public string FullPath { get; init; } = "";
        public DateTime ModifiedUtc { get; init; }
        public string GameDirectory { get; init; } = "";
        public string SourceLabel { get; init; } = "";

        public string Subtitle => ModifiedUtc.ToLocalTime().ToString("dd MMM yyyy · HH:mm");
    }

    public sealed class GameLogService
    {
        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private readonly InstanceService _instances = new();

        public IReadOnlyList<GameLogSource> GetSources()
        {
            var sources = new List<GameLogSource>
            {
                new()
                {
                    Id = "vanilla",
                    Label = ".minecraft (default)",
                    GameDirectory = MinecraftRoot
                }
            };

            foreach (var inst in _instances.GetAll())
            {
                var dir = _instances.GetInstanceDirectory(inst.Id);
                sources.Add(new GameLogSource
                {
                    Id = inst.Id,
                    Label = $"{inst.Name} · {inst.MinecraftVersion}",
                    GameDirectory = dir
                });
            }

            return sources;
        }

        public LatestLogEntry? FindMostRecentLatestLog()
        {
            LatestLogEntry? best = null;
            foreach (var source in GetSources())
            {
                var path = GetLatestLogPath(source.GameDirectory);
                if (path == null)
                    continue;

                DateTime modified;
                try
                {
                    modified = File.GetLastWriteTimeUtc(path);
                }
                catch
                {
                    continue;
                }

                if (best == null || modified > best.ModifiedUtc)
                {
                    best = new LatestLogEntry
                    {
                        Source = source,
                        Path = path,
                        ModifiedUtc = modified
                    };
                }
            }

            return best;
        }

        public string? GetLatestLogPath(string gameDirectory)
        {
            var path = Path.Combine(gameDirectory, "logs", "latest.log");
            return File.Exists(path) ? path : null;
        }

        public string ReadLatestLogTail(string path, int maxLines = 500)
        {
            if (!File.Exists(path))
                return "No latest.log found.";

            try
            {
                return ReadTail(path, maxLines);
            }
            catch (Exception ex)
            {
                return $"Could not read latest.log:\n{ex.Message}";
            }
        }

        public IReadOnlyList<CrashReportInfo> GetRecentCrashReports(int max = 3)
        {
            var reports = new List<CrashReportInfo>();
            foreach (var source in GetSources())
                reports.AddRange(GetCrashReports(source.GameDirectory, source.Label, maxPerSource: 20));

            return reports
                .OrderByDescending(r => r.ModifiedUtc)
                .Take(max)
                .ToList();
        }

        public CrashReportInfo? GetMostRecentCrashReport() =>
            GetRecentCrashReports(1).FirstOrDefault();

        private IReadOnlyList<CrashReportInfo> GetCrashReports(string gameDirectory, string sourceLabel, int maxPerSource)
        {
            var dir = Path.Combine(gameDirectory, "crash-reports");
            if (!Directory.Exists(dir))
                return Array.Empty<CrashReportInfo>();

            try
            {
                return Directory.EnumerateFiles(dir, "*.txt")
                    .Select(path => new FileInfo(path))
                    .OrderByDescending(f => f.LastWriteTimeUtc)
                    .Take(maxPerSource)
                    .Select(f => new CrashReportInfo
                    {
                        FileName = f.Name,
                        FullPath = f.FullName,
                        ModifiedUtc = f.LastWriteTimeUtc,
                        GameDirectory = gameDirectory,
                        SourceLabel = sourceLabel
                    })
                    .ToList();
            }
            catch
            {
                return Array.Empty<CrashReportInfo>();
            }
        }

        public string ReadCrashReport(string path)
        {
            if (!File.Exists(path))
                return "Crash report not found.";

            try
            {
                using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
                using var reader = new StreamReader(stream);
                return reader.ReadToEnd();
            }
            catch (Exception ex)
            {
                return $"Could not read crash report:\n{ex.Message}";
            }
        }

        private static string ReadTail(string path, int maxLines)
        {
            using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var reader = new StreamReader(stream);

            var ring = new Queue<string>(maxLines);
            string? line;
            while ((line = reader.ReadLine()) != null)
            {
                if (ring.Count >= maxLines)
                    ring.Dequeue();
                ring.Enqueue(line);
            }

            return ring.Count == 0
                ? "(empty log file)"
                : string.Join(Environment.NewLine, ring);
        }
    }
}
