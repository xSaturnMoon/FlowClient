using System.IO;
using System.Text.Json;
using Launcher.Models;

namespace Launcher.Services
{
    public sealed class ServerFavoritesService
    {
        private static readonly Lazy<ServerFavoritesService> Shared = new(() => new ServerFavoritesService());

        private static readonly string FavoritesPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "server-favorites.json");

        private static readonly string MinecraftRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".minecraft");

        private static readonly string MinecraftServersPath = Path.Combine(MinecraftRoot, "servers.dat");

        private readonly Dictionary<string, ServerHistoryEntry> _catalog = new(StringComparer.OrdinalIgnoreCase);
        private readonly HashSet<string> _favoriteKeys = new(StringComparer.OrdinalIgnoreCase);
        private bool _seeded;

        public static ServerFavoritesService Instance => Shared.Value;

        private ServerFavoritesService()
        {
            LoadFavorites();
        }

        public IReadOnlyList<ServerHistoryEntry> GetFavorites(int count = 5)
        {
            SyncCatalog();

            return _catalog.Values
                .Where(e => _favoriteKeys.Contains(e.Key))
                .OrderBy(e => e.DisplayName, StringComparer.OrdinalIgnoreCase)
                .Take(count)
                .ToList();
        }

        public void ToggleFavorite(string host, int port)
        {
            if (string.IsNullOrWhiteSpace(host))
                return;

            SyncCatalog();
            var key = BuildKey(host, port);

            if (!_catalog.ContainsKey(key))
            {
                _catalog[key] = new ServerHistoryEntry
                {
                    Host = host.Trim(),
                    Port = port is > 0 and <= 65535 ? port : 25565,
                    Name = host.Trim()
                };
            }

            if (_favoriteKeys.Contains(key))
                _favoriteKeys.Remove(key);
            else
                _favoriteKeys.Add(key);

            SaveFavorites();
        }

        public bool IsFavorite(string host, int port) => _favoriteKeys.Contains(BuildKey(host, port));

        private void SyncCatalog()
        {
            ImportServersDat(MinecraftServersPath);
            ImportAllFlowServersDat();

            if (!_seeded && _favoriteKeys.Count == 0 && _catalog.Count > 0)
            {
                foreach (var key in _catalog.Keys)
                    _favoriteKeys.Add(key);
                SaveFavorites();
            }

            _seeded = true;
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
            var key = BuildKey(server.Host, server.Port);
            var iconPath = ServerIconCache.SaveIcon(server.Host, server.Port, server.IconPng)
                ?? ServerIconCache.GetIconPath(server.Host, server.Port);

            if (!_catalog.TryGetValue(key, out var entry))
            {
                _catalog[key] = new ServerHistoryEntry
                {
                    Host = server.Host,
                    Port = server.Port,
                    Name = server.Name,
                    IconPath = iconPath
                };
                return;
            }

            if (!string.IsNullOrWhiteSpace(server.Name))
                entry.Name = server.Name;
            if (iconPath != null)
                entry.IconPath = iconPath;
        }

        private void LoadFavorites()
        {
            try
            {
                if (!File.Exists(FavoritesPath))
                    return;

                var keys = JsonSerializer.Deserialize<List<string>>(File.ReadAllText(FavoritesPath));
                if (keys == null)
                    return;

                foreach (var key in keys)
                {
                    if (!string.IsNullOrWhiteSpace(key))
                        _favoriteKeys.Add(key);
                }
            }
            catch
            {
                _favoriteKeys.Clear();
            }
        }

        private void SaveFavorites()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(FavoritesPath)!);
                var json = JsonSerializer.Serialize(
                    _favoriteKeys.OrderBy(k => k).ToList(),
                    new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(FavoritesPath, json);
            }
            catch
            {
                // Ignore persistence errors.
            }
        }

        private static string BuildKey(string host, int port)
        {
            var resolvedPort = port is > 0 and <= 65535 ? port : 25565;
            return $"{host.Trim().ToLowerInvariant()}:{resolvedPort}";
        }
    }
}
