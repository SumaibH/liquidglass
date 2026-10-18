// Apache-2.0. A WPF page with glass over its own native content.
//
//   dotnet run --project ports/dotnet/WpfDemo                    a window; drag the lens
//   dotnet run --project ports/dotnet/WpfDemo -- --capture DIR   render offscreen, check, save PNGs, exit
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using LiquidGlass.Skia;
using LiquidGlass.Wpf;
using SkiaSharp;

internal static class Program
{
    [STAThread]
    private static int Main(string[] args)
    {
        var capture = args.Length == 2 && args[0] == "--capture" ? args[1] : null;
        var app = new Application { ShutdownMode = ShutdownMode.OnMainWindowClose };
        var page = new GlassPage();
        if (capture is not null) return page.CaptureOffscreen(capture);
        var window = new Window { Title = "LiquidGlass - WPF", Content = page.Root, Width = 820, Height = 560, Background = Brushes.Black };
        window.Loaded += (_, _) => page.Refresh(VisualTreeHelper.GetDpi(window).DpiScaleX);
        return app.Run(window);
    }
}

/// <summary>The backdrop (ordinary WPF elements), the glass bitmap over it, and labels on the glass.
/// Only the backdrop subtree is captured, so the glass never refracts itself.</summary>
internal sealed class GlassPage
{
    public Grid Root { get; } = new();
    private readonly Canvas backdrop = new() { Background = new SolidColorBrush(Color.FromRgb(246, 243, 248)) };
    private readonly Image glassImage = new() { Stretch = Stretch.None, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Top, IsHitTestVisible = false };
    private readonly Canvas labels = new() { IsHitTestVisible = false };
    private readonly GlassPainter painter = new();
    private SKImage? snapshot;
    private GlassBitmap? glass;
    private double scale = 1;
    private Point lens = new(520, 90);

    private static readonly Rect Card = new(40, 330, 360, 120);
    private const double LensSize = 180;

    public GlassPage()
    {
        // A colourful page made of ordinary WPF elements: bands, a gradient, text and a button.
        var colours = new[] { Color.FromRgb(227, 93, 106), Color.FromRgb(76, 141, 246), Color.FromRgb(60, 196, 124), Color.FromRgb(242, 179, 61), Color.FromRgb(155, 108, 246) };
        for (var i = 0; i < 18; i++)
        {
            var band = new Rectangle { Width = 26, Height = 900, Fill = new SolidColorBrush(colours[i % colours.Length]), RenderTransform = new RotateTransform(30) };
            Canvas.SetLeft(band, -140 + i * 60); Canvas.SetTop(band, -200);
            backdrop.Children.Add(band);
        }
        var title = new TextBlock { Text = "Your WPF page", FontSize = 40, FontWeight = FontWeights.Bold, Foreground = Brushes.Black };
        Canvas.SetLeft(title, 40); Canvas.SetTop(title, 40);
        var body = new TextBlock { Text = "Ordinary WPF text and controls.\nThe glass refracts their pixels.", FontSize = 20, Foreground = Brushes.Black, Background = new SolidColorBrush(Color.FromArgb(200, 255, 255, 255)), Padding = new Thickness(8) };
        Canvas.SetLeft(body, 40); Canvas.SetTop(body, 120);
        var button = new Button { Content = "A native button", FontSize = 18, Padding = new Thickness(16, 8, 16, 8) };
        Canvas.SetLeft(button, 40); Canvas.SetTop(button, 220);
        backdrop.Children.Add(title); backdrop.Children.Add(body); backdrop.Children.Add(button);

        var cardLabel = new TextBlock { Text = "Glass card", FontSize = 24, FontWeight = FontWeights.SemiBold, Foreground = Brushes.Black };
        Canvas.SetLeft(cardLabel, Card.Left + 24); Canvas.SetTop(cardLabel, Card.Top + 24);
        var cardNote = new TextBlock { Text = "A WPF label on LiquidGlass", FontSize = 16, Foreground = new SolidColorBrush(Color.FromRgb(60, 60, 67)) };
        Canvas.SetLeft(cardNote, Card.Left + 24); Canvas.SetTop(cardNote, Card.Top + 62);
        labels.Children.Add(cardLabel); labels.Children.Add(cardNote);

        Root.Children.Add(backdrop); Root.Children.Add(glassImage); Root.Children.Add(labels);
        Root.MouseMove += (_, e) =>
        {
            if (e.LeftButton != MouseButtonState.Pressed) return;
            lens = e.GetPosition(Root); DrawGlass();
        };
        button.Click += (_, _) => { title.Text = title.Text == "Your WPF page" ? "Clicked: recaptured" : "Your WPF page"; Dispatcher().BeginInvoke(() => Refresh(scale)); };
    }

