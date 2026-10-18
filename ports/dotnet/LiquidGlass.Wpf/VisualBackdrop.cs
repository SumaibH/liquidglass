// Apache-2.0. In-process WPF rendering only; no desktop capture or foreign window handles.
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using SkiaSharp;

namespace LiquidGlass.Wpf;

/// <summary>Render an application's own WPF visual subtree into a caller-owned opaque Skia image, the
/// backdrop a <see cref="LiquidGlass.Skia.GlassPainter"/> refracts. Invoke on the element's UI thread
/// after layout. Capture a backdrop-only subtree, with glass overlays outside it. Refresh after
/// content or layout changes, not on every glass animation.</summary>
public static class VisualBackdrop
{
    /// <summary>The element at its own origin, <paramref name="scale"/> device pixels per WPF unit
    /// (the window's DPI scale; 1 at 96 DPI), flattened over an opaque <paramref name="background"/>.
    /// The caller keeps the returned image alive while a painter borrows it, then disposes it.</summary>
    public static SKImage Capture(FrameworkElement backdrop, Color background, double scale = 1)
    {
        ArgumentNullException.ThrowIfNull(backdrop);
        if (!backdrop.Dispatcher.CheckAccess())
            throw new InvalidOperationException("Capture on the element's owning UI thread");
        if (background.A != 255) throw new ArgumentException("Supply an opaque background", nameof(background));
        if (!double.IsFinite(scale) || scale <= 0) throw new ArgumentOutOfRangeException(nameof(scale));
        var width = (int)Math.Ceiling(backdrop.ActualWidth * scale);
        var height = (int)Math.Ceiling(backdrop.ActualHeight * scale);
        if (width <= 0 || height <= 0 || (long)width * height > int.MaxValue / 4)
            throw new InvalidOperationException("Capture an element after layout, with a non-empty size");

        // A VisualBrush draws the element at the origin regardless of its offset in its parent,
        // which RenderTargetBitmap.Render(element) would otherwise include. Its viewbox is pinned to
        // the element's own bounds: by default it fits the content's bounds, which move whenever a
        // child overflows the element (a rotated band, a negative margin).
        var visual = new DrawingVisual();
        using (var context = visual.RenderOpen())
        {
            var size = new Rect(0, 0, backdrop.ActualWidth, backdrop.ActualHeight);
            context.DrawRectangle(new SolidColorBrush(background), null, size);
            context.DrawRectangle(new VisualBrush(backdrop)
            {
                Viewbox = size, ViewboxUnits = BrushMappingMode.Absolute, Stretch = Stretch.Fill,
            }, null, size);
        }
        var target = new RenderTargetBitmap(width, height, 96 * scale, 96 * scale, PixelFormats.Pbgra32);
        target.Render(visual);

        using var native = new SKBitmap(new SKImageInfo(width, height, SKColorType.Bgra8888, SKAlphaType.Opaque));
        var stride = checked(width * 4);
        var pixels = new byte[checked(stride * height)];
        target.CopyPixels(pixels, stride, 0);
        // Drawn over an opaque background, so every pixel is opaque; state it for Skia.
        for (var i = 3; i < pixels.Length; i += 4) pixels[i] = 255;
        System.Runtime.InteropServices.Marshal.Copy(pixels, 0, native.GetPixels(), pixels.Length);
        return SKImage.FromBitmap(native) ?? throw new InvalidOperationException("Cannot create the native backdrop image");
    }
}
