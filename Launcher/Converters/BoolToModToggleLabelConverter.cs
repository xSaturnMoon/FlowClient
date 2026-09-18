using System;
using System.Globalization;
using System.Windows.Data;

namespace Launcher.Converters
{
    public class BoolToModToggleLabelConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
            => value is true ? "Disabilita" : "Abilita";

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotImplementedException();
    }
}
