using System.IO;
using System.Text.Json;
using System.Text.RegularExpressions;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class ServerHistoryService
    {
        private static readonly Lazy<ServerHistoryService> Shared = new(() => new ServerHistoryService());

        private static readonly string HistoryPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "server-history.json");

        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private static readonly string MinecraftServersPath = Path.Combine(MinecraftRoot, "servers.dat");

        private static readonly Regex ConnectRegex = new(
            @"Connecting to ([^,\s]+), (\d+)",
            RegexOptions.Compiled | RegexOptions.CultureInvariant);

        private readonly Dictionary<string, ServerHistoryEntry> _entries = new(StringComparer.OrdinalIgnoreCase);
        private bool _externalSynced;

        public static ServerHistoryService Instance => Shared.Value;

        private ServerHistoryService()
        {
            Load();
        }

        public IReadOnlyList<ServerHistoryEntry> GetTopServers(int count = 5)
        {
            ImportServersDat(MinecraftServersPath);
            ImportAllFlowServersDat();

            if (!_externalSynced)
            {
                _externalSynced = true;
                ImportFromLatestLog();
            }

            Save();
            return _entries.Values
                .OrderByDescending(e => e.PlayCount)
                .ThenByDescending(e => e.LastPlayedAt ?? DateTime.MinValue)
                .ThenBy(e => e.DisplayName, StringComparer.OrdinalIgnoreCase)
                .Take(count)
                .ToList();
        }

        public void RecordLaunch(string host, int port, string? name = null)
        {
            if (string.IsNullOrWhiteSpace(host))
                return;

            host = host.Trim();
            port = port is > 0 and <= 65535 ? port : 25565;

            var key = $"{host.ToLowerInvariant()}:{port}";
            if (!_entries.TryGetValue(key, out var entry))
            {
                entry = new ServerHistoryEntry
                {
                    Host = host,
                    Port = port,
                    Name = string.IsNullOrWhiteSpace(name) ? host : name.Trim()
                };
                _entries[key] = entry;
            }

            if (!string.IsNullOrWhiteSpace(name))
                entry.Name = name.Trim();

            entry.PlayCount++;
            entry.LastPlayedAt = DateTime.UtcNow;
            Save();
        }

        private void ImportServersDat(string? path)
        {
            if (string.IsNullOrWhiteSpace(path) || !File.Exists(path))
                return;

            foreach (var server in ServersDatReader.Read(path))
                MergeServer(server);
        }

        private void ImportAllFlowServersDat()
        {
            var flowVersions = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "FlowLauncher", "FlowVersions");

            if (!Directory.Exists(flowVersions))
                return;

            foreach (var dir in Directory.GetDirectories(flowVersions))
                ImportServersDat(Path.Combine(dir, "servers.dat"));
        }

        private void MergeServer(SavedServerEntry server)
        {
            var key = $"{server.Host.ToLowerInvariant()}:{server.Port}";
            var iconPath = ServerIconCache.SaveIcon(server.Host, server.Port, server.IconPng)
                ?? ServerIconCache.GetIconPath(server.Host, server.Port);

            if (!_entries.TryGetValue(key, out var entry))
            {
                entry = new ServerHistoryEntry
                {
                    Host = server.Host,
                    Port = server.Port,
                    Name = server.Name,
                    PlayCount = 0,
                    IconPath = iconPath
                };
                _entries[key] = entry;
                return;
            }

            if (!string.IsNullOrWhiteSpace(server.Name))
                entry.Name = server.Name;
            if (iconPath != null)
                entry.IconPath = iconPath;
        }

        private static IEnumerable<string> ReadTailLines(string path, int maxLines)
        {
            var queue = new Queue<string>(maxLines);
            foreach (var line in File.ReadLines(path))
            {
                if (queue.Count == maxLines)
                    queue.Dequeue();
                queue.Enqueue(line);
            }

            return queue;
        }

        private void ImportFromLatestLog()
        {
            var latest = Path.Combine(MinecraftRoot, "logs", "latest.log");
            if (!File.Exists(latest))
                return;

            try
            {
                var info = new FileInfo(latest);
                if (info.Length > 1_500_000)
                    return;

                foreach (var line in ReadTailLines(latest, 400))
                    TryImportConnectLine(line, info.LastWriteTimeUtc);
            }
            catch
            {
                // Ignore log parsing issues.
            }
        }

        private void TryImportConnectLine(string line, DateTime seenAt)
        {
            var match = ConnectRegex.Match(line);
            if (!match.Success)
                return;

            var host = match.Groups[1].Value.Trim();
            if (!int.TryParse(match.Groups[2].Value, out var port))
                port = 25565;

            var key = $"{host.ToLowerInvariant()}:{port}";
            if (!_entries.TryGetValue(key, out var entry))
            {
                entry = new ServerHistoryEntry
                {
                    Host = host,
                    Port = port,
                    Name = host
                };
                _entries[key] = entry;
            }

            entry.PlayCount++;
            entry.LastPlayedAt = seenAt;
        }

        private void Load()
        {
            try
            {
                if (!File.Exists(HistoryPath))
                    return;

                var list = JsonSerializer.Deserialize<List<ServerHistoryEntry>>(File.ReadAllText(HistoryPath));
                if (list == null)
                    return;

                _entries.Clear();
                foreach (var entry in list)
                {
                    if (string.IsNullOrWhiteSpace(entry.Host))
                        continue;

                    entry.Port = entry.Port is > 0 and <= 65535 ? entry.Port : 25565;
                    entry.IconPath ??= ServerIconCache.GetIconPath(entry.Host, entry.Port);
                    _entries[entry.Key] = entry;
                }
            }
            catch
            {
                _entries.Clear();
            }
        }

        private void Save()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(HistoryPath)!);
                var json = JsonSerializer.Serialize(
                    _entries.Values.OrderByDescending(e => e.PlayCount).ToList(),
                    new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(HistoryPath, json);
            }
            catch
            {
                // Ignore persistence errors.
            }
        }
    }
}
