using System;

namespace Launcher.Services
{
    public static class InstanceSelectionService
    {
        public static string? SelectedInstanceId { get; private set; }
        public static event Action<string>? SelectedInstanceChanged;

        public static void Select(string id)
        {
            if (string.IsNullOrEmpty(id)) return;
            if (SelectedInstanceId == id) return;

            SelectedInstanceId = id;
            SelectedInstanceChanged?.Invoke(id);
        }

        public static void ForceSelect(string id)
        {
            if (string.IsNullOrEmpty(id)) return;
            SelectedInstanceId = id;
            SelectedInstanceChanged?.Invoke(id);
        }
    }
}