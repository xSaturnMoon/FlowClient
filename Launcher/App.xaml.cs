using System;
using System.Windows;
using System.Windows.Threading;
using Launcher.Services;

namespace Launcher;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            var ex = args.ExceptionObject as Exception;
            LauncherLogService.Instance.Error($"Unhandled domain exception: {ex?.Message}\n{ex?.StackTrace}");
        };

        System.Threading.Tasks.TaskScheduler.UnobservedTaskException += (_, args) =>
        {
            LauncherLogService.Instance.Error($"Unobserved task exception: {args.Exception?.Message}\n{args.Exception?.StackTrace}");
            args.SetObserved();
        };

        System.Threading.Tasks.Task.Run(MinecraftProcessTracker.Instance.CleanupStaleSessions);

        DispatcherUnhandledException += (_, args) =>
        {
            LauncherLogService.Instance.Error(args.Exception.Message);
            MessageBox.Show($"Unexpected error:\n{args.Exception.Message}\n\n{args.Exception.StackTrace}",
                "Flow Client", MessageBoxButton.OK, MessageBoxImage.Error);
            args.Handled = true;
        };
        base.OnStartup(e);
    }
}

