using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;

namespace Launcher.ViewModels
{
    public class MainViewModel : INotifyPropertyChanged
    {
        private string _activeButton = "Launch";

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
