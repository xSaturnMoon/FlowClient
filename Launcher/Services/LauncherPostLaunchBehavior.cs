using System.Windows;

namespace Launcher.Services
{
    public static class LauncherPostLaunchBehavior
    {
        public static void Apply(Window? window)
        {
            if (window == null)
                return;

            var settings = LauncherSettingsService.Instance.Current;

            if (settings.CloseLauncherOnLaunch)
            {
                Application.Current.Shutdown();
                return;
            }

            if (settings.MinimizeLauncherOnLaunch)
                window.WindowState = WindowState.Minimized;
        }
    }
}
