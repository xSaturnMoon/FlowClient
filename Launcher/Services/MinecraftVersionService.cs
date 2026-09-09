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
    public class MinecraftVersionInfo
    {
        public string Id { get; set; } = "";
        public string Type { get; set; } = "release";
        public DateTime ReleaseTime { get; set; }

        public string TypeLabel => Type switch
        {
            "snapshot" => "Snapshot",
            "old_beta" => "Beta",
            "old_alpha" => "Alpha",
            _ => "Release"
        };
    }

    public class MinecraftVersionService
    {
        private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(20) };
        private List<MinecraftVersionInfo>? _cache;

        private static readonly string[] FallbackVersions =
        [
            "1.21.4", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9"
        ];

        public async Task<IReadOnlyList<MinecraftVersionInfo>> GetAllAsync(CancellationToken ct = default)
        {
            if (_cache != null) return _cache;

            try
            {
                var json = await Http.GetStringAsync(
                    "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", ct);

                using var doc = JsonDocument.Parse(json);
                var list = new List<MinecraftVersionInfo>();
                foreach (var v in doc.RootElement.GetProperty("versions").EnumerateArray())
                {
                    var id = v.GetProperty("id").GetString() ?? "";
                    var type = v.GetProperty("type").GetString() ?? "release";
                    var time = v.GetProperty("releaseTime").GetString();
                    DateTime.TryParse(time, out var dt);
                    list.Add(new MinecraftVersionInfo { Id = id, Type = type, ReleaseTime = dt });
                }
                _cache = list.OrderByDescending(x => x.ReleaseTime).ToList();
                return _cache;
            }
            catch
            {
                return FallbackVersions.Select(v => new MinecraftVersionInfo
                {
                    Id = v, Type = "release", ReleaseTime = DateTime.UtcNow
                }).ToList();
            }
        }

        public static IEnumerable<MinecraftVersionInfo> Filter(
            IEnumerable<MinecraftVersionInfo> all,
            string typeFilter,
            string search)
        {
            var q = all.AsEnumerable();
            if (!string.IsNullOrEmpty(typeFilter) && typeFilter != "all")
                q = q.Where(v => v.Type == typeFilter);
            if (!string.IsNullOrWhiteSpace(search))
            {
                var s = search.Trim().ToLowerInvariant();
                q = q.Where(v => v.Id.ToLowerInvariant().Contains(s));
            }
            return q;
        }
    }
}
