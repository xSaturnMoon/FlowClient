namespace Launcher.ViewModels
{
    public class StatCardViewModel
    {
        public string Label    { get; set; } = string.Empty;
        public string Value    { get; set; } = string.Empty;
        public string Subtitle { get; set; } = string.Empty;
        public string Icon     { get; set; } = string.Empty;
    }

    public class TimelineItemViewModel
    {
        public string Icon     { get; set; } = "•";
        public string Title    { get; set; } = string.Empty;
        public string Detail   { get; set; } = string.Empty;
        public string TimeText { get; set; } = string.Empty;
        public bool   IsError  { get; set; }
        public bool   IsLast   { get; set; }
    }

    public class SessionRowViewModel
    {
        public string Duration  { get; set; } = "—";
        public string Version   { get; set; } = "—";
        public string Loader    { get; set; } = "—";
        public string Fps       { get; set; } = "—";
        public string Ram       { get; set; } = "—";
        public string World     { get; set; } = "—";
        public string Server    { get; set; } = "—";
        public string Crashed   { get; set; } = "No";
        public string ExitCode  { get; set; } = "—";
        public string Start     { get; set; } = "—";
        public string End       { get; set; } = "—";
    }

    public class AchievementViewModel
    {
        public string Title       { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Icon        { get; set; } = "◆";
        public double Progress    { get; set; }
        public bool   Unlocked    { get; set; }
        public string ProgressText { get; set; } = string.Empty;
    }

    public class RecentItemViewModel
    {
        public string Title    { get; set; } = string.Empty;
        public string Subtitle { get; set; } = string.Empty;
        public string Icon     { get; set; } = "·";
    }
}
