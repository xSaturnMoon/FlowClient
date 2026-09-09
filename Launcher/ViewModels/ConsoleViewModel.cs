using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Windows;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public sealed class ConsoleViewModel : INotifyPropertyChanged
    {
        private string _selectedPanel = "launcher";
        private string _displayText = "";
        private string _statusText = "";
        private string _activeFileTitle = "";
        private string _activeFileDetail = "";
        private CrashReportInfo? _selectedCrash;
        private LatestLogEntry? _activeLog;
        private bool _autoScroll = true;

        private readonly LauncherLogService _launcherLog = LauncherLogService.Instance;
        private readonly GameLogService _gameLogs = new();

        public string SelectedPanel
        {
            get => _selectedPanel;
            set
            {
                if (_selectedPanel == value) return;
                _selectedPanel = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(IsLauncherPanel));
                OnPropertyChanged(nameof(IsLatestLogPanel));
                OnPropertyChanged(nameof(IsCrashPanel));
                OnPropertyChanged(nameof(ShowActiveFileInfo));
                Refresh();
            }
        }

        public bool IsLauncherPanel => _selectedPanel == "launcher";
        public bool IsLatestLogPanel => _selectedPanel == "log";
        public bool IsCrashPanel => _selectedPanel == "crash";
        public bool ShowActiveFileInfo => IsLatestLogPanel || IsCrashPanel;

        public string DisplayText
        {
            get => _displayText;
            private set { _displayText = value; OnPropertyChanged(); }
        }

        public string StatusText
        {
            get => _statusText;
            private set { _statusText = value; OnPropertyChanged(); }
        }

        public string ActiveFileTitle
        {
            get => _activeFileTitle;
            private set { _activeFileTitle = value; OnPropertyChanged(); }
        }

        public string ActiveFileDetail
        {
            get => _activeFileDetail;
            private set { _activeFileDetail = value; OnPropertyChanged(); }
        }

        public bool AutoScroll
        {
            get => _autoScroll;
            set { _autoScroll = value; OnPropertyChanged(); }
        }

        public ObservableCollection<CrashReportInfo> RecentCrashes { get; } = new();

        public CrashReportInfo? SelectedCrash
        {
            get => _selectedCrash;
            set
            {
                if (_selectedCrash == value) return;
                _selectedCrash = value;
                OnPropertyChanged();
                if (IsCrashPanel && value != null)
                {
                    DisplayText = _gameLogs.ReadCrashReport(value.FullPath);
                    ActiveFileTitle = value.FileName;
                    ActiveFileDetail = $"{value.SourceLabel} · {value.Subtitle}";
                    StatusText = $"Crash report · {value.SourceLabel}";
                }
            }
        }

        public void Load()
        {
            ReloadRecentCrashes();
            Refresh();
        }

        public void Refresh()
        {
            switch (SelectedPanel)
            {
                case "launcher":
                    DisplayText = _launcherLog.GetText();
                    StatusText = "Launcher output";
                    ActiveFileTitle = "";
                    ActiveFileDetail = "";
                    break;
                case "log":
                    RefreshLatestLog();
                    break;
                case "crash":
                    RefreshCrashPanel();
                    break;
            }
        }

        public void OnLauncherLineAdded()
        {
            if (!IsLauncherPanel)
                return;
            DisplayText = _launcherLog.GetText();
        }

        public void ClearLauncher()
        {
            _launcherLog.Clear();
            if (IsLauncherPanel)
                Refresh();
        }

        public void CopyDisplayText()
        {
            if (string.IsNullOrEmpty(DisplayText))
                return;
            try
            {
                Clipboard.SetText(DisplayText);
                StatusText = "Copied to clipboard";
            }
            catch
            {
                StatusText = "Could not copy";
            }
        }

        public void OpenActiveFolder()
        {
            try
            {
                string? folder = SelectedPanel switch
                {
                    "log" => _activeLog != null
                        ? Path.Combine(_activeLog.Source.GameDirectory, "logs")
                        : null,
                    "crash" => SelectedCrash != null
                        ? Path.Combine(SelectedCrash.GameDirectory, "crash-reports")
                        : null,
                    _ => Path.Combine(
                        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                        "FlowLauncher")
                };

                if (string.IsNullOrEmpty(folder) || !Directory.Exists(folder))
                {
                    StatusText = "Folder not found";
                    return;
                }

                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                {
                    FileName = folder,
                    UseShellExecute = true
                });
            }
            catch
            {
                StatusText = "Could not open folder";
            }
        }

        private void RefreshLatestLog()
        {
            _activeLog = _gameLogs.FindMostRecentLatestLog();
            if (_activeLog == null)
            {
                DisplayText = "No latest.log found in any installation.";
                StatusText = "Latest log";
                ActiveFileTitle = "No log file";
                ActiveFileDetail = "";
                return;
            }

            DisplayText = _gameLogs.ReadLatestLogTail(_activeLog.Path);
            ActiveFileTitle = "latest.log";
            ActiveFileDetail = $"{_activeLog.Source.Label} · {_activeLog.ModifiedText}";
            StatusText = $"{_activeLog.Source.Label} · updated {_activeLog.ModifiedText}";
        }

        private void RefreshCrashPanel()
        {
            ReloadRecentCrashes();

            var latest = RecentCrashes.FirstOrDefault();
            if (latest == null)
            {
                SelectedCrash = null;
                DisplayText = "No crash reports found.";
                StatusText = "Crash report";
                ActiveFileTitle = "No crash report";
                ActiveFileDetail = "";
                return;
            }

            if (SelectedCrash == null || !RecentCrashes.Any(r => r.FullPath == SelectedCrash.FullPath))
                SelectedCrash = latest;
            else
            {
                DisplayText = _gameLogs.ReadCrashReport(SelectedCrash.FullPath);
                ActiveFileTitle = SelectedCrash.FileName;
                ActiveFileDetail = $"{SelectedCrash.SourceLabel} · {SelectedCrash.Subtitle}";
                StatusText = $"Crash report · {SelectedCrash.SourceLabel}";
            }
        }

        private void ReloadRecentCrashes()
        {
            var selectedPath = SelectedCrash?.FullPath;
            RecentCrashes.Clear();
            foreach (var report in _gameLogs.GetRecentCrashReports(3))
                RecentCrashes.Add(report);

            if (selectedPath != null)
                SelectedCrash = RecentCrashes.FirstOrDefault(r => r.FullPath == selectedPath) ?? RecentCrashes.FirstOrDefault();
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        private void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
