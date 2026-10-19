// Apache-2.0. A MAUI page with glass over its own scrolling content: a light card and a dark bar.
using LiquidGlass.Maui;
using Microsoft.Maui.Controls.Shapes;

namespace MauiDemo;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp() =>
        MauiApp.CreateBuilder().UseMauiApp<App>().UseLiquidGlass().Build();
}

public sealed class App : Application
{
    protected override Window CreateWindow(IActivationState? activationState) => new(new DemoPage());
}

public sealed class DemoPage : ContentPage
{
    private uint lastCapture;
    private static readonly Color[] Swatches =
        [Color.FromRgb(227, 93, 106), Color.FromRgb(76, 141, 246), Color.FromRgb(60, 196, 124), Color.FromRgb(242, 179, 61), Color.FromRgb(155, 108, 246)];

    public DemoPage()
    {
        BackgroundColor = Color.FromRgb(246, 243, 248);
        // The backdrop: ordinary MAUI views in a ScrollView. Glass sits outside it, over it.
        var rows = new VerticalStackLayout { Padding = new Thickness(20, 190, 20, 140), Spacing = 14 };
        rows.Add(new Label { Text = "Your MAUI page", FontSize = 34, FontAttributes = FontAttributes.Bold, TextColor = Colors.Black });
        for (var i = 0; i < 24; i++)
        {
            var row = new Grid { ColumnDefinitions = [new ColumnDefinition(56), new ColumnDefinition(GridLength.Star)], ColumnSpacing = 14 };
            row.Add(new Border { BackgroundColor = Swatches[i % Swatches.Length], StrokeThickness = 0, HeightRequest = 56, StrokeShape = new RoundRectangle { CornerRadius = 14 } });
            var text = new VerticalStackLayout { VerticalOptions = LayoutOptions.Center };
            text.Add(new Label { Text = $"Row {i + 1}: a native label", FontSize = 18, FontAttributes = FontAttributes.Bold, TextColor = Colors.Black });
            text.Add(new Label { Text = "Scrolls under the glass, refracted live", FontSize = 14, TextColor = Color.FromRgb(60, 60, 67) });
            row.Add(text, 1);
            rows.Add(row);
        }
        var scroller = new ScrollView { Content = rows };
        var backdrop = new GlassBackdrop(rows, BackgroundColor);
        // One logcat line (tag DOTNET) per capture: what device checks look for.
        backdrop.Changed += (_, _) =>
        {
            if (backdrop.Image is { } image && image.UniqueId != lastCapture)
            {
                lastCapture = image.UniqueId;
                Console.WriteLine($"LiquidGlass backdrop {image.Width}x{image.Height} at {backdrop.Scale:0.###} px/unit");
            }
        };

        var card = new GlassSurface
        {
            Backdrop = backdrop, Radius = 28, Margin = new Thickness(20, 56, 20, 0), HeightRequest = 116,
            VerticalOptions = LayoutOptions.Start, Padding = new Thickness(22, 18),
            Content = new VerticalStackLayout
            {
                Children =
                {
                    new Label { Text = "Glass card", FontSize = 24, FontAttributes = FontAttributes.Bold, TextColor = Colors.Black },
                    new Label { Text = "MAUI labels on LiquidGlass", FontSize = 16, TextColor = Color.FromRgb(60, 60, 67) },
                },
            },
        };
        var tabs = new Grid { ColumnDefinitions = [new(GridLength.Star), new(GridLength.Star), new(GridLength.Star), new(GridLength.Star)] };
        string[] names = ["Home", "Search", "Library", "Profile"];
        for (var i = 0; i < names.Length; i++)
            tabs.Add(new Label { Text = names[i], FontSize = 15, FontAttributes = i == 0 ? FontAttributes.Bold : FontAttributes.None,
                TextColor = i == 0 ? Color.FromRgb(54, 156, 254) : Colors.White, HorizontalOptions = LayoutOptions.Center, VerticalOptions = LayoutOptions.Center }, i);
        var bar = new GlassSurface
        {
            Backdrop = backdrop, Dark = true, Radius = 36, Margin = new Thickness(20, 0, 20, 28), HeightRequest = 72,
            VerticalOptions = LayoutOptions.End, Content = tabs,
        };

        // The backdrop captures the rows by itself once they are drawn, and again if their size changes.
        Content = new Grid { Children = { scroller, card, bar } };
    }
}
