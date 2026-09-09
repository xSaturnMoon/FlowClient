using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Launcher.ViewModels
{
    public class QuitViewModel : INotifyPropertyChanged
    {
        private string _launcherVersion = "—";
        private string _launcherChannel = "Stable";

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

        public event PropertyChangedEventHandler? PropertyChanged;

        protected void OnPropertyChanged([CallerMemberName] string? name = null) =>
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
