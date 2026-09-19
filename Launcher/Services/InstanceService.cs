using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using Launcher.Helpers;
using Launcher.Models;

namespace Launcher.Services
{
    public class InstanceService
    {
        private static readonly object Sync = new();
        private static List<MinecraftInstance> _instances = new();
        private static bool _loaded;

        private static readonly string AppRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher");

        private static readonly string VersionsRoot = Path.Combine(AppRoot, "FlowVersions");
        private static readonly string IndexPath = Path.Combine(AppRoot, "FlowVersions.json");

        private static readonly string LegacyInstancesRoot = Path.Combine(AppRoot, "instances");
        private static readonly string LegacyIndexPath = Path.Combine(AppRoot, "instances.json");

        public static event Action? InstancesChanged;

        public InstanceService() => EnsureLoaded();

        private InstanceService(bool loading) { }

        public void Reload()
        {
            lock (Sync)
            {
                _loaded = false;
                EnsureLoaded();
            }
            InstancesChanged?.Invoke();
        }

        private static void EnsureLoaded()
        {
            lock (Sync)
            {
                if (_loaded) return;
                new InstanceService(loading: true).Load();
                _loaded = true;
            }
        }

        public IReadOnlyList<MinecraftInstance> GetAll()
        {
            EnsureLoaded();
            return _instances;
        }

        public MinecraftInstance? GetById(string id)
        {
            EnsureLoaded();
            return _instances.FirstOrDefault(i => i.Id == id);
        }

        public string GetInstanceDirectory(string id) =>
            Path.Combine(VersionsRoot, id);

        public void Save(MinecraftInstance instance)
        {
            EnsureLoaded();
            Directory.CreateDirectory(GetInstanceDirectory(instance.Id));
            Directory.CreateDirectory(Path.Combine(GetInstanceDirectory(instance.Id), "mods"));
            Directory.CreateDirectory(Path.Combine(GetInstanceDirectory(instance.Id), "resourcepacks"));
            Directory.CreateDirectory(Path.Combine(GetInstanceDirectory(instance.Id), "shaderpacks"));
            Directory.CreateDirectory(Path.Combine(GetInstanceDirectory(instance.Id), "saves"));

            var idx = _instances.FindIndex(i => i.Id == instance.Id);
            if (idx >= 0) _instances[idx] = instance;
            else _instances.Add(instance);

            File.WriteAllText(
                Path.Combine(GetInstanceDirectory(instance.Id), "instance.json"),
                JsonSerializer.Serialize(instance, new JsonSerializerOptions { WriteIndented = true }));

            SaveIndex();
            InstancesChanged?.Invoke();
        }

        public MinecraftInstance Create(string name, string version, string loader)
        {
            var id = AllocateUniqueId(version, loader);
            var instance = new MinecraftInstance
            {
                Id = id,
                Name = name,
                MinecraftVersion = version,
                Loader = loader,
                IconLetter = string.IsNullOrEmpty(loader) ? "V" : loader[..1].ToUpper(),
                CreatedAt = DateTime.UtcNow
            };
            Save(instance);
            return instance;
        }

        public MinecraftInstance Duplicate(MinecraftInstance source)
        {
            var copy = JsonSerializer.Deserialize<MinecraftInstance>(
                JsonSerializer.Serialize(source))!;
            copy.Id = AllocateUniqueId(source.MinecraftVersion, source.Loader);
            copy.Name = $"{source.Name} (copy)";
            copy.CreatedAt = DateTime.UtcNow;
            copy.LastPlayedAt = null;

            Save(copy);
            CopyDirectory(GetInstanceDirectory(source.Id), GetInstanceDirectory(copy.Id));
            return copy;
        }

        public void Delete(string id)
        {
            _instances.RemoveAll(i => i.Id == id);
            SaveIndex();
            try
            {
                var dir = GetInstanceDirectory(id);
                if (Directory.Exists(dir)) Directory.Delete(dir, true);
            }
            catch { }
            InstancesChanged?.Invoke();
        }

