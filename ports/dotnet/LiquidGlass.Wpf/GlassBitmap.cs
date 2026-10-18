// Apache-2.0. Draw Skia glass into a WPF WriteableBitmap; the host shows it with an Image.
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using SkiaSharp;

namespace LiquidGlass.Wpf;

/// <summary>A device-pixel <see cref="WriteableBitmap"/> that a Skia callback draws into: put it in an
/// <c>Image</c> (Stretch none, at the backdrop's origin) between the backdrop and the labels that sit on
/// the glass. The labels stay ordinary WPF elements, with their layout and accessibility.</summary>
public sealed class GlassBitmap
{
    /// <summary>The bitmap to show. Its size is the backdrop's in device pixels.</summary>
    public WriteableBitmap Bitmap { get; }

    public GlassBitmap(int width, int height, double scale = 1)
    {
        if (width <= 0 || height <= 0) throw new ArgumentOutOfRangeException(nameof(width));
        if (!double.IsFinite(scale) || scale <= 0) throw new ArgumentOutOfRangeException(nameof(scale));
        Bitmap = new WriteableBitmap(width, height, 96 * scale, 96 * scale, PixelFormats.Pbgra32, null);
    }

    /// <summary>Clears to transparent and lets <paramref name="draw"/> paint the glass with a
    /// <see cref="LiquidGlass.Skia.GlassPainter"/> (device pixels, the backdrop's coordinates).</summary>
    public void Render(Action<SKCanvas> draw)
    {
        ArgumentNullException.ThrowIfNull(draw);
        if (!Bitmap.Dispatcher.CheckAccess()) throw new InvalidOperationException("Render on the bitmap's UI thread");
        Bitmap.Lock();
        try
        {
            var info = new SKImageInfo(Bitmap.PixelWidth, Bitmap.PixelHeight, SKColorType.Bgra8888, SKAlphaType.Premul);
            using (var surface = SKSurface.Create(info, Bitmap.BackBuffer, Bitmap.BackBufferStride))
            {
                if (surface is null) throw new InvalidOperationException("Cannot wrap the bitmap's back buffer");
                surface.Canvas.Clear(SKColors.Transparent);
                draw(surface.Canvas);
                surface.Canvas.Flush();
            }
            Bitmap.AddDirtyRect(new Int32Rect(0, 0, Bitmap.PixelWidth, Bitmap.PixelHeight));
        }
        finally { Bitmap.Unlock(); }
    }
}
