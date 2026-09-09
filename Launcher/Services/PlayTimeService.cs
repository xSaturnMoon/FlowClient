using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace Launcher.Services
{
    public class PlaySession
    {
        public DateTime Date    { get; set; }
        public double   Hours   { get; set; }
    }

    public class PlayTimeService
    {
        private static readonly string FilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "playtime.json");

        private List<PlaySession> _sessions = new();

        public PlayTimeService() => Load();

        // ── Persistence ───────────────────────────────────────────────────────

        private void Load()
        {
            try
            {
                if (!File.Exists(FilePath)) { SeedDemoData(); return; }
                var json = File.ReadAllText(FilePath);
                _sessions = JsonSerializer.Deserialize<List<PlaySession>>(json) ?? new();
            }
            catch { SeedDemoData(); }
        }

        private void Save()
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
                File.WriteAllText(FilePath, JsonSerializer.Serialize(_sessions,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }

        // ── Write ─────────────────────────────────────────────────────────────

        /// <summary>Call this when a game session ends to record play time.</summary>
        public void RecordSession(TimeSpan duration)
        {
            var today = DateTime.Today;
            var existing = _sessions.FirstOrDefault(s => s.Date.Date == today);
            if (existing != null)
                existing.Hours += duration.TotalHours;
            else
                _sessions.Add(new PlaySession { Date = today, Hours = duration.TotalHours });
            Save();
        }

        // ── Read ──────────────────────────────────────────────────────────────

        public double TotalHours() => _sessions.Sum(s => s.Hours);

        public int GetActiveDaysCount(int days = 7) =>
            _sessions.Count(s => s.Date.Date >= DateTime.Today.AddDays(-(days - 1)) && s.Hours > 0);

        public double GetAverageDailyHours(int days = 7)
        {
            var since = DateTime.Today.AddDays(-(days - 1));
            var relevant = _sessions.Where(s => s.Date.Date >= since).ToList();
            if (relevant.Count == 0) return 0;
            return relevant.Sum(s => s.Hours) / days;
        }

        public double GetLongestSessionHours() =>
            _sessions.Count > 0 ? _sessions.Max(s => s.Hours) : 0;

        public List<(DateTime Date, double Hours)> GetRecentSessions(int count = 5) =>
            _sessions
                .Where(s => s.Hours > 0)
                .OrderByDescending(s => s.Date)
                .Take(count)
                .Select(s => (s.Date, s.Hours))
                .ToList();

        /// <summary>Returns (label, hours) pairs for the last 7 days (bar chart).</summary>
        public List<(string Label, double Hours)> GetWeeklyData()
        {
            var result = new List<(string, double)>();
            string[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
            for (int i = 6; i >= 0; i--)
            {
                var date  = DateTime.Today.AddDays(-i);
                var hours = _sessions.Where(s => s.Date.Date == date.Date).Sum(s => s.Hours);
                result.Add((days[(int)date.DayOfWeek == 0 ? 6 : (int)date.DayOfWeek - 1], hours));
            }
            return result;
        }

        /// <summary>Returns (label, hours) pairs for the last 4 weeks.</summary>
        public List<(string Label, double Hours)> GetMonthlyData()
        {
            var result = new List<(string, double)>();
            for (int w = 3; w >= 0; w--)
            {
                var start = DateTime.Today.AddDays(-(w * 7 + 6));
                var end   = DateTime.Today.AddDays(-w * 7);
                var hours = _sessions.Where(s => s.Date.Date >= start && s.Date.Date <= end).Sum(s => s.Hours);
                result.Add(($"W{4 - w}", hours));
            }
            return result;
        }

        /// <summary>Returns (label, hours) pairs for the last 12 months.</summary>
        public List<(string Label, double Hours)> GetYearlyData()
        {
            string[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
            var result = new List<(string, double)>();
            for (int m = 11; m >= 0; m--)
            {
                var date  = DateTime.Today.AddMonths(-m);
                var hours = _sessions
                    .Where(s => s.Date.Year == date.Year && s.Date.Month == date.Month)
                    .Sum(s => s.Hours);
                result.Add((months[date.Month - 1], hours));
            }
            return result;
        }

        // ── Demo seed (first-run) ─────────────────────────────────────────────

        private void SeedDemoData()
        {
            _sessions = new List<PlaySession>();
            Save();
        }
    }
}
