using System;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;

namespace Launcher.Converters
{
    public class StringToBrushConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is not string s || string.IsNullOrWhiteSpace(s))
                return new SolidColorBrush(Color.FromRgb(0x0A, 0x84, 0xFF));
            try
            {
                var brush = new BrushConverter().ConvertFromString(s) as Brush;
                if (brush != null)
                {
                    if (brush.CanFreeze) brush.Freeze();
                    return brush;
                }
            }
            catch { }
            return new SolidColorBrush(Color.FromRgb(0x0A, 0x84, 0xFF));
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}
