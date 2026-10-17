using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class LibraryView : UserControl
    {
        public LibraryViewModel Vm { get; }

        public LibraryView()
        {
            Vm = new LibraryViewModel();
            DataContext = Vm;
            InitializeComponent();
        }

        private void Card_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is LibraryCardItemViewModel item)
            {
                InstanceSelectionService.Select(item.Id);
            }
        }

        private void SettingsButton_Click(object sender, RoutedEventArgs e)
        {
            e.Handled = true;
            if (sender is Button btn && btn.Tag is LibraryCardItemViewModel item)
            {
                Vm.OpenSettings(item);
            }
        }

        private void BackToLibrary_Click(object sender, RoutedEventArgs e)
        {
            Vm.CloseSettings();
        }

        public void ApplySearch(string query)
        {
            if (Vm.IsViewingSettings)
            {
                Vm.CloseSettings();
            }
            Vm.SearchQuery = query;
        }

        private void OpenFolder_Click(object sender, RoutedEventArgs e) => Vm.OpenFolder();

        private void OpenModsFolder_Click(object sender, RoutedEventArgs e) => Vm.OpenModsFolder();

        private void Duplicate_Click(object sender, RoutedEventArgs e) => Vm.DuplicateCurrentInstance();

        private void Delete_Click(object sender, RoutedEventArgs e) => Vm.DeleteCurrentInstance();

        private void InfoTab_Click(object sender, RoutedEventArgs e) => Vm.SettingsTab = "Info";

        private void ModTab_Click(object sender, RoutedEventArgs e) => Vm.SettingsTab = "Mod";

        private void RamPill_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tagStr && int.TryParse(tagStr, out var mb))
            {
                Vm.SetRam(mb);
            }
        }
    }
}
