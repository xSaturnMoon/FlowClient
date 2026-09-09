using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Threading;
using Launcher.Services;
using Launcher.ViewModels;

namespace Launcher.Views
{
    public partial class ConsoleView : UserControl
    {
        private readonly DispatcherTimer _pollTimer = new() { Interval = TimeSpan.FromSeconds(1.5) };

        public ConsoleView()
        {
            InitializeComponent();
            Loaded += OnLoaded;
            Unloaded += OnUnloaded;
            _pollTimer.Tick += (_, _) => Vm.Refresh();
        }

        private ConsoleViewModel Vm => (ConsoleViewModel)DataContext;

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            Vm.Load();
            UpdatePanelChips();
            LauncherLogService.Instance.LineAdded += OnLauncherLineAdded;
            _pollTimer.Start();
            ScrollToEnd();
        }

        private void OnUnloaded(object sender, RoutedEventArgs e)
        {
            LauncherLogService.Instance.LineAdded -= OnLauncherLineAdded;
            _pollTimer.Stop();
        }

        private void OnLauncherLineAdded()
        {
            Dispatcher.BeginInvoke(() =>
            {
                Vm.OnLauncherLineAdded();
                ScrollToEnd();
            });
        }

        private void PanelChip_Click(object sender, RoutedEventArgs e)
        {
            if (sender is not Button btn || btn.Tag is not string panel)
                return;

            Vm.SelectedPanel = panel;
            UpdatePanelChips();
            ScrollToEnd();
        }

        private void UpdatePanelChips()
        {
            SetChipStyle(ChipLauncher, Vm.SelectedPanel == "launcher");
            SetChipStyle(ChipLog, Vm.SelectedPanel == "log");
            SetChipStyle(ChipCrash, Vm.SelectedPanel == "crash");
        }

        private void SetChipStyle(Button chip, bool active)
        {
            chip.Style = (Style)FindResource(active ? "ExploreNavPillActive" : "ExploreNavPill");
        }

        private void Crash_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is not FrameworkElement el || el.Tag is not CrashReportInfo report)
                return;
            Vm.SelectedCrash = report;
            ScrollToEnd();
        }

        private void Refresh_Click(object sender, RoutedEventArgs e)
        {
            Vm.Refresh();
            ScrollToEnd();
        }

        private void Copy_Click(object sender, RoutedEventArgs e) => Vm.CopyDisplayText();

        private void OpenFolder_Click(object sender, RoutedEventArgs e) => Vm.OpenActiveFolder();

        private void ClearLauncher_Click(object sender, RoutedEventArgs e) => Vm.ClearLauncher();

        private void BackToLaunch_Click(object sender, RoutedEventArgs e)
        {
            if (Window.GetWindow(this) is MainWindow window)
                window.NavigateTo("Launch");
        }

        private void ScrollToEnd()
        {
            if (!Vm.AutoScroll)
                return;

            Dispatcher.BeginInvoke(() =>
            {
                LogScroll.ScrollToEnd();
                LogOutput.CaretIndex = LogOutput.Text.Length;
                LogOutput.ScrollToEnd();
            }, DispatcherPriority.Background);
        }
    }
}
