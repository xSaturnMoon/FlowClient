using System;
using System.IO;
using System.Text.Json;

namespace Launcher.Services
{
    public enum LaunchPostAction
    {
        KeepOpen = 0,
        Minimize = 1,
        HideWhilePlaying = 2,
        CloseWhenGameReady = 3
    }

    public sealed class LauncherSettings
    {
        public LaunchPostAction PostLaunchAction { get; set; } = LaunchPostAction.KeepOpen;

        public bool CloseLauncherOnLaunch
        {
            get => PostLaunchAction == LaunchPostAction.CloseWhenGameReady;
            set
            {
                if (value)
                    PostLaunchAction = LaunchPostAction.CloseWhenGameReady;
                else if (PostLaunchAction == LaunchPostAction.CloseWhenGameReady)
                    PostLaunchAction = LaunchPostAction.KeepOpen;
            }
        }

        public bool MinimizeLauncherOnLaunch
        {
            get => PostLaunchAction == LaunchPostAction.Minimize;
            set
            {
                if (value)
                    PostLaunchAction = LaunchPostAction.Minimize;
                else if (PostLaunchAction == LaunchPostAction.Minimize)
                    PostLaunchAction = LaunchPostAction.KeepOpen;
            }
        }

        public bool EnableFlowClientMod { get; set; } = true;
        public bool EnableVoiceChatMod { get; set; } = true;
        public bool UiAnimations { get; set; } = true;
        public bool AutoCheckUpdates { get; set; } = true;
        public bool ConfirmBeforeStop { get; set; }
        public bool EnableDiscordRpc { get; set; } = true;
        public int DefaultRamMb { get; set; } = 4096;
        public string? CustomJvmArgs { get; set; }
        public string? CustomJavaPath { get; set; }
        public int GameWidth { get; set; } = 1280;
        public int GameHeight { get; set; } = 720;
        public bool IsGameFullscreen { get; set; } = false;

        // Legacy UI prefs (kept for forward compatibility)
        public bool GlassEffect { get; set; } = true;
        public bool CompactUi { get; set; }
        public bool ShowGlow { get; set; } = true;
        public double UiRoundness { get; set; } = 14;
    }

    public sealed class LauncherSettingsService
    {
        private static readonly Lazy<LauncherSettingsService> InstanceLazy =
            new(() => new LauncherSettingsService());

        public static LauncherSettingsService Instance => InstanceLazy.Value;

        private static readonly string SettingsPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "settings.json");

        private static readonly string LegacyPreferencesPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "preferences.json");

        private readonly object _lock = new();
        private LauncherSettings _current;

        private LauncherSettingsService()
        {
            _current = LoadInternal();
        }

        public LauncherSettings Current
        {
            get { lock (_lock) return _current; }
        }

        public void Save(LauncherSettings settings)
        {
            lock (_lock)
            {
                _current = settings;
                Write(settings);
            }
        }

        public void Update(Action<LauncherSettings> mutate)
        {
            lock (_lock)
            {
                mutate(_current);
                Write(_current);
            }
        }

        private static LauncherSettings LoadInternal()
        {
            try
            {
                if (File.Exists(SettingsPath))
                {
                    return JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(SettingsPath))
                           ?? new LauncherSettings();
                }

                if (File.Exists(LegacyPreferencesPath))
                {
                    var legacy = JsonSerializer.Deserialize<LegacyPreferences>(File.ReadAllText(LegacyPreferencesPath));
                    if (legacy != null)
                    {
                        return new LauncherSettings
                        {
                            UiAnimations = legacy.Animations,
                            GlassEffect = legacy.GlassEffect,
                            CompactUi = legacy.CompactUi,
                            ShowGlow = legacy.ShowGlow,
                            UiRoundness = legacy.UiRoundness
                        };
                    }
                }
            }
            catch { }

            return new LauncherSettings();
        }

        private static void Write(LauncherSettings settings)
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(SettingsPath)!);
                File.WriteAllText(SettingsPath, JsonSerializer.Serialize(settings,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }

        private sealed class LegacyPreferences
        {
            public bool GlassEffect { get; set; } = true;
            public bool Animations { get; set; } = true;
            public bool CompactUi { get; set; }
            public bool ShowGlow { get; set; } = true;
            public double UiRoundness { get; set; } = 14;
        }
    }
}
