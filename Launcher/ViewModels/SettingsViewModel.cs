using System;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Launcher.Services;

namespace Launcher.ViewModels
{
    public sealed class SettingsViewModel : INotifyPropertyChanged
    {
        private string _selectedCategory = "Launch";
        private LaunchPostAction _postLaunchAction = LaunchPostAction.KeepOpen;
        private bool _confirmBeforeStop;
        private bool _enableFlowClientMod = true;
        private bool _enableVoiceChatMod = true;
        private bool _enableDiscordRpc = true;
        private bool _uiAnimations = true;
        private bool _autoCheckUpdates = true;
        private int _defaultRamMb = 4096;
        private string? _customJvmArgs;
        private string? _customJavaPath;
        private int _gameWidth = 1280;
        private int _gameHeight = 720;
        private bool _isGameFullscreen;

        private string _javaPath = "Detecting…";
        private string _javaVersion = "—";
        private string _totalRam = "—";
        private int _systemTotalRamMb = 16384;
        private string _cpuName = "—";
        private string _gpuName = "—";
        private string _osInfo = "—";
        private string _currentVersion = "v1.2.3";
        private string _releaseDate = "—";
        private string _installDate = "—";
        private string _maintenanceMessage = "";
        private string _updateCheckMessage = "";
        private bool _isCheckingUpdate;

        // Storage & Cleanup
        private StorageBreakdownInfo? _storageBreakdown;
        private string _reclaimableSpaceText = "Calcolo in corso…";
        private string _updateCacheSizeText = "—";
        private string _tempFilesSizeText = "—";
        private string _logsSizeText = "—";
        private string _cleanupResultText = "";
        private bool _isScanningStorage;
        private bool _isCleaningStorage;

        public string SelectedCategory
        {
            get => _selectedCategory;
            set
            {
                if (_selectedCategory == value) return;
                _selectedCategory = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(IsLaunchCategory));
                OnPropertyChanged(nameof(IsPerformanceCategory));
                OnPropertyChanged(nameof(IsModsCategory));
                OnPropertyChanged(nameof(IsFoldersCategory));
                OnPropertyChanged(nameof(IsStorageCategory));
                OnPropertyChanged(nameof(IsAppearanceCategory));
                OnPropertyChanged(nameof(IsAboutCategory));

                if (value == "Storage")
                {
                    _ = RefreshStorageAnalysisAsync();
                }
            }
        }

        public bool IsLaunchCategory => SelectedCategory == "Launch";
        public bool IsPerformanceCategory => SelectedCategory == "Performance";
        public bool IsModsCategory => SelectedCategory == "Mods";
        public bool IsFoldersCategory => SelectedCategory == "Folders";
        public bool IsStorageCategory => SelectedCategory == "Storage";
        public bool IsAppearanceCategory => SelectedCategory == "Appearance";
        public bool IsAboutCategory => SelectedCategory == "About";

