using System.Net;
using System.Text.RegularExpressions;
using Launcher.ViewModels;

namespace Launcher.Helpers
{
    public static class ModrinthMarkdownParser
    {
        private static readonly Regex ImageTokenRegex = new(
            @"!\[[^\]]*\]\(([^)]+)\)" +
            @"|" +
            @"<img\s+[^>]*?\bsrc\s*=\s*(?:""([^""]+)""|'([^']+)'|([^\s>""']+))[^>]*>",
            RegexOptions.Compiled | RegexOptions.IgnoreCase);

        private static readonly Regex HtmlTagRegex = new(@"<[^>]+>", RegexOptions.Compiled);
        private static readonly Regex BrTagRegex = new(@"<br\s*/?>", RegexOptions.Compiled | RegexOptions.IgnoreCase);
        private static readonly Regex BlockEndRegex = new(@"</(?:p|div|li|h[1-6])>", RegexOptions.Compiled | RegexOptions.IgnoreCase);
        private static readonly Regex MarkdownLinkRegex = new(@"\[([^\]]+)\]\(([^)]+)\)", RegexOptions.Compiled);
        private static readonly Regex BareBracketUrlRegex = new(@"\[(https?://[^\]]+)\]", RegexOptions.Compiled);

        public static List<CatalogContentBlockViewModel> Parse(string? content)
        {
            if (string.IsNullOrWhiteSpace(content))
                return [new CatalogContentBlockViewModel { Text = "No content available." }];

            var blocks = new List<CatalogContentBlockViewModel>();
            var lastIndex = 0;

            foreach (Match match in ImageTokenRegex.Matches(content))
            {
                var before = content[lastIndex..match.Index];
                AddTextBlock(blocks, before);

                var url = FirstNonEmpty(
                    match.Groups[1].Value,
                    match.Groups[2].Value,
                    match.Groups[3].Value,
                    match.Groups[4].Value);

                url = NormalizeImageUrl(url);
                if (!string.IsNullOrEmpty(url))
                    blocks.Add(new CatalogContentBlockViewModel { IsImage = true, ImageUrl = url });

                lastIndex = match.Index + match.Length;
            }

            AddTextBlock(blocks, content[lastIndex..]);

            if (blocks.Count == 0)
                blocks.Add(new CatalogContentBlockViewModel { Text = "No content available." });

            return blocks;
        }

        private static readonly Regex AnchorTagRegex = new(
            @"<a\s+[^>]*?href\s*=\s*(?:""([^""]+)""|'([^']+)'|([^\s>""']+))[^>]*>(.*?)</a>",
            RegexOptions.Compiled | RegexOptions.IgnoreCase | RegexOptions.Singleline);

        public static string StripToPlainText(string raw)
        {
            if (string.IsNullOrWhiteSpace(raw))
                return "";

            var text = raw;
            text = ImageTokenRegex.Replace(text, "");
            text = AnchorTagRegex.Replace(text, m =>
            {
                var href = FirstNonEmpty(m.Groups[1].Value, m.Groups[2].Value, m.Groups[3].Value);
                var label = WebUtility.HtmlDecode(m.Groups[4].Value.Trim());
                if (string.IsNullOrWhiteSpace(label))
                    return href ?? "";
                if (string.IsNullOrWhiteSpace(href) || string.Equals(label, href, StringComparison.OrdinalIgnoreCase))
                    return label;
                return $"{label} ({href})";
            });
            text = BrTagRegex.Replace(text, "\n");
            text = BlockEndRegex.Replace(text, "\n");
            text = MarkdownLinkRegex.Replace(text, "$1 ($2)");
            text = BareBracketUrlRegex.Replace(text, "$1");
            text = Regex.Replace(text, @"```[\s\S]*?```", "", RegexOptions.Multiline);
            text = Regex.Replace(text, @"`([^`]+)`", "$1");
            text = Regex.Replace(text, @"^#+\s*", "", RegexOptions.Multiline);
            text = Regex.Replace(text, @"[*_~]", "");
            text = HtmlTagRegex.Replace(text, "");
            text = WebUtility.HtmlDecode(text);
            text = Regex.Replace(text, @"[ \t]+\n", "\n");
            text = Regex.Replace(text, @"\n{3,}", "\n\n");
            return text.Trim();
        }

        private static void AddTextBlock(List<CatalogContentBlockViewModel> blocks, string segment)
        {
            var text = StripToPlainText(segment);
            if (string.IsNullOrWhiteSpace(text))
                return;

            blocks.Add(new CatalogContentBlockViewModel { Text = text });
        }

        private static string? FirstNonEmpty(params string[] values)
        {
            foreach (var value in values)
            {
                if (!string.IsNullOrWhiteSpace(value))
                    return value.Trim();
            }

            return null;
        }

        private static string? NormalizeImageUrl(string? url)
        {
            if (string.IsNullOrWhiteSpace(url))
                return null;

            url = url.Trim();
            if (url.StartsWith("//", StringComparison.Ordinal))
                return "https:" + url;

            if (Uri.TryCreate(url, UriKind.Absolute, out var absolute) &&
                (absolute.Scheme == Uri.UriSchemeHttp || absolute.Scheme == Uri.UriSchemeHttps))
                return absolute.ToString();

            return null;
        }
    }
}