    private System.Windows.Threading.Dispatcher Dispatcher() => Root.Dispatcher;

    /// <summary>Captures the backdrop again (after its content or layout changed) and redraws the glass.</summary>
    public void Refresh(double dpiScale)
    {
        scale = dpiScale;
        var next = VisualBackdrop.Capture(backdrop, Color.FromRgb(246, 243, 248), scale);
        try { painter.SetSource(next); } catch { next.Dispose(); throw; }
        var previous = snapshot; snapshot = next; previous?.Dispose();
        if (glass is null || glass.Bitmap.PixelWidth != next.Width || glass.Bitmap.PixelHeight != next.Height)
        {
            glass = new GlassBitmap(next.Width, next.Height, scale);
            glassImage.Source = glass.Bitmap;
        }
        DrawGlass();
    }

    private void DrawGlass()
    {
        if (glass is null) return;
        var s = (float)scale;
        glass.Render(canvas =>
        {
            painter.DrawSurface(canvas, SKRect.Create((float)Card.X * s, (float)Card.Y * s, (float)Card.Width * s, (float)Card.Height * s), 28 * s,
                new GlassSurfaceOptions(Density: s, LightX: 0, LightY: -1));
            painter.DrawLens(canvas, SKRect.Create((float)(lens.X - LensSize / 2) * s, (float)(lens.Y - LensSize / 2) * s, (float)LensSize * s, (float)LensSize * s), 1.4f);
        });
    }

    /// <summary>Lays the page out at 800 x 520 without a window, renders it, checks that the glass changed
    /// the pixels under the card and the lens and left the rest alone, and saves the result.</summary>
    public int CaptureOffscreen(string directory)
    {
        Directory.CreateDirectory(directory);
        Root.Width = 800; Root.Height = 520;
        Root.Measure(new Size(800, 520)); Root.Arrange(new Rect(0, 0, 800, 520)); Root.UpdateLayout();
        Refresh(1);
        Root.UpdateLayout();
        var frame = new RenderTargetBitmap(800, 520, 96, 96, PixelFormats.Pbgra32);
        frame.Render(Root);
        var encoder = new PngBitmapEncoder(); encoder.Frames.Add(BitmapFrame.Create(frame));
        using (var file = File.Create(System.IO.Path.Combine(directory, "wpf-glass.png"))) encoder.Save(file);

        // The glass must have drawn: under the card and the lens the frame differs from the backdrop;
        // far from both it does not.
        using var bitmap = SKBitmap.FromImage(snapshot!);
        var pixels = new byte[800 * 520 * 4];
        frame.CopyPixels(pixels, 800 * 4, 0);
        int Diff(int x, int y)
        {
            var i = (y * 800 + x) * 4;
            var c = bitmap.GetPixel(x, y);
            return Math.Abs(pixels[i + 2] - c.Red) + Math.Abs(pixels[i + 1] - c.Green) + Math.Abs(pixels[i] - c.Blue);
        }
        // Mean difference over a block: a single pixel can land where a band meets its own colour.
        double Block(int cx, int cy, int r)
        {
            var sum = 0; var n = 0;
            for (var y = cy - r; y <= cy + r; y++) for (var x = cx - r; x <= cx + r; x++) { sum += Diff(x, y); n++; }
            return sum / (double)n;
        }
        var underCard = Block((int)(Card.X + Card.Width - 60), (int)(Card.Y + Card.Height / 2), 20);
        var underLens = Block((int)lens.X, (int)lens.Y, 40);
        var outside = Block(760, 490, 10);
        Console.WriteLine($"WPF_GLASS mean difference under the card {underCard:F1}, under the lens {underLens:F1}, outside {outside:F1}");
        if (underCard < 10 || underLens < 10 || outside > 1)
        {
            Console.Error.WriteLine("The glass did not draw where expected");
            return 1;
        }
        return 0;
    }
}
