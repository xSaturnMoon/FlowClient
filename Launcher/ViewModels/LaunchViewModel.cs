using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Media;

namespace Launcher.ViewModels
{
    public class LaunchViewModel : INotifyPropertyChanged
    {
        private string? _selectedInstanceId;
        private string _greeting = "Bentornato";
        private string _playerName = "Player";
        private ImageSource? _playerAvatar;
        private bool _isAuthenticated;
        private bool _isDiscordRpcConnected;
        private string _discordRpcStatus = "Discord RPC Attivo";

        private string _selectedName = "";
        private string _selectedVersion = "";
        private string _selectedLoader = "";
        private string? _selectedLoaderIconUri;
        private string _selectedRamText = "";
        private string _selectedLastPlayed = "";
        private string? _statusMessage;
        private bool _canPlay;
        private bool _isRunning;

        private ObservableCollection<InstanceListItemViewModel> _instances = new();
        private ObservableCollection<ServerListItemViewModel> _favoriteServers = new();
        private bool _isServersExpanded = true;

        public string? SelectedInstanceId
        {
            get => _selectedInstanceId;
            set
            {
                _selectedInstanceId = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasSelection));
                OnPropertyChanged(nameof(ShowEmpty));
            }
        }

        public bool HasSelection => !string.IsNullOrEmpty(_selectedInstanceId);
        public bool HasInstances => _instances.Count > 0;
        public bool ShowEmpty => !HasInstances;

        public string Greeting
        {
            get => _greeting;
            set { _greeting = value; OnPropertyChanged(); }
        }

        public string PlayerName
        {
            get => _playerName;
            set { _playerName = value; OnPropertyChanged(); }
        }

        public ImageSource? PlayerAvatar
        {
            get => _playerAvatar;
            set
            {
                _playerAvatar = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasPlayerAvatar));
            }
        }

        public bool HasPlayerAvatar => _playerAvatar != null;

        public bool IsAuthenticated
        {
            get => _isAuthenticated;
            set { _isAuthenticated = value; OnPropertyChanged(); }
        }

        public bool IsDiscordRpcConnected
        {
            get => _isDiscordRpcConnected;
            set { _isDiscordRpcConnected = value; OnPropertyChanged(); }
        }

        public string DiscordRpcStatus
        {
            get => _discordRpcStatus;
            set { _discordRpcStatus = value; OnPropertyChanged(); }
        }

        public string SelectedName
        {
            get => _selectedName;
            set { _selectedName = value; OnPropertyChanged(); }
        }

        public string SelectedVersion
        {
            get => _selectedVersion;
            set { _selectedVersion = value; OnPropertyChanged(); }
        }

        public string SelectedLoader
        {
            get => _selectedLoader;
            set { _selectedLoader = value; OnPropertyChanged(); }
        }

        public string? SelectedLoaderIconUri
        {
            get => _selectedLoaderIconUri;
            set { _selectedLoaderIconUri = value; OnPropertyChanged(); }
        }

        public string SelectedRamText
        {
            get => _selectedRamText;
            set { _selectedRamText = value; OnPropertyChanged(); }
        }

        public string SelectedLastPlayed
        {
            get => _selectedLastPlayed;
            set { _selectedLastPlayed = value; OnPropertyChanged(); }
        }

        public bool CanPlay
        {
            get => _canPlay;
            set { _canPlay = value; OnPropertyChanged(); }
        }

        public bool IsRunning
        {
            get => _isRunning;
            set
            {
                _isRunning = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(PlayButtonText));
            }
        }

        public string PlayButtonText => _isRunning ? "Chiudi Minecraft" : "GIOCA ORA";

        public string? StatusMessage
        {
            get => _statusMessage;
            set { _statusMessage = value; OnPropertyChanged(); }
        }

        public ObservableCollection<InstanceListItemViewModel> Instances
        {
            get => _instances;
            set
            {
                _instances = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasInstances));
                OnPropertyChanged(nameof(ShowEmpty));
            }
        }

        public bool IsServersExpanded
        {
            get => _isServersExpanded;
            set
            {
                _isServersExpanded = value;
                OnPropertyChanged();
            }
        }

        public ObservableCollection<ServerListItemViewModel> FavoriteServers
        {
            get => _favoriteServers;
            set
            {
                _favoriteServers = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasServers));
                OnPropertyChanged(nameof(ShowServersEmpty));
            }
        }

        public bool HasServers => _favoriteServers.Count > 0;
        public bool ShowServersEmpty => _favoriteServers.Count == 0;

        public void NotifyServersChanged()
        {
            OnPropertyChanged(nameof(HasServers));
            OnPropertyChanged(nameof(ShowServersEmpty));
        }

        public void NotifyListState()
        {
            OnPropertyChanged(nameof(HasInstances));
            OnPropertyChanged(nameof(ShowEmpty));
        }

        public event PropertyChangedEventHandler? PropertyChanged;

        protected void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
