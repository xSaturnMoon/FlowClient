using System.IO;
using System.Text.Json;
using Launcher.Models;

namespace Launcher.Services
{
    public enum GlobalContentKind
    {
        ResourcePack,
        Shader
    }

    public sealed class GlobalContentStore
    {
        public List<InstalledContent> ResourcePacks { get; set; } = new();
        public List<InstalledContent> Shaders { get; set; } = new();
    }

    public sealed class GlobalContentService
    {
        private static readonly string AppRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher");

        private static readonly string GlobalRoot = Path.Combine(AppRoot, "global");
        private static readonly string IndexPath = Path.Combine(AppRoot, "global-content.json");

        private static readonly JsonSerializerOptions JsonOptions = new()
        {
            WriteIndented = true
        };

        public static string ResourcePacksDirectory => Path.Combine(GlobalRoot, "resourcepacks");
        public static string ShadersDirectory => Path.Combine(GlobalRoot, "shaderpacks");

        public GlobalContentStore Load()
        {
            Directory.CreateDirectory(ResourcePacksDirectory);
            Directory.CreateDirectory(ShadersDirectory);

            var store = LoadIndex();
            if (SyncFromDisk(store))
                Save(store);

            return store;
        }

        public void Save(GlobalContentStore store)
        {
            Directory.CreateDirectory(GlobalRoot);
            File.WriteAllText(IndexPath, JsonSerializer.Serialize(store, JsonOptions));
        }

        public string GetDirectory(GlobalContentKind kind) =>
            kind == GlobalContentKind.ResourcePack ? ResourcePacksDirectory : ShadersDirectory;

        public List<InstalledContent> GetList(GlobalContentStore store, GlobalContentKind kind) =>
            kind == GlobalContentKind.ResourcePack ? store.ResourcePacks : store.Shaders;

        public bool SyncFromDisk(GlobalContentStore store)
        {
            var changed = SyncKindFromDisk(store.ResourcePacks, ResourcePacksDirectory);
            changed |= SyncKindFromDisk(store.Shaders, ShadersDirectory);
            return changed;
        }

        public void SyncToGameDirectory(string gameDirectory)
        {
            MirrorDirectory(ResourcePacksDirectory, Path.Combine(gameDirectory, "resourcepacks"));
            MirrorDirectory(ShadersDirectory, Path.Combine(gameDirectory, "shaderpacks"));
        }

        public void SyncToAllInstanceDirectories(InstanceService instances)
        {
            foreach (var inst in instances.GetAll())
                SyncToGameDirectory(instances.GetInstanceDirectory(inst.Id));

            SyncToGameDirectory(Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                ".minecraft"));
        }

        public void RemoveFileFromAllInstances(InstanceService instances, GlobalContentKind kind, string entryName)
        {
            foreach (var inst in instances.GetAll())
            {
                var dir = kind == GlobalContentKind.ResourcePack
                    ? instances.GetResourcePacksPath(inst.Id)
                    : instances.GetShadersPath(inst.Id);
                TryDeleteEntry(Path.Combine(dir, entryName));
            }

            var subDir = kind == GlobalContentKind.ResourcePack ? "resourcepacks" : "shaderpacks";
            TryDeleteEntry(Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                ".minecraft",
                subDir,
                entryName));
        }

        private GlobalContentStore LoadIndex()
        {
            if (!File.Exists(IndexPath))
                return new GlobalContentStore();

            try
            {
                var json = File.ReadAllText(IndexPath);
                return JsonSerializer.Deserialize<GlobalContentStore>(json) ?? new GlobalContentStore();
            }
            catch
            {
                return new GlobalContentStore();
            }
        }

        private static bool SyncKindFromDisk(List<InstalledContent> list, string directory)
        {
            if (!Directory.Exists(directory))
                return false;

            var found = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var changed = false;

            foreach (var file in Directory.EnumerateFiles(directory))
            {
                var name = Path.GetFileName(file);
                if (string.IsNullOrEmpty(name) || name.StartsWith('.'))
                    continue;
                if (!IsPackArchive(name))
                    continue;

                found.Add(name);
                if (UpsertDiscoveredEntry(list, name, Path.GetFileNameWithoutExtension(name), File.GetLastWriteTimeUtc(file)))
                    changed = true;
            }

            foreach (var dir in Directory.EnumerateDirectories(directory))
            {
                var name = Path.GetFileName(dir);
                if (string.IsNullOrEmpty(name) || name.StartsWith('.'))
                    continue;
                if (!LooksLikePackDirectory(dir))
                    continue;

                found.Add(name);
                if (UpsertDiscoveredEntry(list, name, name, Directory.GetLastWriteTimeUtc(dir)))
                    changed = true;
            }

            var removed = list.RemoveAll(x => !found.Contains(x.FileName));
            return changed || removed > 0;
        }

        private static bool UpsertDiscoveredEntry(
            List<InstalledContent> list,
            string fileName,
            string title,
            DateTime installedAt)
        {
            var existing = list.FirstOrDefault(x =>
                string.Equals(x.FileName, fileName, StringComparison.OrdinalIgnoreCase));

            if (existing != null)
            {
                if (string.IsNullOrWhiteSpace(existing.Title))
                {
                    existing.Title = title;
                    return true;
                }

                return false;
            }

            list.Add(new InstalledContent
            {
                Title = title,
                FileName = fileName,
                InstalledAt = installedAt
            });
            return true;
        }

        private static bool IsPackArchive(string fileName)
        {
            var ext = Path.GetExtension(fileName);
            return ext.Equals(".zip", StringComparison.OrdinalIgnoreCase)
                || ext.Equals(".jar", StringComparison.OrdinalIgnoreCase);
        }

        private static bool LooksLikePackDirectory(string path)
        {
            if (File.Exists(Path.Combine(path, "pack.mcmeta")))
                return true;

            if (Directory.Exists(Path.Combine(path, "assets")))
                return true;

            return Directory.Exists(Path.Combine(path, "shaders"));
        }

        private static void TryDeleteEntry(string path)
        {
            try
            {
                if (File.Exists(path))
                    File.Delete(path);
                else if (Directory.Exists(path))
                    Directory.Delete(path, recursive: true);
            }
            catch { }
        }

        private static void MirrorDirectory(string sourceDirectory, string targetDirectory)
        {
            if (!Directory.Exists(sourceDirectory))
                return;

            Directory.CreateDirectory(targetDirectory);

            foreach (var file in Directory.EnumerateFiles(sourceDirectory))
            {
                var name = Path.GetFileName(file);
                if (string.IsNullOrEmpty(name) || name.StartsWith('.'))
                    continue;

                File.Copy(file, Path.Combine(targetDirectory, name), overwrite: true);
            }

            foreach (var subdir in Directory.EnumerateDirectories(sourceDirectory))
            {
                var name = Path.GetFileName(subdir);
                if (string.IsNullOrEmpty(name) || name.StartsWith('.'))
                    continue;

                MirrorDirectory(subdir, Path.Combine(targetDirectory, name));
            }
        }
    }
}

