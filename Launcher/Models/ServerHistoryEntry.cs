namespace Launcher.Models
{
    public sealed class ServerHistoryEntry
    {
        public string Host { get; set; } = "";
        public int Port { get; set; } = 25565;
        public string Name { get; set; } = "";
        public int PlayCount { get; set; }
        public DateTime? LastPlayedAt { get; set; }

        public string Key => $"{Host.ToLowerInvariant()}:{Port}";

        public string DisplayName =>
            string.IsNullOrWhiteSpace(Name) ? Host : Name;

        public string AddressText => Port == 25565 ? Host : $"{Host}:{Port}";

        public string? IconPath { get; set; }
    }
}
