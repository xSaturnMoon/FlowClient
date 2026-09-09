using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Launcher.ViewModels
{
    public class LaunchViewModel : INotifyPropertyChanged
    {
        private string? _selectedInstanceId;
        private string _greeting = "Ready to play";
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

        public string PlayButtonText => _isRunning ? "Stop" : "Play";

        public string? StatusMessage
        {
            get => _statusMessage;
            set { _statusMessage = value; OnPropertyChanged(); }
        }

        public ObservableCollection<InstanceListItemViewModel> Instances
        {
            get => _instances;
            set { _instances = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasInstances)); OnPropertyChanged(nameof(ShowEmpty)); }
        }

        private bool _isServersExpanded;
        private ObservableCollection<ServerListItemViewModel> _favoriteServers = new();

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
