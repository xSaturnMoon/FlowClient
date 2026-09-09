using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace Launcher.Converters
{
    public class BoolToVisibilityConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is bool boolValue)
            {
                bool inverse = parameter?.ToString() == "Inverse";
                return (boolValue ^ inverse) ? Visibility.Visible : Visibility.Collapsed;
            }
            return Visibility.Collapsed;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is Visibility visibility)
            {
                bool inverse = parameter?.ToString() == "Inverse";
                return (visibility == Visibility.Visible) ^ inverse;
            }
            return false;
        }
    }
}
