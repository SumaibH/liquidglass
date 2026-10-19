// Apache-2.0. An Avalonia page with glass over its own native content, rendered headlessly with Skia.
//
//   dotnet run --project ports/dotnet/AvaloniaDemo -- DIR   render, check the pixels, save avalonia-glass.png
using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Shapes;
using Avalonia.Headless;
using Avalonia.Layout;
using Avalonia.Media;
using Avalonia.Themes.Fluent;
using LiquidGlass.Avalonia;
using LiquidGlass.Skia;
using SkiaSharp;

internal sealed class App : Application
{
    public override void Initialize() => Styles.Add(new FluentTheme());
}

internal static class Program
{
    private static readonly Rect Card = new(40, 330, 360, 120);
    private static readonly Point Lens = new(520, 90);
    private const double LensSize = 180;

    private static int Main(string[] args)
    {
        var directory = args.Length > 0 ? args[0] : "avalonia-out";
        Directory.CreateDirectory(directory);
        AppBuilder.Configure<App>().UseSkia()
            .UseHeadless(new AvaloniaHeadlessPlatformOptions { UseHeadlessDrawing = false })
            .SetupWithoutStarting();

        // A page of ordinary Avalonia controls: colour bands, a title, text and a button.
        var backdrop = new Canvas { Width = 800, Height = 520, Background = new SolidColorBrush(Color.FromRgb(246, 243, 248)) };
        var colours = new[] { Color.FromRgb(227, 93, 106), Color.FromRgb(76, 141, 246), Color.FromRgb(60, 196, 124), Color.FromRgb(242, 179, 61), Color.FromRgb(155, 108, 246) };
        for (var i = 0; i < 18; i++)
        {
            var band = new Rectangle { Width = 26, Height = 900, Fill = new SolidColorBrush(colours[i % colours.Length]), RenderTransform = new RotateTransform(30) };
            Canvas.SetLeft(band, -140 + i * 60); Canvas.SetTop(band, -200);
            backdrop.Children.Add(band);
        }
        var title = new TextBlock { Text = "Your Avalonia page", FontSize = 40, FontWeight = FontWeight.Bold, Foreground = Brushes.Black };
        Canvas.SetLeft(title, 40); Canvas.SetTop(title, 40);
        var button = new Button { Content = "A native button", FontSize = 18 };
        Canvas.SetLeft(button, 40); Canvas.SetTop(button, 220);
        backdrop.Children.Add(title); backdrop.Children.Add(button);

        var glassImage = new Image { Stretch = Stretch.None, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Top, IsHitTestVisible = false };
        var labels = new Canvas { IsHitTestVisible = false };
        var cardLabel = new TextBlock { Text = "Glass card", FontSize = 24, FontWeight = FontWeight.SemiBold, Foreground = Brushes.Black };
        Canvas.SetLeft(cardLabel, Card.X + 24); Canvas.SetTop(cardLabel, Card.Y + 24);
        var cardNote = new TextBlock { Text = "An Avalonia label on LiquidGlass", FontSize = 16, Foreground = new SolidColorBrush(Color.FromRgb(60, 60, 67)) };
        Canvas.SetLeft(cardNote, Card.X + 24); Canvas.SetTop(cardNote, Card.Y + 62);
        labels.Children.Add(cardLabel); labels.Children.Add(cardNote);

        var root = new Grid();
        root.Children.Add(backdrop); root.Children.Add(glassImage); root.Children.Add(labels);
        var window = new Window { Width = 800, Height = 520, Content = root };
        window.Show();
        window.UpdateLayout();

        // Capture the backdrop, draw the glass into a bitmap over it, and let Avalonia compose the frame.
        using var painter = new GlassPainter();
        using var source = VisualBackdrop.Capture(backdrop, new SKColor(246, 243, 248));
        painter.SetSource(source);
        using var glass = new GlassBitmap(source.Width, source.Height);
        glass.Render(canvas =>
        {
            painter.DrawSurface(canvas, SKRect.Create((float)Card.X, (float)Card.Y, (float)Card.Width, (float)Card.Height), 28,
                new GlassSurfaceOptions(LightX: 0, LightY: -1));
            painter.DrawLens(canvas, SKRect.Create((float)(Lens.X - LensSize / 2), (float)(Lens.Y - LensSize / 2), (float)LensSize, (float)LensSize), 1.4f);
        });
        glassImage.Source = glass.Bitmap;
        window.UpdateLayout();

        var frame = window.CaptureRenderedFrame() ?? throw new InvalidOperationException("No frame rendered");
        using var shot = new SKBitmap(new SKImageInfo(frame.PixelSize.Width, frame.PixelSize.Height, VisualBackdrop.ColorTypeOf(frame.Format), SKAlphaType.Premul));
        using (var locked = frame.Lock())
        {
            var rows = shot.RowBytes;
            for (var y = 0; y < shot.Height; y++)
                unsafe { Buffer.MemoryCopy((byte*)locked.Address + (long)y * locked.RowBytes, (byte*)shot.GetPixels() + (long)y * rows, rows, Math.Min(rows, locked.RowBytes)); }
        }
        using (var png = SKImage.FromBitmap(shot).Encode(SKEncodedImageFormat.Png, 100))
        using (var file = File.Create(System.IO.Path.Combine(directory, "avalonia-glass.png"))) png.SaveTo(file);

        // The glass must have drawn: under the card and the lens the frame differs from the backdrop;
        // far from both it does not.
        using var back = SKBitmap.FromImage(source);
        double Block(int cx, int cy, int r)
        {
            var sum = 0; var n = 0;
            for (var y = cy - r; y <= cy + r; y++) for (var x = cx - r; x <= cx + r; x++)
            {
                var a = shot.GetPixel(x, y); var b = back.GetPixel(x, y);
                sum += Math.Abs(a.Red - b.Red) + Math.Abs(a.Green - b.Green) + Math.Abs(a.Blue - b.Blue); n++;
            }
            return sum / (double)n;
        }
        var underCard = Block((int)(Card.X + Card.Width - 60), (int)(Card.Y + Card.Height / 2), 20);
        var underLens = Block((int)Lens.X, (int)Lens.Y, 40);
        var outside = Block(760, 490, 10);
        Console.WriteLine($"AVALONIA_GLASS mean difference under the card {underCard:F1}, under the lens {underLens:F1}, outside {outside:F1}");
        if (underCard < 10 || underLens < 10 || outside > 1)
        {
            Console.Error.WriteLine("The glass did not draw where expected");
            return 1;
        }
        return 0;
    }
}
