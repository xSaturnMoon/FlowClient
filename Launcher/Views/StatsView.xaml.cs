using System.Windows;
using System.Windows.Controls;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class StatsView : UserControl
    {
        public StatsView()
        {
            InitializeComponent();
            Loaded += OnLoaded;
        }

        private StatsViewModel Vm => (StatsViewModel)DataContext;

        private void OnLoaded(object sender, RoutedEventArgs e) => Vm.Load();

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }
    }
}
