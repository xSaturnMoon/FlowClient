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

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? name = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
        }
    }
}
