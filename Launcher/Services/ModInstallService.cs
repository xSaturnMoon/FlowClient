using System.IO;
using Launcher.Helpers;
using Launcher.Models;
using Launcher.Services;

namespace Launcher.Services
{
    public sealed class ModInstallResult
    {
        public List<InstalledContent> Installed { get; init; } = new();
        public InstalledContent? MainMod { get; init; }
    }

    public sealed class ModInstallService
    {
        private readonly ModrinthService _modrinth = new();
        private readonly InstanceService _instances = new();

        public async Task<ModInstallResult> InstallModAsync(
            MinecraftInstance instance,
            string versionId,
            IProgress<string>? progress = null,
            CancellationToken ct = default)
        {
            var loader = instance.Loader == "Vanilla" ? null : instance.Loader;
            var plan = await BuildInstallPlanAsync(instance, versionId, loader, progress, ct);
            var modsDir = _instances.GetModsPath(instance.Id);
            Directory.CreateDirectory(modsDir);

            var installed = new List<InstalledContent>();
            foreach (var item in plan)
            {
                if (instance.Mods.Any(m => m.ProjectId == item.Project.Id))
                    continue;

                progress?.Report($"Downloading {item.Project.Title}…");

                var file = item.Version.Files.FirstOrDefault(f => f.Primary) ?? item.Version.Files.FirstOrDefault();
                if (file == null)
                    continue;

                var bytes = item.CachedBytes ?? await _modrinth.DownloadFileAsync(file.Url, ct);
                var dest = Path.Combine(modsDir, file.Filename);
                await File.WriteAllBytesAsync(dest, bytes, ct);

                var content = new InstalledContent
                {
                    ProjectId = item.Project.Id,
                    Slug = item.Project.Slug,
                    Title = item.Project.Title,
                    Author = "",
                    VersionId = item.Version.Id,
                    VersionNumber = item.Version.VersionNumber,
                    FileName = file.Filename,
                    IconUrl = item.Project.IconUrl ?? ""
                };

                instance.Mods.RemoveAll(m => m.ProjectId == content.ProjectId);
                instance.Mods.Add(content);
                installed.Add(content);
            }

            _instances.Save(instance);

            return new ModInstallResult
            {
                Installed = installed,
                MainMod = installed.FirstOrDefault(m => m.VersionId == versionId)
                    ?? installed.LastOrDefault()
            };
        }

        private async Task<List<InstallPlanItem>> BuildInstallPlanAsync(
            MinecraftInstance instance,
            string versionId,
            string? loader,
            IProgress<string>? progress,
            CancellationToken ct)
        {
            var result = new List<InstallPlanItem>();
            var visiting = new HashSet<string>(StringComparer.Ordinal);
            var visited = new HashSet<string>(StringComparer.Ordinal);
            var bytesCache = new Dictionary<string, byte[]>(StringComparer.Ordinal);

            async Task ResolveVersionAsync(string targetVersionId)
            {
                if (visited.Contains(targetVersionId))
                    return;
                if (!visiting.Add(targetVersionId))
                    return;

                var version = await _modrinth.GetVersionByIdAsync(targetVersionId, ct);
                if (version == null)
                {
                    visiting.Remove(targetVersionId);
                    return;
                }

                foreach (var dependency in version.Dependencies)
                {
                    if (!IsRequiredDependency(dependency))
                        continue;

                    await ResolveDependencyAsync(dependency);
                }

                var primaryFile = version.Files.FirstOrDefault(f => f.Primary) ?? version.Files.FirstOrDefault();
                if (primaryFile != null && !bytesCache.ContainsKey(version.Id))
                {
                    progress?.Report($"Checking dependencies for {version.Name}…");
                    var bytes = await _modrinth.DownloadFileAsync(primaryFile.Url, ct);
                    bytesCache[version.Id] = bytes;

                    foreach (var modId in FabricModDependsReader.ReadRequiredModIds(bytes))
                    {
                        await ResolveProjectSlugAsync(modId);
                    }
                }

                visiting.Remove(targetVersionId);

                if (!visited.Add(targetVersionId))
                    return;

                var project = await _modrinth.GetProjectAsync(version.ProjectId, ct);
                if (project == null)
                    return;

                result.Add(new InstallPlanItem(
                    project,
                    version,
                    bytesCache.TryGetValue(version.Id, out var cached) ? cached : null));
            }

            async Task ResolveDependencyAsync(ModrinthDependency dependency)
            {
                if (!string.IsNullOrWhiteSpace(dependency.ProjectId)
                    && instance.Mods.Any(m => m.ProjectId == dependency.ProjectId))
                {
                    return;
                }

                ModrinthVersion? dependencyVersion = null;
                if (!string.IsNullOrWhiteSpace(dependency.VersionId))
                {
                    dependencyVersion = await _modrinth.GetVersionByIdAsync(dependency.VersionId, ct);
                }
                else if (!string.IsNullOrWhiteSpace(dependency.ProjectId))
                {
                    var versions = await _modrinth.GetVersionsAsync(
                        dependency.ProjectId,
                        instance.MinecraftVersion,
                        loader,
                        ct);
                    dependencyVersion = versions.FirstOrDefault();
                }

                if (dependencyVersion != null)
                    await ResolveVersionAsync(dependencyVersion.Id);
            }

            async Task ResolveProjectSlugAsync(string slugOrId)
            {
                if (instance.Mods.Any(m =>
                        m.ProjectId.Equals(slugOrId, StringComparison.OrdinalIgnoreCase)
                        || m.Slug.Equals(slugOrId, StringComparison.OrdinalIgnoreCase)))
                {
                    return;
                }

                var project = await _modrinth.GetProjectAsync(slugOrId, ct);
                if (project == null)
                    return;

                if (instance.Mods.Any(m => m.ProjectId == project.Id))
                    return;

                var versions = await _modrinth.GetVersionsAsync(
                    project.Id,
                    instance.MinecraftVersion,
                    loader,
                    ct);
                var version = versions.FirstOrDefault();
                if (version != null)
                    await ResolveVersionAsync(version.Id);
            }

            await ResolveVersionAsync(versionId);
            return result;
        }

        private static bool IsRequiredDependency(ModrinthDependency dependency) =>
            dependency.DependencyType.Equals("required", StringComparison.OrdinalIgnoreCase)
            && string.IsNullOrWhiteSpace(dependency.FileName);

        private sealed record InstallPlanItem(ModrinthProject Project, ModrinthVersion Version, byte[]? CachedBytes);
    }
}
