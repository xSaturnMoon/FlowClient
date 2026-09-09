using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace Launcher.Services
{
    public class ActivityEvent
    {
        public DateTime Timestamp { get; set; }
        public string   Type      { get; set; } = string.Empty;
        public string   Title     { get; set; } = string.Empty;
        public string   Detail    { get; set; } = string.Empty;
        public string   Icon      { get; set; } = "•";
        public bool     IsError   { get; set; }
    }

    public class GameSessionRecord
    {
        public DateTime Start       { get; set; }
        public DateTime? End        { get; set; }
        public double   Hours       { get; set; }
        public string   Version     { get; set; } = "—";
        public string   Loader      { get; set; } = "—";
        public string   World       { get; set; } = "—";
        public string   Server      { get; set; } = "—";
        public int?     AvgFps      { get; set; }
        public int?     RamMb        { get; set; }
        public bool     Crashed     { get; set; }
        public string   ExitCode    { get; set; } = "—";

        public string DurationText => Hours > 0 ? $"{Hours:0.0}h" : "—";
        public string StartText    => Start.ToString("dd/MM HH:mm");
        public string EndText      => End?.ToString("dd/MM HH:mm") ?? "—";
        public string FpsText      => AvgFps.HasValue ? $"{AvgFps}" : "—";
        public string RamText      => RamMb.HasValue ? $"{RamMb} MB" : "—";
    }

    public class AccountActivityService
    {
        private static readonly string ActivityPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "activity.json");

        private static readonly string SessionsPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "sessions.json");

        private List<ActivityEvent> _events = new();
        private List<GameSessionRecord> _sessions = new();

        public AccountActivityService()
        {
            Load();
        }

        public IReadOnlyList<ActivityEvent> GetTimeline(int count = 30)
            => _events.OrderByDescending(e => e.Timestamp).Take(count).ToList();

        public IReadOnlyList<GameSessionRecord> GetSessions(int count = 20)
            => _sessions.OrderByDescending(s => s.Start).Take(count).ToList();

        public int GetConsecutiveDays()
        {
            var days = _sessions.Select(s => s.Start.Date).ToHashSet();
            int streak = 0;
            var cursor = DateTime.Today;
            while (days.Contains(cursor))
            {
                streak++;
                cursor = cursor.AddDays(-1);
            }
            return streak;
        }

        public void Log(string type, string title, string detail = "", string icon = "•", bool isError = false)
        {
            _events.Add(new ActivityEvent
            {
                Timestamp = DateTime.Now,
                Type = type,
                Title = title,
                Detail = detail,
                Icon = icon,
                IsError = isError
            });
            if (_events.Count > 200) _events.RemoveRange(0, _events.Count - 200);
            SaveActivity();
        }

        public void RecordSession(GameSessionRecord session)
        {
            _sessions.Add(session);
            SaveSessions();
            Log("play", $"Sessione {session.DurationText}", session.Server != "—" ? session.Server : "Minecraft", "▶");
        }

        public void SyncFromPlayTime(PlayTimeService playTime)
        {
            var existing = _sessions.Select(s => s.Start.Date).ToHashSet();
            foreach (var (date, hours) in playTime.GetRecentSessions(14))
            {
                if (existing.Contains(date.Date)) continue;
                _sessions.Add(new GameSessionRecord
                {
                    Start = date,
                    End = date.AddHours(hours),
                    Hours = hours,
                    Version = "—",
                    Loader = "—"
                });
            }
            SaveSessions();
        }

        private void Load()
        {
            try
            {
                if (File.Exists(ActivityPath))
                    _events = JsonSerializer.Deserialize<List<ActivityEvent>>(File.ReadAllText(ActivityPath)) ?? new();
            }
            catch { _events = new(); }

            try
            {
                if (File.Exists(SessionsPath))
                    _sessions = JsonSerializer.Deserialize<List<GameSessionRecord>>(File.ReadAllText(SessionsPath)) ?? new();
            }
            catch { _sessions = new(); }
        }

        private void SaveActivity()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(ActivityPath)!);
                File.WriteAllText(ActivityPath, JsonSerializer.Serialize(_events,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }

        private void SaveSessions()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(SessionsPath)!);
                File.WriteAllText(SessionsPath, JsonSerializer.Serialize(_sessions,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }
    }
}
