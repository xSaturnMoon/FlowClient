using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace Launcher.Converters
{
    public class ActiveButtonConverter : IMultiValueConverter
    {
        public object Convert(object[] values, Type targetType, object parameter, CultureInfo culture)
        {
            if (values.Length >= 2 && values[0] is string activeButton && values[1] is string buttonTag)
            {
                return activeButton == buttonTag;
            }
            return false;
        }

        public object[] ConvertBack(object value, Type[] targetTypes, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
