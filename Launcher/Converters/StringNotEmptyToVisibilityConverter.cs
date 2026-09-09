using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace Launcher.Converters
{
    public class StringNotEmptyToVisibilityConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var hasText = value is string s && !string.IsNullOrWhiteSpace(s);
            var inverse = parameter?.ToString() == "Inverse";
            return (hasText ^ inverse) ? Visibility.Visible : Visibility.Collapsed;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}
