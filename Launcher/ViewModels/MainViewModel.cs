using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Media;

namespace Launcher.ViewModels
{
    public class MainViewModel : INotifyPropertyChanged
    {
        private string _activeButton = "Home";
        private string _playerName = "Player";
        private ImageSource? _playerAvatar;
        private bool _isAuthenticated;

        public MainViewModel()
        {
        }

        public string ActiveButton
        {
            get => _activeButton;
            set
            {
                if (_activeButton != value)
                {
                    _activeButton = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(IsHomeActive));
                    OnPropertyChanged(nameof(IsVersionsActive));
                    OnPropertyChanged(nameof(IsCosmeticsActive));
                    OnPropertyChanged(nameof(IsAccountActive));
                    OnPropertyChanged(nameof(IsNewsActive));
                    OnPropertyChanged(nameof(IsSettingsActive));
                }
            }
        }

        public bool IsHomeActive => _activeButton is "Home" or "Launch";
        public bool IsVersionsActive => _activeButton is "Versions" or "Explore";
        public bool IsCosmeticsActive => _activeButton is "Cosmetics" or "Account";
        public bool IsAccountActive => _activeButton is "Account" or "Cosmetics";
        public bool IsNewsActive => _activeButton is "News";
        public bool IsSettingsActive => _activeButton is "Settings";

        public string PlayerName
        {
            get => _playerName;
            set
            {
                if (_playerName != value)
                {
                    _playerName = value;
                    OnPropertyChanged();
                }
            }
        }

        public ImageSource? PlayerAvatar
        {
            get => _playerAvatar;
            set
            {
                if (_playerAvatar != value)
                {
                    _playerAvatar = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(HasPlayerAvatar));
                }
            }
        }

        public bool HasPlayerAvatar => _playerAvatar != null;

        public bool IsAuthenticated
        {
            get => _isAuthenticated;
            set
            {
                if (_isAuthenticated != value)
                {
                    _isAuthenticated = value;
                    OnPropertyChanged();
                }
            }
        }

        private System.Collections.ObjectModel.ObservableCollection<InstanceListItemViewModel> _installedVersions = new();

        public System.Collections.ObjectModel.ObservableCollection<InstanceListItemViewModel> InstalledVersions => _installedVersions;

        public bool HasInstalledVersions => _installedVersions.Count > 0;

        public void NotifyInstalledVersionsChanged()
        {
            OnPropertyChanged(nameof(HasInstalledVersions));
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
        }
    }
}
