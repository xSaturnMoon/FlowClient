using System;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using Launcher.Helpers;

namespace Launcher.Converters
{
    public class StringToImageSourceConverter : IValueConverter
    {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture)
            => value is string uri ? RemoteImageLoader.TryLoad(uri) : null;

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}