        public void ToggleFavorite(string id)
        {
            var inst = GetById(id);
            if (inst == null) return;
            inst.IsFavorite = !inst.IsFavorite;
            Save(inst);
        }

        public void MarkPlayed(string id)
        {
            var inst = GetById(id);
            if (inst == null) return;
            inst.LastPlayedAt = DateTime.UtcNow;
            Save(inst);
        }

        private static readonly ConcurrentDictionary<string, (string size, DateTime expires)> _sizeCache = new();

        public string GetFolderSizeText(string id)
        {
            if (_sizeCache.TryGetValue(id, out var cached) && cached.expires > DateTime.UtcNow)
                return cached.size;

            try
            {
                var dir = GetInstanceDirectory(id);
                if (!Directory.Exists(dir)) return "—";
                long bytes = Directory.EnumerateFiles(dir, "*", SearchOption.AllDirectories).Sum(f => new FileInfo(f).Length);
                string res;
                if (bytes < 1024) res = $"{bytes} B";
                else if (bytes < 1024 * 1024) res = $"{bytes / 1024.0:0.#} KB";
                else if (bytes < 1024 * 1024 * 1024) res = $"{bytes / (1024.0 * 1024):0.#} MB";
                else res = $"{bytes / (1024.0 * 1024 * 1024):0.#} GB";

                _sizeCache[id] = (res, DateTime.UtcNow.AddMinutes(5));
                return res;
            }
            catch { return "—"; }
        }

        public string GetCachedFolderSizeText(string id)
        {
            if (_sizeCache.TryGetValue(id, out var cached) && cached.expires > DateTime.UtcNow)
                return cached.size;
            return "—";
        }

        public string GetModsPath(string id) => Path.Combine(GetInstanceDirectory(id), "mods");
        public string GetResourcePacksPath(string id) => Path.Combine(GetInstanceDirectory(id), "resourcepacks");
        public string GetShadersPath(string id) => Path.Combine(GetInstanceDirectory(id), "shaderpacks");
        public string GetSavesPath(string id) => Path.Combine(GetInstanceDirectory(id), "saves");

        public void SyncModsFromDisk(MinecraftInstance instance)
        {
            var modsDir = GetModsPath(instance.Id);
            if (!Directory.Exists(modsDir))
                return;

            var changed = false;
            foreach (var path in Directory.GetFiles(modsDir, "*.jar"))
            {
                var fileName = Path.GetFileName(path);
                if (instance.Mods.Any(m => m.FileName.Equals(fileName, StringComparison.OrdinalIgnoreCase)))
                    continue;

                instance.Mods.Add(new InstalledContent
                {
                    Title = Path.GetFileNameWithoutExtension(fileName),
                    FileName = fileName,
                    Enabled = true
                });
                changed = true;
            }

            foreach (var path in Directory.GetFiles(modsDir, "*.jar.disabled"))
            {
                var fileName = Path.GetFileName(path);
                var enabledName = fileName[..^".disabled".Length];
                if (instance.Mods.Any(m => m.FileName.Equals(enabledName, StringComparison.OrdinalIgnoreCase)))
                    continue;

                instance.Mods.Add(new InstalledContent
                {
                    Title = Path.GetFileNameWithoutExtension(enabledName),
                    FileName = enabledName,
                    Enabled = false
                });
                changed = true;
            }

            if (changed)
                Save(instance);
        }

        private string AllocateUniqueId(string version, string loader)
        {
            var baseId = LoaderBranding.BuildFolderId(version, loader);
            var id = baseId;
            var n = 2;
            while (_instances.Any(i => i.Id == id) || Directory.Exists(GetInstanceDirectory(id)))
                id = $"{baseId}-{n++}";
            return id;
        }