        public LaunchPostAction PostLaunchAction
        {
            get => _postLaunchAction;
            set
            {
                if (_postLaunchAction == value) return;
                _postLaunchAction = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(IsActionKeepOpen));
                OnPropertyChanged(nameof(IsActionMinimize));
                OnPropertyChanged(nameof(IsActionHide));
                OnPropertyChanged(nameof(IsActionClose));
                OnPropertyChanged(nameof(LaunchBehaviorSummary));
            }
        }

        public bool IsActionKeepOpen
        {
            get => PostLaunchAction == LaunchPostAction.KeepOpen;
            set { if (value) PostLaunchAction = LaunchPostAction.KeepOpen; }
        }

        public bool IsActionMinimize
        {
            get => PostLaunchAction == LaunchPostAction.Minimize;
            set { if (value) PostLaunchAction = LaunchPostAction.Minimize; }
        }

        public bool IsActionHide
        {
            get => PostLaunchAction == LaunchPostAction.HideWhilePlaying;
            set { if (value) PostLaunchAction = LaunchPostAction.HideWhilePlaying; }
        }

        public bool IsActionClose
        {
            get => PostLaunchAction == LaunchPostAction.CloseWhenGameReady;
            set { if (value) PostLaunchAction = LaunchPostAction.CloseWhenGameReady; }
        }

        public bool CloseLauncherOnLaunch
        {
            get => IsActionClose;
            set { if (value) PostLaunchAction = LaunchPostAction.CloseWhenGameReady; else if (IsActionClose) PostLaunchAction = LaunchPostAction.KeepOpen; }
        }

        public bool MinimizeLauncherOnLaunch
        {
            get => IsActionMinimize;
            set { if (value) PostLaunchAction = LaunchPostAction.Minimize; else if (IsActionMinimize) PostLaunchAction = LaunchPostAction.KeepOpen; }
        }

        public bool ConfirmBeforeStop
        {
            get => _confirmBeforeStop;
            set
            {
                if (_confirmBeforeStop == value) return;
                _confirmBeforeStop = value;
                OnPropertyChanged();
            }
        }

        public bool EnableFlowClientMod
        {
            get => _enableFlowClientMod;
            set
            {
                if (_enableFlowClientMod == value) return;
                _enableFlowClientMod = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(FlowClientModSummary));
            }
        }

        public bool EnableVoiceChatMod
        {
            get => _enableVoiceChatMod;
            set
            {
                if (_enableVoiceChatMod == value) return;
                _enableVoiceChatMod = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(VoiceChatSummary));
            }
        }

        public bool EnableDiscordRpc
        {
            get => _enableDiscordRpc;
            set
            {
                if (_enableDiscordRpc == value) return;
                _enableDiscordRpc = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(DiscordRpcSummary));
            }
        }

        public bool UiAnimations
        {
            get => _uiAnimations;
            set
            {
                if (_uiAnimations == value) return;
                _uiAnimations = value;
                OnPropertyChanged();
            }
        }

        public bool AutoCheckUpdates
        {
            get => _autoCheckUpdates;
            set
            {
                if (_autoCheckUpdates == value) return;
                _autoCheckUpdates = value;
                OnPropertyChanged();
            }
        }

        public int DefaultRamMb
        {
            get => _defaultRamMb;
            set
            {
                if (_defaultRamMb == value) return;
                _defaultRamMb = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(DefaultRamGbText));
                OnPropertyChanged(nameof(RamUsagePercentText));
                OnPropertyChanged(nameof(RamUsageRatio));
                OnPropertyChanged(nameof(IsRamWarningVisible));
            }
        }

        public string DefaultRamGbText => $"{_defaultRamMb / 1024.0:F0} GB";

        public double RamUsageRatio => _systemTotalRamMb > 0
            ? Math.Min(1.0, (double)_defaultRamMb / _systemTotalRamMb)
            : 0.25;

        public string RamUsagePercentText
        {
            get
            {
                if (_systemTotalRamMb <= 0) return $"{_defaultRamMb / 1024} GB";
                var pct = (int)Math.Round(((double)_defaultRamMb / _systemTotalRamMb) * 100);
                return $"{pct}% della RAM totale ({_totalRam})";
            }
        }

        public bool IsRamWarningVisible => _systemTotalRamMb > 0 && ((double)_defaultRamMb / _systemTotalRamMb) > 0.75;

        public string? CustomJvmArgs
        {
            get => _customJvmArgs;
            set
            {
                if (_customJvmArgs == value) return;
                _customJvmArgs = value;
                OnPropertyChanged();
            }
        }

        public string? CustomJavaPath
        {
            get => _customJavaPath;
            set
            {
                if (_customJavaPath == value) return;
                _customJavaPath = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasCustomJava));
                OnPropertyChanged(nameof(EffectiveJavaPath));
            }
        }

        public bool HasCustomJava => !string.IsNullOrWhiteSpace(_customJavaPath);

        public string EffectiveJavaPath => HasCustomJava ? _customJavaPath! : _javaPath;

        public int GameWidth
        {
            get => _gameWidth;
            set { if (_gameWidth != value) { _gameWidth = value; OnPropertyChanged(); } }
        }

        public int GameHeight
        {
            get => _gameHeight;
            set { if (_gameHeight != value) { _gameHeight = value; OnPropertyChanged(); } }
        }

        public bool IsGameFullscreen
        {
            get => _isGameFullscreen;
            set { if (_isGameFullscreen != value) { _isGameFullscreen = value; OnPropertyChanged(); } }
        }

        public string JavaPath
        {
            get => _javaPath;
            set { _javaPath = value; OnPropertyChanged(); OnPropertyChanged(nameof(EffectiveJavaPath)); }
        }

        public string JavaVersion
        {
            get => _javaVersion;
            set { _javaVersion = value; OnPropertyChanged(); }
        }

        public string TotalRam
        {
            get => _totalRam;
            set
            {
                _totalRam = value;
                OnPropertyChanged();
                ParseTotalRamMb(value);
            }
        }

        private void ParseTotalRamMb(string ramStr)
        {
            try
            {
                var match = Regex.Match(ramStr, @"(\d+([\.,]\d+)?)\s*GB", RegexOptions.IgnoreCase);
                if (match.Success && double.TryParse(match.Groups[1].Value.Replace(',', '.'), System.Globalization.NumberStyles.Any, System.Globalization.CultureInfo.InvariantCulture, out var gb))
                {
                    _systemTotalRamMb = (int)(gb * 1024);
                    OnPropertyChanged(nameof(RamUsagePercentText));
                    OnPropertyChanged(nameof(RamUsageRatio));
                    OnPropertyChanged(nameof(IsRamWarningVisible));
                }
            }
            catch { }
        }

        public string CpuName
        {
            get => _cpuName;
            set { _cpuName = value; OnPropertyChanged(); }
        }

        public string GpuName
        {
            get => _gpuName;
            set { _gpuName = value; OnPropertyChanged(); }
        }

        public string OsInfo
        {
            get => _osInfo;
            set { _osInfo = value; OnPropertyChanged(); }
        }

        public string CurrentVersion
        {
            get => _currentVersion;
            set { _currentVersion = value; OnPropertyChanged(); }
        }

        public string ReleaseDate
        {
            get => _releaseDate;
            set { _releaseDate = value; OnPropertyChanged(); }
        }

        public string InstallDate
        {
            get => _installDate;
            set { _installDate = value; OnPropertyChanged(); }
        }

        public string MaintenanceMessage
        {
            get => _maintenanceMessage;
            set { _maintenanceMessage = value; OnPropertyChanged(); }
        }

        public string UpdateCheckMessage
        {
            get => _updateCheckMessage;
            set { _updateCheckMessage = value; OnPropertyChanged(); }
        }

        private bool _isUpdateAvailable;
        private string _latestVersion = "";
        private bool _isUpdating;

        public bool IsUpdateAvailable
        {
            get => _isUpdateAvailable;
            set { _isUpdateAvailable = value; OnPropertyChanged(); }
        }

        public string LatestVersion
        {
            get => _latestVersion;
            set { _latestVersion = value; OnPropertyChanged(); }
        }

        public bool IsUpdating
        {
            get => _isUpdating;
            set { _isUpdating = value; OnPropertyChanged(); }
        }

        public bool IsCheckingUpdate
        {
            get => _isCheckingUpdate;
            set { _isCheckingUpdate = value; OnPropertyChanged(); }
        }

        // ══════ DIRECTORIES ══════
        public string MinecraftDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");

        public string WorldsDirectory => Path.Combine(MinecraftDirectory, "saves");

        public string ScreenshotsDirectory => Path.Combine(MinecraftDirectory, "screenshots");

        public string ResourcePacksDirectory => Path.Combine(MinecraftDirectory, "resourcepacks");

        public string ShaderPacksDirectory => Path.Combine(MinecraftDirectory, "shaderpacks");

        public string ModsDirectory => Path.Combine(MinecraftDirectory, "mods");

        public string CrashReportsDirectory => Path.Combine(MinecraftDirectory, "crash-reports");

        public string LauncherDataDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "FlowLauncher");

        public string OfficialInstallDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "FlowClient");

        // ══════ STORAGE & CLEANUP ══════
        public string ReclaimableSpaceText
        {
            get => _reclaimableSpaceText;
            set { _reclaimableSpaceText = value; OnPropertyChanged(); }
        }

        public string UpdateCacheSizeText
        {
            get => _updateCacheSizeText;
            set { _updateCacheSizeText = value; OnPropertyChanged(); }
        }

        public string TempFilesSizeText
        {
            get => _tempFilesSizeText;
            set { _tempFilesSizeText = value; OnPropertyChanged(); }
        }

        public string LogsSizeText
        {
            get => _logsSizeText;
            set { _logsSizeText = value; OnPropertyChanged(); }
        }

        public string CleanupResultText
        {
            get => _cleanupResultText;
            set { _cleanupResultText = value; OnPropertyChanged(); }
        }

        public bool IsScanningStorage
        {
            get => _isScanningStorage;
            set { _isScanningStorage = value; OnPropertyChanged(); }
        }

        public bool IsCleaningStorage
        {
            get => _isCleaningStorage;
            set { _isCleaningStorage = value; OnPropertyChanged(); }
        }

        public async Task RefreshStorageAnalysisAsync()
        {
            if (IsScanningStorage) return;
            IsScanningStorage = true;
            try
            {
                var info = await Task.Run(() => StorageMaintenanceService.GetStorageBreakdown());
                _storageBreakdown = info;
                ReclaimableSpaceText = info.FormattedTotal;
                UpdateCacheSizeText = info.FormattedUpdateCache;
                TempFilesSizeText = info.FormattedTempFiles;
                LogsSizeText = info.FormattedLogsAndCrashes;
            }
            catch
            {
                ReclaimableSpaceText = "—";
            }
            finally
            {
                IsScanningStorage = false;
            }
        }

        public async Task<StorageCleanupResult> CleanTempFilesAsync()
        {
            IsCleaningStorage = true;
            CleanupResultText = "Pulizia in corso…";
            try
            {
                var res = await Task.Run(() => StorageMaintenanceService.CleanTempAndCacheFiles());
                CleanupResultText = res.Message;
                await RefreshStorageAnalysisAsync();
                return res;
            }
            finally
            {
                IsCleaningStorage = false;
            }
        }

        public async Task<StorageCleanupResult> CleanOldLogsAsync()
        {
            IsCleaningStorage = true;
            CleanupResultText = "Pulizia log in corso…";
            try
            {
                var res = await Task.Run(() => StorageMaintenanceService.CleanOldLogsAndCrashes());
                CleanupResultText = res.Message;
                await RefreshStorageAnalysisAsync();
                return res;
            }
            finally
            {
                IsCleaningStorage = false;
            }
        }

        public string GenerateDiagnosticReport()
        {
            return $"""
                ============================================================
                FLOW CLIENT — SYSTEM DIAGNOSTIC REPORT
                Generated: {DateTime.Now:yyyy-MM-dd HH:mm:ss}
                ============================================================
                App Version:      {CurrentVersion} ({ReleaseDate})
                Installed:        {InstallDate}
                OS:               {OsInfo}
                CPU:              {CpuName}
                GPU:              {GpuName}
                System RAM:       {TotalRam}
                Allocated RAM:    {DefaultRamGbText} ({_defaultRamMb} MB)
                Primary Java:     {JavaVersion}
                Java Executable:  {EffectiveJavaPath}
                Game Directory:   {MinecraftDirectory}
                Client Directory: {LauncherDataDirectory}
                Install Path:     {OfficialInstallDirectory}
                ============================================================
                """;
        }

        public string LaunchBehaviorSummary =>
            PostLaunchAction switch
            {
                LaunchPostAction.Minimize => "Riduci a icona",
                LaunchPostAction.HideWhilePlaying => "Nascondi durante il gioco",
                LaunchPostAction.CloseWhenGameReady => "Chiudi all'avvio del gioco",
                _ => "Mantieni aperto"
            };

        public string FlowClientModSummary => EnableFlowClientMod ? "Attivo" : "Disattivato";
        public string VoiceChatSummary => EnableVoiceChatMod ? "Installazione automatica" : "Disattivato";
        public string DiscordRpcSummary => EnableDiscordRpc ? "Rich Presence attiva" : "Disattivato";

        public void LoadSystemInfo()
        {
            try
            {
                var info = new LauncherInfoService().Gather();
                JavaPath = info.JavaPath;
                JavaVersion = info.JavaVersion;
                TotalRam = info.RamTotal;
                CpuName = info.CpuName;
                GpuName = info.GpuName;
                OsInfo = $"{info.OsName}";

                CurrentVersion = $"v{AppVersionInfoService.GetCurrentVersion()}";
                ReleaseDate = AppVersionInfoService.GetReleaseDateString();
                InstallDate = AppVersionInfoService.GetDownloadOrInstallDateString();
            }
            catch { }
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        public void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
