using System;
using System.Diagnostics;
using System.Threading.Tasks;
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
            var action = settings.PostLaunchAction;

            switch (action)
            {
                case LaunchPostAction.Minimize:
                    window.WindowState = WindowState.Minimized;
                    break;

                case LaunchPostAction.HideWhilePlaying:
                    window.Hide();
                    Action? restore = null;
                    restore = () =>
                    {
                        MinecraftProcessTracker.Instance.ProcessExited -= restore;
                        window.Dispatcher.BeginInvoke(() =>
                        {
                            try
                            {
                                window.Show();
                                window.WindowState = WindowState.Normal;
                                window.Activate();
                            }
                            catch { }
                        });
                    };
                    MinecraftProcessTracker.Instance.ProcessExited += restore;
                    break;

                case LaunchPostAction.CloseWhenGameReady:
                    _ = WaitForGameReadyAndShutdownAsync(window);
                    break;

                case LaunchPostAction.KeepOpen:
                default:
                    break;
            }
        }

        private static async Task WaitForGameReadyAndShutdownAsync(Window window)
        {
            var tracker = MinecraftProcessTracker.Instance;
            var process = tracker.CurrentProcess;
            if (process == null)
                return;

            // Wait up to 60 seconds for Minecraft window to become active
            var startTime = DateTime.UtcNow;
            while (DateTime.UtcNow - startTime < TimeSpan.FromSeconds(60))
            {
                await Task.Delay(500);

                try
                {
                    if (process.HasExited)
                    {
                        // Game crashed or closed early — do NOT close launcher!
                        _ = window.Dispatcher.BeginInvoke(() =>
                        {
                            window.Show();
                            window.WindowState = WindowState.Normal;
                            window.Activate();
                        });
                        return;
                    }

                    process.Refresh();

                    // Check if main window handle is valid and visible
                    if (process.MainWindowHandle != IntPtr.Zero)
                    {
                        // Give it 2 seconds to finish opening smoothly
                        await Task.Delay(2000);

                        // Safely release the game from JobObject so shutdown doesn't terminate it
                        tracker.ReleaseRunningGame();

                        _ = window.Dispatcher.BeginInvoke(() =>
                        {
                            try { Application.Current.Shutdown(); } catch { }
                        });
                        return;
                    }
                }
                catch
                {
                    return;
                }
            }

            // Fallback: If 60s passed and process is still running with memory > 200MB, release and shutdown
            try
            {
                if (!process.HasExited && process.WorkingSet64 > 200_000_000)
                {
                    tracker.ReleaseRunningGame();
                    _ = window.Dispatcher.BeginInvoke(() =>
                    {
                        try { Application.Current.Shutdown(); } catch { }
                    });
                }
            }
            catch { }
        }
    }
}
