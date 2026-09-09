using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;

namespace Launcher.Services
{
    public class ModrinthSearchHit
    {
        [JsonPropertyName("project_id")] public string ProjectId { get; set; } = "";
        [JsonPropertyName("slug")] public string Slug { get; set; } = "";
        [JsonPropertyName("title")] public string Title { get; set; } = "";
        [JsonPropertyName("description")] public string Description { get; set; } = "";
        [JsonPropertyName("author")] public string Author { get; set; } = "";
        [JsonPropertyName("icon_url")] public string? IconUrl { get; set; }
        [JsonPropertyName("downloads")] public long Downloads { get; set; }
        [JsonPropertyName("project_type")] public string ProjectType { get; set; } = "";
        [JsonPropertyName("categories")] public List<string> Categories { get; set; } = new();
    }

    public class ModrinthProject
    {
        [JsonPropertyName("id")] public string Id { get; set; } = "";
        [JsonPropertyName("slug")] public string Slug { get; set; } = "";
        [JsonPropertyName("title")] public string Title { get; set; } = "";
        [JsonPropertyName("description")] public string Description { get; set; } = "";
        [JsonPropertyName("body")] public string Body { get; set; } = "";
        [JsonPropertyName("icon_url")] public string? IconUrl { get; set; }
        [JsonPropertyName("downloads")] public long Downloads { get; set; }
        [JsonPropertyName("followers")] public long Followers { get; set; }
        [JsonPropertyName("project_type")] public string ProjectType { get; set; } = "";
        [JsonPropertyName("categories")] public List<string> Categories { get; set; } = new();
        [JsonPropertyName("game_versions")] public List<string> GameVersions { get; set; } = new();
        [JsonPropertyName("issues_url")] public string? IssuesUrl { get; set; }
        [JsonPropertyName("source_url")] public string? SourceUrl { get; set; }
        [JsonPropertyName("wiki_url")] public string? WikiUrl { get; set; }
        [JsonPropertyName("discord_url")] public string? DiscordUrl { get; set; }
    }

    public class ModrinthVersion
    {
        [JsonPropertyName("id")] public string Id { get; set; } = "";
        [JsonPropertyName("project_id")] public string ProjectId { get; set; } = "";
        [JsonPropertyName("name")] public string Name { get; set; } = "";
        [JsonPropertyName("version_number")] public string VersionNumber { get; set; } = "";
        [JsonPropertyName("changelog")] public string? Changelog { get; set; }
        [JsonPropertyName("game_versions")] public List<string> GameVersions { get; set; } = new();
        [JsonPropertyName("loaders")] public List<string> Loaders { get; set; } = new();
        [JsonPropertyName("files")] public List<ModrinthFile> Files { get; set; } = new();
        [JsonPropertyName("dependencies")] public List<ModrinthDependency> Dependencies { get; set; } = new();
    }

    public class ModrinthDependency
    {
        [JsonPropertyName("version_id")] public string? VersionId { get; set; }
        [JsonPropertyName("project_id")] public string? ProjectId { get; set; }
        [JsonPropertyName("file_name")] public string? FileName { get; set; }
        [JsonPropertyName("dependency_type")] public string DependencyType { get; set; } = "";
    }

    public class ModrinthFile
    {
        [JsonPropertyName("url")] public string Url { get; set; } = "";
        [JsonPropertyName("filename")] public string Filename { get; set; } = "";
        [JsonPropertyName("primary")] public bool Primary { get; set; }
    }

    public class ModrinthService
    {
        private static readonly HttpClient Http = new()
        {
            BaseAddress = new Uri("https://api.modrinth.com/v2/"),
            Timeout = TimeSpan.FromSeconds(30)
        };

        static ModrinthService()
        {
            Http.DefaultRequestHeaders.UserAgent.ParseAdd("FlowLauncher/1.0 (flow-client)");
        }

        public async Task<List<ModrinthSearchHit>> SearchAsync(
            string query,
            string projectType,
            string? mcVersion,
            string? loader,
            int limit = 20,
            CancellationToken ct = default)
        {
            var facets = BuildFacets(projectType, mcVersion, loader);
            var url = $"search?query={Uri.EscapeDataString(query)}&limit={limit}&index=relevance&facets={Uri.EscapeDataString(facets)}";
            var json = await Http.GetStringAsync(url, ct);
            using var doc = JsonDocument.Parse(json);
            var hits = new List<ModrinthSearchHit>();
            if (!doc.RootElement.TryGetProperty("hits", out var arr)) return hits;
            foreach (var el in arr.EnumerateArray())
            {
                var hit = JsonSerializer.Deserialize<ModrinthSearchHit>(el.GetRawText());
                if (hit != null) hits.Add(hit);
            }
            return hits;
        }

        public async Task<ModrinthProject?> GetProjectAsync(string idOrSlug, CancellationToken ct = default)
        {
            try
            {
                var json = await Http.GetStringAsync($"project/{idOrSlug}", ct);
                return JsonSerializer.Deserialize<ModrinthProject>(json);
            }
            catch { return null; }
        }

        public async Task<ModrinthVersion?> GetVersionByIdAsync(string versionId, CancellationToken ct = default)
        {
            try
            {
                var json = await Http.GetStringAsync($"version/{versionId}", ct);
                return JsonSerializer.Deserialize<ModrinthVersion>(json);
            }
            catch { return null; }
        }

        public async Task<List<ModrinthVersion>> GetVersionsAsync(
            string projectId,
            string? mcVersion,
            string? loader,
            CancellationToken ct = default)
        {
            var loaders = MapLoader(loader);
            var query = $"project/{projectId}/version";
            var parts = new List<string>();
            if (!string.IsNullOrEmpty(mcVersion))
                parts.Add($"game_versions={Uri.EscapeDataString($"[\"{mcVersion}\"]")}");
            if (!string.IsNullOrEmpty(loaders))
                parts.Add($"loaders={Uri.EscapeDataString($"[\"{loaders}\"]")}");
            if (parts.Count > 0) query += "?" + string.Join("&", parts);

            try
            {
                var json = await Http.GetStringAsync(query, ct);
                return JsonSerializer.Deserialize<List<ModrinthVersion>>(json) ?? new();
            }
            catch { return new(); }
        }

        public async Task<byte[]> DownloadFileAsync(string url, CancellationToken ct = default) =>
            await Http.GetByteArrayAsync(url, ct);

        private static string BuildFacets(string projectType, string? mcVersion, string? loader)
        {
            var facets = new List<List<string>> { new() { $"project_type:{projectType}" } };
            if (!string.IsNullOrEmpty(mcVersion))
                facets.Add(new() { $"versions:{mcVersion}" });
            var mapped = MapLoader(loader);
            if (!string.IsNullOrEmpty(mapped))
                facets.Add(new() { $"categories:{mapped}" });
            return JsonSerializer.Serialize(facets);
        }

        private static string? MapLoader(string? loader) => loader?.ToLowerInvariant() switch
        {
            "fabric" => "fabric",
            "forge" => "forge",
            "neoforge" => "neoforge",
            "quilt" => "quilt",
            "vanilla" => null,
            _ => null
        };
    }
}
