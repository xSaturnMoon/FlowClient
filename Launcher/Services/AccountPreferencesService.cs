using System;
using System.IO;
using System.Text.Json;

namespace Launcher.Services
{
    public class AccountPreferences
    {
        public bool GlassEffect    { get; set; } = true;
        public bool Animations     { get; set; } = true;
        public bool CompactUi      { get; set; } = false;
        public bool ShowGlow       { get; set; } = true;
        public double UiRoundness  { get; set; } = 14;
    }

    public class AccountPreferencesService
    {
        private static readonly string Path = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "FlowLauncher", "preferences.json");

        public AccountPreferences Load()
        {
            try
            {
                if (!File.Exists(Path)) return new AccountPreferences();
                return JsonSerializer.Deserialize<AccountPreferences>(File.ReadAllText(Path)) ?? new();
            }
            catch { return new AccountPreferences(); }
        }

        public void Save(AccountPreferences prefs)
        {
            try
            {
                Directory.CreateDirectory(System.IO.Path.GetDirectoryName(Path)!);
                File.WriteAllText(Path, JsonSerializer.Serialize(prefs,
                    new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }
    }
}
