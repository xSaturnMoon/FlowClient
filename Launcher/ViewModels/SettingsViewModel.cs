using System;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
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

        private string _javaPath = "Rilevamento in corso…";
        private string _javaVersion = "—";
        private string _totalRam = "—";
        private string _cpuName = "—";
        private string _gpuName = "—";
        private string _osInfo = "—";
        private string _currentVersion = "v1.0.6";
        private string _releaseDate = "—";
        private string _installDate = "—";
        private string _maintenanceMessage = "";
        private string _updateCheckMessage = "";
        private bool _isCheckingUpdate;

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
                OnPropertyChanged(nameof(IsAppearanceCategory));
                OnPropertyChanged(nameof(IsStorageCategory));
            }
        }

        public bool IsLaunchCategory => SelectedCategory == "Launch";
        public bool IsPerformanceCategory => SelectedCategory == "Performance";
        public bool IsModsCategory => SelectedCategory == "Mods";
        public bool IsAppearanceCategory => SelectedCategory == "Appearance";
        public bool IsStorageCategory => SelectedCategory == "Storage";

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
            }
        }

        public string DefaultRamGbText => $"{_defaultRamMb / 1024} GB";

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

        public string JavaPath
        {
            get => _javaPath;
            set { _javaPath = value; OnPropertyChanged(); }
        }

        public string JavaVersion
        {
            get => _javaVersion;
            set { _javaVersion = value; OnPropertyChanged(); }
        }

        public string TotalRam
        {
            get => _totalRam;
            set { _totalRam = value; OnPropertyChanged(); }
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

        public bool IsCheckingUpdate
        {
            get => _isCheckingUpdate;
            set { _isCheckingUpdate = value; OnPropertyChanged(); }
        }

        public string InstancesDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");

        public string LauncherDataDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "FlowLauncher");

        public string OfficialInstallDirectory => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "FlowClient");

        public string LaunchBehaviorSummary =>
            PostLaunchAction switch
            {
                LaunchPostAction.Minimize => "Riduci a icona",
                LaunchPostAction.HideWhilePlaying => "Nascondi mentre giochi",
                LaunchPostAction.CloseWhenGameReady => "Chiudi ad avvio pronto",
                _ => "Resta aperto"
            };

        public string FlowClientModSummary => EnableFlowClientMod ? "Attivo" : "Disattivato";
        public string VoiceChatSummary => EnableVoiceChatMod ? "Installazione automatica" : "Disattivato";
        public string DiscordRpcSummary => EnableDiscordRpc ? "Rich Presence attiva" : "Disattivata";

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
