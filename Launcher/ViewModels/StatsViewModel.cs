using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using Launcher.Models;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public sealed class StatsViewModel : INotifyPropertyChanged
    {
        private string _heroTotalHours = "0h";
        private string _heroSessions = "0";
        private string _heroStreak = "0 days";
        private string _heroAvgDaily = "0h";
        private string _heroLaunches = "0";
        private string _heroServers = "0";
        private string _periodSummary = "Last 7 days";
        private bool _hasPlayData;

        private readonly PlayTimeService _playTime = new();
        private readonly AccountActivityService _activity = new();
        private readonly InstanceService _instances = new();

        public string HeroTotalHours
        {
            get => _heroTotalHours;
            private set { _heroTotalHours = value; OnPropertyChanged(); }
        }

        public string HeroSessions
        {
            get => _heroSessions;
            private set { _heroSessions = value; OnPropertyChanged(); }
        }

        public string HeroStreak
        {
            get => _heroStreak;
            private set { _heroStreak = value; OnPropertyChanged(); }
        }

        public string HeroAvgDaily
        {
            get => _heroAvgDaily;
            private set { _heroAvgDaily = value; OnPropertyChanged(); }
        }

        public string HeroLaunches
        {
            get => _heroLaunches;
            private set { _heroLaunches = value; OnPropertyChanged(); }
        }

        public string HeroServers
        {
            get => _heroServers;
            private set { _heroServers = value; OnPropertyChanged(); }
        }

        public string PeriodSummary
        {
            get => _periodSummary;
            private set { _periodSummary = value; OnPropertyChanged(); }
        }

        public bool HasPlayData
        {
            get => _hasPlayData;
            private set { _hasPlayData = value; OnPropertyChanged(); }
        }

        public ObservableCollection<ChartPointViewModel> WeeklyChart { get; } = new();
        public ObservableCollection<SessionRowViewModel> RecentSessions { get; } = new();
        public ObservableCollection<StatCardViewModel> OverviewCards { get; } = new();
        public ObservableCollection<RecentItemViewModel> TopServers { get; } = new();
        public ObservableCollection<RecentItemViewModel> TopInstances { get; } = new();

        public void Load()
        {
            _activity.SyncFromPlayTime(_playTime);

            var totalHours = _playTime.TotalHours();
            var sessions = _activity.GetSessions(100);
            var streak = _activity.GetConsecutiveDays();
            var avgDaily = _playTime.GetAverageDailyHours(7);
            var instances = _instances.GetAll().ToList();
            var servers = ServerHistoryService.Instance.GetTopServers(8);

            HeroTotalHours = FormatHours(totalHours);
            HeroSessions = sessions.Count.ToString();
            HeroStreak = streak == 1 ? "1 day" : $"{streak} days";
            HeroAvgDaily = FormatHours(avgDaily) + "/day";
            HeroLaunches = servers.Sum(s => s.PlayCount).ToString();
            HeroServers = servers.Count.ToString();

            var weekly = BuildWeeklyChart();
            WeeklyChart.Clear();
            foreach (var point in weekly)
                WeeklyChart.Add(point);

            HasPlayData = WeeklyChart.Any(p => p.Value > 0);

            RecentSessions.Clear();
            foreach (var session in sessions.Take(6))
            {
                RecentSessions.Add(new SessionRowViewModel
                {
                    Duration = session.DurationText,
                    Version = session.Version,
                    Loader = session.Loader,
                    Server = session.Server,
                    Start = session.StartText,
                    End = session.EndText,
                    Ram = session.RamText
                });
            }

            OverviewCards.Clear();
            OverviewCards.Add(new StatCardViewModel
            {
                Label = "Installations",
                Value = instances.Count.ToString(),
                Subtitle = $"{instances.Count(i => i.IsFavorite)} favorites"
            });
            OverviewCards.Add(new StatCardViewModel
            {
                Label = "Mods installed",
                Value = instances.Sum(i => i.Mods.Count).ToString(),
                Subtitle = "Across all instances"
            });
            OverviewCards.Add(new StatCardViewModel
            {
                Label = "Active days",
                Value = _playTime.GetActiveDaysCount(30).ToString(),
                Subtitle = "Last 30 days"
            });
            OverviewCards.Add(new StatCardViewModel
            {
                Label = "Longest day",
                Value = FormatHours(_playTime.GetLongestSessionHours()),
                Subtitle = "Single-day record"
            });

            TopServers.Clear();
            foreach (var server in servers.Take(5))
            {
                TopServers.Add(new RecentItemViewModel
                {
                    Title = string.IsNullOrWhiteSpace(server.Name) ? server.Host : server.Name,
                    Subtitle = $"{server.PlayCount} joins · {FormatRelative(server.LastPlayedAt)}",
                    Icon = "◆"
                });
            }

            TopInstances.Clear();
            foreach (var inst in instances
                         .Where(i => i.LastPlayedAt.HasValue)
                         .OrderByDescending(i => i.LastPlayedAt)
                         .Take(5))
            {
                TopInstances.Add(new RecentItemViewModel
                {
                    Title = inst.Name,
                    Subtitle = $"{inst.MinecraftVersion} · {inst.Loader} · {FormatRelative(inst.LastPlayedAt)}",
                    Icon = inst.IconLetter
                });
            }

            PeriodSummary = HasPlayData
                ? $"{FormatHours(weekly.Sum(p => p.Value))} this week"
                : "No play time recorded yet";
        }

        private List<ChartPointViewModel> BuildWeeklyChart()
        {
            var data = _playTime.GetWeeklyData();
            var max = data.Max(p => p.Hours);
            if (max <= 0)
                max = 1;

            return data.Select(p => new ChartPointViewModel
            {
                Label = p.Label,
                Value = p.Hours,
                DisplayValue = p.Hours >= 1 ? $"{p.Hours:0.#}h" : p.Hours > 0 ? $"{p.Hours * 60:0}m" : "—",
                BarHeight = p.Hours > 0 ? Math.Max(6, p.Hours / max * 88) : 4
            }).ToList();
        }

        private static string FormatHours(double hours)
        {
            if (hours <= 0)
                return "0h";
            if (hours < 1)
                return $"{hours * 60:0}m";
            if (hours >= 100)
                return $"{hours:0}h";
            return $"{hours:0.#}h";
        }

        private static string FormatRelative(DateTime? utc)
        {
            if (!utc.HasValue)
                return "never";

            var local = utc.Value.ToLocalTime();
            var delta = DateTime.Now - local;
            if (delta.TotalMinutes < 1) return "just now";
            if (delta.TotalHours < 1) return $"{(int)delta.TotalMinutes}m ago";
            if (delta.TotalDays < 1) return $"{(int)delta.TotalHours}h ago";
            if (delta.TotalDays < 7) return $"{(int)delta.TotalDays}d ago";
            return local.ToString("dd MMM");
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        private void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
