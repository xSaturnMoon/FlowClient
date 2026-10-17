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

        private void ModToggle_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel mod)
            {
                Vm.ToggleMod(mod);
            }
        }

        private void OpenTexturesFolder_Click(object sender, RoutedEventArgs e) => Vm.OpenTexturesFolder();

        private void OpenShadersFolder_Click(object sender, RoutedEventArgs e) => Vm.OpenShadersFolder();

        private void TextureToggle_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel pack)
            {
                Vm.ToggleTexture(pack);
            }
        }

        private void ShaderToggle_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel pack)
            {
                Vm.ToggleShader(pack);
            }
        }

        private void ImportMod_Click(object sender, RoutedEventArgs e) => Vm.ImportMod();

        private void ImportTexture_Click(object sender, RoutedEventArgs e) => Vm.ImportTexture();

        private void ImportShader_Click(object sender, RoutedEventArgs e) => Vm.ImportShader();

        private void DeleteMod_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel mod)
            {
                Vm.DeleteMod(mod);
            }
        }

        private void DeleteTexture_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel pack)
            {
                Vm.DeleteTexture(pack);
            }
        }

        private void DeleteShader_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModItemViewModel pack)
            {
                Vm.DeleteShader(pack);
            }
        }

        private void ModrinthSubtype_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string subtype)
            {
                Vm.ModrinthSubtype = subtype;
            }
        }

        private void ModrinthSearch_Click(object sender, RoutedEventArgs e)
        {
            _ = Vm.SearchModrinthAsync(Vm.ModrinthSearchQuery);
        }

        private void ModrinthSearch_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                _ = Vm.SearchModrinthAsync(Vm.ModrinthSearchQuery);
            }
        }

        private void InstallModrinthItem_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModrinthItemViewModel item)
            {
                _ = Vm.InstallModrinthItemAsync(item);
            }
        }

        private void UninstallModrinthItem_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && fe.DataContext is ModrinthItemViewModel item)
            {
                Vm.UninstallModrinthItem(item);
            }
        }
    }
}
