using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace Launcher.Services
{
    public sealed class QuickConnectServer
    {
        public string Id { get; set; } = Guid.NewGuid().ToString("N");
        public string Name { get; set; } = "";
        public string Address { get; set; } = "";
        public string Host { get; set; } = "";
        public int Port { get; set; } = 25565;
        public DateTime AddedAt { get; set; } = DateTime.UtcNow;
    }

    public sealed class QuickConnectServerService
    {
        private static readonly Lazy<QuickConnectServerService> InstanceLazy =
            new(() => new QuickConnectServerService());

        public static QuickConnectServerService Instance => InstanceLazy.Value;

        private static readonly string StoragePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "quick_servers.json");

        private readonly object _lock = new();
        private readonly List<QuickConnectServer> _servers = new();

        public event Action? ServersChanged;

        private QuickConnectServerService()
        {
            Load();
        }

        public IReadOnlyList<QuickConnectServer> GetAll()
        {
            lock (_lock)
            {
                return _servers.OrderByDescending(s => s.AddedAt).ToList();
            }
        }

        public QuickConnectServer Add(string name, string address)
        {
            var (host, port) = MinecraftServerPingService.ParseAddress(address);
            var server = new QuickConnectServer
            {
                Id = Guid.NewGuid().ToString("N"),
                Name = string.IsNullOrWhiteSpace(name) ? host : name.Trim(),
                Address = port == 25565 ? host : $"{host}:{port}",
                Host = host,
                Port = port,
                AddedAt = DateTime.UtcNow
            };

            lock (_lock)
            {
                // Remove duplicate if same address
                _servers.RemoveAll(s => string.Equals(s.Address, server.Address, StringComparison.OrdinalIgnoreCase));
                _servers.Add(server);
                SaveInternal();
            }

            ServersChanged?.Invoke();
            return server;
        }

        public bool Remove(string id)
        {
            bool removed;
            lock (_lock)
            {
                removed = _servers.RemoveAll(s => s.Id == id) > 0;
                if (removed)
                {
                    SaveInternal();
                }
            }

            if (removed)
            {
                ServersChanged?.Invoke();
            }
            return removed;
        }

        private void Load()
        {
            lock (_lock)
            {
                try
                {
                    if (File.Exists(StoragePath))
                    {
                        var json = File.ReadAllText(StoragePath);
                        var list = JsonSerializer.Deserialize<List<QuickConnectServer>>(json);
                        if (list != null)
                        {
                            _servers.Clear();
                            _servers.AddRange(list);
                        }
                    }
                }
                catch
                {
                    // If file is corrupted or unreadable, start fresh
                }
            }
        }

        private void SaveInternal()
        {
            try
            {
                var dir = Path.GetDirectoryName(StoragePath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                var json = JsonSerializer.Serialize(_servers, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(StoragePath, json);
            }
            catch
            {
                // Ignore storage write issues
            }
        }
    }
}
