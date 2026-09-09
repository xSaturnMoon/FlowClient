using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Launcher.ViewModels
{
    public class UpdateViewModel : INotifyPropertyChanged
    {
        private string _launcherVersion = "—";
        private string _launcherChannel = "Stable";
        private string _latestVersion = "—";
        private string _statusMessage = "Check for the latest Flow release.";
        private string _lastCheckedText = "Never";
        private string? _releaseNotes;
        private bool _isChecking;
        private bool _hasUpdate;
        private string? _downloadUrl;
        private string? _downloadedPackagePath;
        private bool _isDownloading;
        private double _downloadProgress;

        public string LauncherVersion
        {
            get => _launcherVersion;
            set { _launcherVersion = value; OnPropertyChanged(); }
        }

        public string LauncherChannel
        {
            get => _launcherChannel;
            set { _launcherChannel = value; OnPropertyChanged(); }
        }

        public string LatestVersion
        {
            get => _latestVersion;
            set { _latestVersion = value; OnPropertyChanged(); }
        }

        public string StatusMessage
        {
            get => _statusMessage;
            set { _statusMessage = value; OnPropertyChanged(); }
        }

        public string LastCheckedText
        {
            get => _lastCheckedText;
            set { _lastCheckedText = value; OnPropertyChanged(); }
        }

        public string? ReleaseNotes
        {
            get => _releaseNotes;
            set { _releaseNotes = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasReleaseNotes)); }
        }

        public bool HasReleaseNotes => !string.IsNullOrWhiteSpace(_releaseNotes);

        public bool IsChecking
        {
            get => _isChecking;
            set { _isChecking = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanCheck)); }
        }

        public bool HasUpdate
        {
            get => _hasUpdate;
            set { _hasUpdate = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanDownload)); OnPropertyChanged(nameof(CanInstall)); }
        }

        public string? DownloadUrl
        {
            get => _downloadUrl;
            set { _downloadUrl = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanDownload)); }
        }

        public bool IsDownloading
        {
            get => _isDownloading;
            set { _isDownloading = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanDownload)); OnPropertyChanged(nameof(CanCheck)); OnPropertyChanged(nameof(CanInstall)); }
        }

        public double DownloadProgress
        {
            get => _downloadProgress;
            set { _downloadProgress = value; OnPropertyChanged(); OnPropertyChanged(nameof(ShowDownloadProgress)); }
        }

        public string? DownloadedPackagePath
        {
            get => _downloadedPackagePath;
            private set
            {
                _downloadedPackagePath = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(CanDownload));
                OnPropertyChanged(nameof(CanInstall));
            }
        }

        public bool CanCheck => !_isDownloading;
        public bool CanDownload => _hasUpdate && !string.IsNullOrEmpty(_downloadUrl) && !_isDownloading && string.IsNullOrEmpty(_downloadedPackagePath);
        public bool CanInstall => !string.IsNullOrEmpty(_downloadedPackagePath) && !_isDownloading;
        public bool ShowDownloadProgress => _isDownloading || _downloadProgress > 0;

        public void SetDownloadedPackage(string? path) => DownloadedPackagePath = path;

        public void ResetDownloadState()
        {
            DownloadedPackagePath = null;
            DownloadProgress = 0;
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        protected void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
