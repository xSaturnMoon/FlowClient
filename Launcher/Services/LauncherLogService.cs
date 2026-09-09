using System;
using System.Collections.Generic;
using System.Text;

namespace Launcher.Services
{
    public sealed class LauncherLogService
    {
        private static readonly Lazy<LauncherLogService> InstanceLazy = new(() => new LauncherLogService());
        public static LauncherLogService Instance => InstanceLazy.Value;

        private const int MaxLines = 2000;
        private readonly object _lock = new();
        private readonly List<string> _lines = new();

        public event Action? LineAdded;

        private LauncherLogService()
        {
            Write("INFO", "Flow launcher started.");
        }

        public void Info(string message) => Write("INFO", message);
        public void Warn(string message) => Write("WARN", message);
        public void Error(string message) => Write("ERROR", message);

        public void Write(string level, string message)
        {
            if (string.IsNullOrWhiteSpace(message))
                return;

            var line = $"[{DateTime.Now:HH:mm:ss}] {level,-5} {message.Trim()}";
            lock (_lock)
            {
                _lines.Add(line);
                if (_lines.Count > MaxLines)
                    _lines.RemoveRange(0, _lines.Count - MaxLines);
            }

            LineAdded?.Invoke();
        }

        public string GetText()
        {
            lock (_lock)
                return string.Join(Environment.NewLine, _lines);
        }

        public void Clear()
        {
            lock (_lock)
                _lines.Clear();
            LineAdded?.Invoke();
        }

        public IProgress<string> CreateProgress(string? prefix = null) =>
            new Progress<string>(msg =>
            {
                if (string.IsNullOrWhiteSpace(msg))
                    return;
                Write("INFO", string.IsNullOrWhiteSpace(prefix) ? msg : $"{prefix} {msg}");
            });
    }
}
