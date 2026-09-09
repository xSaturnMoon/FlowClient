using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Launcher.ViewModels
{
    public sealed class SettingsViewModel : INotifyPropertyChanged
    {
        private bool _closeLauncherOnLaunch;
        private bool _minimizeLauncherOnLaunch;
        private bool _enableFlowClientMod = true;
        private bool _enableVoiceChatMod = true;
        private bool _uiAnimations = true;
        private bool _autoCheckUpdates = true;
        private bool _confirmBeforeStop;

        public bool CloseLauncherOnLaunch
        {
            get => _closeLauncherOnLaunch;
            set
            {
                if (_closeLauncherOnLaunch == value) return;
                _closeLauncherOnLaunch = value;
                if (value && _minimizeLauncherOnLaunch)
                    MinimizeLauncherOnLaunch = false;
                OnPropertyChanged();
                OnPropertyChanged(nameof(LaunchBehaviorSummary));
            }
        }

        public bool MinimizeLauncherOnLaunch
        {
            get => _minimizeLauncherOnLaunch;
            set
            {
                if (_minimizeLauncherOnLaunch == value) return;
                _minimizeLauncherOnLaunch = value;
                if (value && _closeLauncherOnLaunch)
                    CloseLauncherOnLaunch = false;
                OnPropertyChanged();
                OnPropertyChanged(nameof(LaunchBehaviorSummary));
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

        public string LaunchBehaviorSummary =>
            CloseLauncherOnLaunch ? "Close on launch"
            : MinimizeLauncherOnLaunch ? "Minimize on launch"
            : "Stay open";

        public string FlowClientModSummary => EnableFlowClientMod ? "Enabled" : "Disabled";
        public string VoiceChatSummary => EnableVoiceChatMod ? "Auto-install" : "Off";

        public event PropertyChangedEventHandler? PropertyChanged;

        public void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
