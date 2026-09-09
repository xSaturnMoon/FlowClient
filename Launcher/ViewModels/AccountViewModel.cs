using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public class AccountViewModel : INotifyPropertyChanged
    {
        // Auth
        private bool _isAuthenticated;
        private bool _isLoading;
        private bool _isSyncing;
        private string? _statusMessage;
        private bool _showDeviceCode;
        private string? _verificationUrl;
        private string? _userCode;

        // Profile
        private string? _minecraftUsername;
        private string? _formattedUuid;
        private string? _shortUuid;
        private string? _email;
        private string? _skinTextureUrl;
        private string? _activeCapeTextureUrl;
        private string? _linkedAtText;
        private string? _lastSyncText;
        private string? _tokenExpiryText;
        private string? _lastRefreshText;
        private string? _lastLoginText;
        private string? _activeCapeName;
        private string? _accountTypeText = "Java Edition";

        // Skin
        private string _selectedSkinVariant = "classic";
        private string _activeSkinVariant = "classic";
        private string? _selectedSkinFilePath;
        private string? _selectedSkinFileName;
        private bool _isUploadingSkin;
        private string? _skinUploadStatus;
        private ObservableCollection<CapeOptionViewModel> _capeOptions = new();
        private bool _isUpdatingCape;
        private string? _capeStatus;

        // Hero stats
        private string _heroTotalHours = "0h";
        private string _heroAvgFps = "—";
        private string _heroAvgRam = "—";
        private string _heroStreak = "0";

        // Live session
        private bool _isSessionLive;
        private string _sessionVersion = "—";
        private string _sessionLoader = "—";
        private string _sessionPlayTime = "—";
        private string _sessionFps = "—";
        private string _sessionRam = "—";
        private string _sessionCpu = "—";
        private string _sessionWorld = "—";
        private string _sessionServer = "—";
        private string _sessionDimension = "—";
        private string _sessionCoords = "—";
        private string _sessionPing = "—";

        private System.Windows.Media.ImageSource? _profilePicture;
        private string _playTimePeriod = "week";

        private ObservableCollection<ChartPointViewModel> _playTimePoints = new();
        private ObservableCollection<RecentSkinViewModel> _recentSkins = new();
        private ObservableCollection<MinecraftCape> _capes = new();
        private ObservableCollection<TimelineItemViewModel> _timeline = new();
        private ObservableCollection<AchievementViewModel> _achievements = new();
        private ObservableCollection<RecentItemViewModel> _recentWorlds = new();
        private ObservableCollection<RecentItemViewModel> _recentServers = new();
        private ObservableCollection<RecentItemViewModel> _recentModpacks = new();

        // Launcher info
        private string _launcherVersion = "—";
        private string _launcherChannel = "Stable";
        private string _minecraftDir = "—";
        private string _javaPath = "—";
        private string _javaVersion = "—";
        private string _ramTotal = "—";
        private string _gpuName = "—";
        private string _cpuName = "—";
        private string _osName = "—";

        public ObservableCollection<TimelineItemViewModel> Timeline
        {
            get => _timeline;
            set { _timeline = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasTimeline)); }
        }

        public bool HasTimeline => _timeline.Count > 0;

        public ObservableCollection<AchievementViewModel> Achievements
        {
            get => _achievements;
            set { _achievements = value; OnPropertyChanged(); }
        }

        public ObservableCollection<MinecraftCape> Capes
        {
            get => _capes;
            set { _capes = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasCapes)); }
        }

        public bool HasCapes => _capes.Count > 0;

        public ObservableCollection<RecentSkinViewModel> RecentSkins
        {
            get => _recentSkins;
            set { _recentSkins = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasRecentSkins)); }
        }

        public bool HasRecentSkins => _recentSkins.Count > 0;

        public ObservableCollection<ChartPointViewModel> PlayTimePoints
        {
            get => _playTimePoints;
            set { _playTimePoints = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasNoPlayData)); }
        }

        public bool HasNoPlayData => _playTimePoints.Count == 0 || _playTimePoints.All(p => p.Value == 0);

        public ObservableCollection<RecentItemViewModel> RecentWorlds
        {
            get => _recentWorlds;
            set { _recentWorlds = value; OnPropertyChanged(); }
        }

        public ObservableCollection<RecentItemViewModel> RecentServers
        {
            get => _recentServers;
            set { _recentServers = value; OnPropertyChanged(); }
        }

        public ObservableCollection<RecentItemViewModel> RecentModpacks
        {
            get => _recentModpacks;
            set { _recentModpacks = value; OnPropertyChanged(); }
        }

        public bool IsAuthenticated { get => _isAuthenticated; set { _isAuthenticated = value; OnPropertyChanged(); } }
        public bool IsLoading { get => _isLoading; set { _isLoading = value; OnPropertyChanged(); } }
        public bool IsSyncing
        {
            get => _isSyncing;
            set { _isSyncing = value; OnPropertyChanged(); OnPropertyChanged(nameof(ConnectionStatusText)); }
        }
        public string ConnectionStatusText => _isSyncing ? "Syncing…" : "Microsoft connected";
        public string? StatusMessage { get => _statusMessage; set { _statusMessage = value; OnPropertyChanged(); } }
        public bool ShowDeviceCode { get => _showDeviceCode; set { _showDeviceCode = value; OnPropertyChanged(); } }
        public string? VerificationUrl { get => _verificationUrl; set { _verificationUrl = value; OnPropertyChanged(); } }
        public string? UserCode { get => _userCode; set { _userCode = value; OnPropertyChanged(); } }

        public string? MinecraftUsername { get => _minecraftUsername; set { _minecraftUsername = value; OnPropertyChanged(); } }
        public string? FormattedUuid { get => _formattedUuid; set { _formattedUuid = value; OnPropertyChanged(); } }
        public string? ShortUuid { get => _shortUuid; set { _shortUuid = value; OnPropertyChanged(); } }
        public string? Email { get => _email; set { _email = value; OnPropertyChanged(); } }
        public string? SkinTextureUrl { get => _skinTextureUrl; set { _skinTextureUrl = value; OnPropertyChanged(); } }
        public string? ActiveCapeTextureUrl { get => _activeCapeTextureUrl; set { _activeCapeTextureUrl = value; OnPropertyChanged(); } }
        public string? LinkedAtText { get => _linkedAtText; set { _linkedAtText = value; OnPropertyChanged(); } }
        public string? LastSyncText { get => _lastSyncText; set { _lastSyncText = value; OnPropertyChanged(); } }
        public string? TokenExpiryText { get => _tokenExpiryText; set { _tokenExpiryText = value; OnPropertyChanged(); } }
        public string? LastRefreshText { get => _lastRefreshText; set { _lastRefreshText = value; OnPropertyChanged(); } }
        public string? LastLoginText { get => _lastLoginText; set { _lastLoginText = value; OnPropertyChanged(); } }
        public string? SkinModelLabel =>
            _selectedSkinVariant == "slim" ? "Slim (Alex)" : "Classic (Steve)";
        public string? ActiveCapeName { get => _activeCapeName; set { _activeCapeName = value; OnPropertyChanged(); } }
        public string? AccountTypeText { get => _accountTypeText; set { _accountTypeText = value; OnPropertyChanged(); } }

        public string HeroTotalHours { get => _heroTotalHours; set { _heroTotalHours = value; OnPropertyChanged(); } }
        public string HeroAvgFps { get => _heroAvgFps; set { _heroAvgFps = value; OnPropertyChanged(); } }
        public string HeroAvgRam { get => _heroAvgRam; set { _heroAvgRam = value; OnPropertyChanged(); } }
        public string HeroStreak { get => _heroStreak; set { _heroStreak = value; OnPropertyChanged(); } }

        public bool IsSessionLive { get => _isSessionLive; set { _isSessionLive = value; OnPropertyChanged(); } }
        public string SessionVersion { get => _sessionVersion; set { _sessionVersion = value; OnPropertyChanged(); } }
        public string SessionLoader { get => _sessionLoader; set { _sessionLoader = value; OnPropertyChanged(); } }
        public string SessionPlayTime { get => _sessionPlayTime; set { _sessionPlayTime = value; OnPropertyChanged(); } }
        public string SessionFps { get => _sessionFps; set { _sessionFps = value; OnPropertyChanged(); } }
        public string SessionRam { get => _sessionRam; set { _sessionRam = value; OnPropertyChanged(); } }
        public string SessionCpu { get => _sessionCpu; set { _sessionCpu = value; OnPropertyChanged(); } }
        public string SessionWorld { get => _sessionWorld; set { _sessionWorld = value; OnPropertyChanged(); } }
        public string SessionServer { get => _sessionServer; set { _sessionServer = value; OnPropertyChanged(); } }
        public string SessionDimension { get => _sessionDimension; set { _sessionDimension = value; OnPropertyChanged(); } }
        public string SessionCoords { get => _sessionCoords; set { _sessionCoords = value; OnPropertyChanged(); } }
        public string SessionPing { get => _sessionPing; set { _sessionPing = value; OnPropertyChanged(); } }

        public string? SelectedSkinFilePath
        {
            get => _selectedSkinFilePath;
            set { _selectedSkinFilePath = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanApplySkin)); }
        }
        public string? SelectedSkinFileName
        {
            get => _selectedSkinFileName;
            set { _selectedSkinFileName = value; OnPropertyChanged(); }
        }
        public string SelectedSkinVariant
        {
            get => _selectedSkinVariant;
            set
            {
                _selectedSkinVariant = value == "slim" ? "slim" : "classic";
                OnPropertyChanged();
                OnPropertyChanged(nameof(SkinModelLabel));
                OnPropertyChanged(nameof(CanApplySkin));
                OnPropertyChanged(nameof(HasPendingSkinChange));
            }
        }
        public string ActiveSkinVariant
        {
            get => _activeSkinVariant;
            set
            {
                _activeSkinVariant = value == "slim" ? "slim" : "classic";
                OnPropertyChanged();
                OnPropertyChanged(nameof(CanApplySkin));
                OnPropertyChanged(nameof(HasPendingSkinChange));
            }
        }
        public bool IsUploadingSkin
        {
            get => _isUploadingSkin;
            set { _isUploadingSkin = value; OnPropertyChanged(); OnPropertyChanged(nameof(CanApplySkin)); }
        }
        public bool CanApplySkin =>
            CanModifyProfile &&
            !_isUploadingSkin &&
            (!string.IsNullOrEmpty(_selectedSkinFilePath) || HasPendingSkinChange);
        public bool HasPendingSkinChange =>
            !string.Equals(_selectedSkinVariant, _activeSkinVariant, StringComparison.OrdinalIgnoreCase);
        public bool CanModifyProfile => !ProfileModificationGuard.IsCoolingDown;
        public string? ProfileLockMessage =>
            ProfileModificationGuard.IsCoolingDown ? ProfileModificationGuard.FormatRemaining() : null;

        public void RefreshProfileLockState()
        {
            OnPropertyChanged(nameof(CanApplySkin));
            OnPropertyChanged(nameof(CanModifyProfile));
            OnPropertyChanged(nameof(ProfileLockMessage));
        }
        public string? SkinUploadStatus { get => _skinUploadStatus; set { _skinUploadStatus = value; OnPropertyChanged(); } }

        public ObservableCollection<CapeOptionViewModel> CapeOptions
        {
            get => _capeOptions;
            set { _capeOptions = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasCapeChoices)); }
        }
        public bool HasCapeChoices => _capeOptions.Count > 0;
        public bool IsUpdatingCape
        {
            get => _isUpdatingCape;
            set { _isUpdatingCape = value; OnPropertyChanged(); }
        }
        public string? CapeStatus { get => _capeStatus; set { _capeStatus = value; OnPropertyChanged(); } }

        public System.Windows.Media.ImageSource? ProfilePicture
        {
            get => _profilePicture;
            set { _profilePicture = value; OnPropertyChanged(); }
        }

        public string PlayTimePeriod { get => _playTimePeriod; set { _playTimePeriod = value; OnPropertyChanged(); } }

        public string LauncherVersion { get => _launcherVersion; set { _launcherVersion = value; OnPropertyChanged(); } }
        public string LauncherChannel { get => _launcherChannel; set { _launcherChannel = value; OnPropertyChanged(); } }
        public string MinecraftDir { get => _minecraftDir; set { _minecraftDir = value; OnPropertyChanged(); } }
        public string JavaPath { get => _javaPath; set { _javaPath = value; OnPropertyChanged(); } }
        public string JavaVersion { get => _javaVersion; set { _javaVersion = value; OnPropertyChanged(); } }
        public string RamTotal { get => _ramTotal; set { _ramTotal = value; OnPropertyChanged(); } }
        public string GpuName { get => _gpuName; set { _gpuName = value; OnPropertyChanged(); } }
        public string CpuName { get => _cpuName; set { _cpuName = value; OnPropertyChanged(); } }
        public string OsName { get => _osName; set { _osName = value; OnPropertyChanged(); } }

        public static string FormatUuid(string? uuid)
        {
            if (string.IsNullOrEmpty(uuid)) return "—";
            var clean = uuid.Replace("-", "");
            if (clean.Length != 32) return uuid;
            return $"{clean[..8]}-{clean[8..12]}-{clean[12..16]}-{clean[16..20]}-{clean[20..]}";
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

        public void OnPropertyChangedPublic(string name)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class CapeOptionViewModel
    {
        public string? Id { get; set; }
        public string? TextureUrl { get; set; }
        public string DisplayName { get; set; } = string.Empty;
        public bool IsActive { get; set; }
    }

    public class ChartPointViewModel
    {
        public string Label { get; set; } = string.Empty;
        public double Value { get; set; }
        public string DisplayValue { get; set; } = string.Empty;
        public double BarHeight { get; set; }
    }

    public class RecentSkinViewModel
    {
        public string FilePath { get; set; } = string.Empty;
        public System.Windows.Media.ImageSource? HeadImage { get; set; }
    }
}
