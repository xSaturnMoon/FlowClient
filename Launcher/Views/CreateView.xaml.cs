using System.Windows;
using System.Windows.Controls;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class CreateView : UserControl
    {
        public CreateView()
        {
            InitializeComponent();
        }

        private void LoaderButton_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { DataContext: LoaderOptionViewModel loaderOpt })
            {
                Vm.SelectLoader(loaderOpt.Name);
            }
        }

        private void VersionButton_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button { DataContext: VersionOptionViewModel verOpt })
            {
                Vm.SelectVersion(verOpt.Version);
            }
        }

        private void TxtCustomVersion_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (TxtCustomPlaceholder != null)
            {
                TxtCustomPlaceholder.Visibility = string.IsNullOrEmpty(TxtCustomVersion.Text)
                    ? Visibility.Visible
                    : Visibility.Collapsed;
            }
        }

        private void CreateInstance_Click(object sender, RoutedEventArgs e)
        {
            var inst = Vm.CreateInstance();
            if (inst != null)
            {
                if (Application.Current.MainWindow is MainWindow mw)
                {
                    mw.NavigateTo("Library");
                }
            }
        }
    }
}
