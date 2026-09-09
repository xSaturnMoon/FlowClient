using System;
using System.Globalization;
using System.Windows.Data;

namespace Launcher.Converters
{
    public class ProgressToWidthConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is not double progress) return 0.0;
            double max = 200;
            if (parameter is string s && double.TryParse(s, out var parsed))
                max = parsed;
            return Math.Max(0, Math.Min(max, progress / 100.0 * max));
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}
