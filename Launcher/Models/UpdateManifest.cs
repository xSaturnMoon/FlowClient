using System.Text.Json.Serialization;

namespace Launcher.Models
{
    public sealed class UpdateManifest
    {
        [JsonPropertyName("version")]
        public string Version { get; set; } = "1.0.0";

        [JsonPropertyName("channel")]
        public string Channel { get; set; } = "Stable";

        [JsonPropertyName("releaseNotes")]
        public string? ReleaseNotes { get; set; }

        [JsonPropertyName("manifestUrl")]
        public string? ManifestUrl { get; set; }

        [JsonPropertyName("githubRepository")]
        public string? GitHubRepository { get; set; }

        [JsonPropertyName("downloadUrl")]
        public string? DownloadUrl { get; set; }
    }
}