        private void Load()
        {
            _instances.Clear();
            Directory.CreateDirectory(VersionsRoot);

            try
            {
                if (File.Exists(IndexPath))
                {
                    var list = JsonSerializer.Deserialize<List<MinecraftInstance>>(File.ReadAllText(IndexPath));
                    if (list != null) _instances = list;
                }
                else if (File.Exists(LegacyIndexPath))
                {
                    var list = JsonSerializer.Deserialize<List<MinecraftInstance>>(File.ReadAllText(LegacyIndexPath));
                    if (list != null) _instances = list;
                }
            }
            catch { _instances = new(); }

            foreach (var inst in _instances.ToList())
            {
                var path = Path.Combine(GetInstanceDirectory(inst.Id), "instance.json");
                if (!File.Exists(path))
                {
                    var legacyPath = Path.Combine(LegacyInstancesRoot, inst.Id, "instance.json");
                    if (File.Exists(legacyPath)) path = legacyPath;
                    else continue;
                }
                try
                {
                    var loaded = JsonSerializer.Deserialize<MinecraftInstance>(File.ReadAllText(path));
                    if (loaded != null)
                    {
                        var idx = _instances.FindIndex(i => i.Id == inst.Id);
                        if (idx >= 0) _instances[idx] = loaded;
                    }
                }
                catch { }
            }

            MigrateLegacyStorage();
            SaveIndex();
        }

        private void MigrateLegacyStorage()
        {
            Directory.CreateDirectory(VersionsRoot);
            var changed = false;

            foreach (var inst in _instances.ToList())
            {
                var legacyDir = Path.Combine(LegacyInstancesRoot, inst.Id);
                var currentDir = GetInstanceDirectory(inst.Id);

                if (LoaderBranding.IsLegacyGuidId(inst.Id))
                {
                    var newId = AllocateUniqueId(inst.MinecraftVersion, inst.Loader);
                    var newDir = GetInstanceDirectory(newId);

                    if (Directory.Exists(legacyDir) && !Directory.Exists(newDir))
                        Directory.Move(legacyDir, newDir);
                    else if (Directory.Exists(currentDir) && currentDir != newDir && !Directory.Exists(newDir))
                        Directory.Move(currentDir, newDir);

                    inst.Id = newId;
                    Save(inst);
                    changed = true;
                    continue;
                }

                if (Directory.Exists(legacyDir) && !Directory.Exists(currentDir))
                {
                    Directory.Move(legacyDir, currentDir);
                    changed = true;
                }
            }

            if (!Directory.Exists(LegacyInstancesRoot)) return;

            foreach (var dir in Directory.GetDirectories(LegacyInstancesRoot))
            {
                var folderName = Path.GetFileName(dir);
                var jsonPath = Path.Combine(dir, "instance.json");
                if (!File.Exists(jsonPath)) continue;

                try
                {
                    var inst = JsonSerializer.Deserialize<MinecraftInstance>(File.ReadAllText(jsonPath));
                    if (inst == null) continue;

                    if (LoaderBranding.IsLegacyGuidId(folderName))
                    {
                        var newId = AllocateUniqueId(inst.MinecraftVersion, inst.Loader);
                        var target = GetInstanceDirectory(newId);
                        if (!Directory.Exists(target))
                            Directory.Move(dir, target);
                        inst.Id = newId;
                        if (_instances.All(i => i.Id != newId))
                            _instances.Add(inst);
                        Save(inst);
                        changed = true;
                    }
                    else
                    {
                        var target = Path.Combine(VersionsRoot, folderName);
                        if (!Directory.Exists(target))
                            Directory.Move(dir, target);
                        changed = true;
                    }
                }
                catch { }
            }

            if (changed) SaveIndex();
        }

        private void SaveIndex()
        {
            try
            {
                Directory.CreateDirectory(AppRoot);
                File.WriteAllText(IndexPath, JsonSerializer.Serialize(_instances,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }

        private static void CopyDirectory(string src, string dst)
        {
            if (!Directory.Exists(src)) return;
            Directory.CreateDirectory(dst);
            foreach (var file in Directory.GetFiles(src, "*", SearchOption.AllDirectories))
            {
                var rel = Path.GetRelativePath(src, file);
                if (rel.Equals("instance.json", StringComparison.OrdinalIgnoreCase)) continue;
                var target = Path.Combine(dst, rel);
                Directory.CreateDirectory(Path.GetDirectoryName(target)!);
                File.Copy(file, target, true);
            }
        }
    }
}
