using System.Windows;
using System.Windows.Controls;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class QuitView : UserControl
    {
        private readonly LauncherInfoService _launcherInfo = new();

        public QuitView()
        {
            InitializeComponent();
            Loaded += (_, _) => LoadInfo();
        }

        private void LoadInfo()
        {
            if (DataContext is not QuitViewModel vm) return;
            var info = _launcherInfo.Gather();
            vm.LauncherVersion = info.LauncherVersion;
            vm.LauncherChannel = info.Channel;
        }

        private void Exit_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is Window window)
                window.Close();
            else
                Application.Current.Shutdown();
        }

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
