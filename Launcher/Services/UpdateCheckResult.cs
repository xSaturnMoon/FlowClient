namespace Launcher.Services
{
    public sealed class UpdateCheckResult
    {
        public bool Success { get; init; }
        public bool UpdateAvailable { get; init; }
        public string CurrentVersion { get; init; } = "—";
        public string? LatestVersion { get; init; }
        public string? ReleaseNotes { get; init; }
        public string? DownloadUrl { get; init; }
        public string? Channel { get; init; }
        public string Message { get; init; } = string.Empty;
    }
}
