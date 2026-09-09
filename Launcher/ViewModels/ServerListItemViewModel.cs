namespace Launcher.ViewModels
{
    using System.ComponentModel;
    using System.Runtime.CompilerServices;
    using System.Windows.Media;

    public sealed class ServerListItemViewModel : INotifyPropertyChanged
    {
        public string Host { get; init; } = "";
        public int Port { get; init; } = 25565;
        public string Name { get; init; } = "";
        public string Address { get; init; } = "";

        private ImageSource? _icon;
        public ImageSource? Icon
        {
            get => _icon;
            set
            {
                _icon = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(HasIcon));
            }
        }

        public bool HasIcon => Icon != null;
        public string Initial => string.IsNullOrWhiteSpace(Name) ? "?" : Name.Trim()[..1].ToUpperInvariant();

        public event PropertyChangedEventHandler? PropertyChanged;

        private void OnPropertyChanged([CallerMemberName] string? name = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
