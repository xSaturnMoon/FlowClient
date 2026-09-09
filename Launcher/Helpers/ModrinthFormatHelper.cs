namespace Launcher.Helpers
{
    public static class ModrinthFormatHelper
    {
        public static string FormatCount(long value)
        {
            if (value >= 1_000_000_000)
                return $"{value / 1_000_000_000.0:0.#}B";
            if (value >= 1_000_000)
                return $"{value / 1_000_000.0:0.#}M";
            if (value >= 1_000)
                return $"{value / 1_000.0:0.#}K";
            return value.ToString();
        }
    }
}
