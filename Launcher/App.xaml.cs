using System;
using System.Windows;
using System.Windows.Threading;
using Launcher.Services;

namespace Launcher;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        System.Threading.Tasks.Task.Run(MinecraftProcessTracker.Instance.CleanupStaleSessions);

        DispatcherUnhandledException += (_, args) =>
        {
            LauncherLogService.Instance.Error(args.Exception.Message);
            MessageBox.Show($"Errore imprevisto:\n{args.Exception.Message}\n\n{args.Exception.StackTrace}",
                "Flow", MessageBoxButton.OK, MessageBoxImage.Error);
            args.Handled = true;
        };
        base.OnStartup(e);
    }
}

