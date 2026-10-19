// Apache-2.0. In-process Avalonia rendering only; no desktop capture or foreign windows.
using Avalonia;
using Avalonia.Media.Imaging;
using Avalonia.Platform;
using SkiaSharp;

namespace LiquidGlass.Avalonia;

/// <summary>Render an application's own Avalonia visual subtree into a caller-owned opaque Skia image,
/// the backdrop a <see cref="LiquidGlass.Skia.GlassPainter"/> refracts. Call on the UI thread after
/// layout. Capture a backdrop-only subtree, with glass overlays outside it, and capture again after its
/// content or layout changes, not on every glass animation.</summary>
public static class VisualBackdrop
{
    /// <summary>The visual at <paramref name="scale"/> device pixels per Avalonia unit (the top level's
    /// render scaling), flattened over an opaque <paramref name="background"/> (ARGB, alpha 255).</summary>
    public static SKImage Capture(Visual backdrop, SKColor background, double scale = 1)
    {
        ArgumentNullException.ThrowIfNull(backdrop);
        if (background.Alpha != 255) throw new ArgumentException("Supply an opaque background", nameof(background));
        if (!double.IsFinite(scale) || scale <= 0) throw new ArgumentOutOfRangeException(nameof(scale));
        var width = (int)Math.Ceiling(backdrop.Bounds.Width * scale);
        var height = (int)Math.Ceiling(backdrop.Bounds.Height * scale);
        if (width <= 0 || height <= 0 || (long)width * height > int.MaxValue / 4)
            throw new InvalidOperationException("Capture a visual after layout, with a non-empty size");
        using var target = new RenderTargetBitmap(new PixelSize(width, height), new Vector(96 * scale, 96 * scale));
        target.Render(backdrop);
        var stride = checked(width * 4);
        var pixels = new byte[checked(stride * height)];
        unsafe
        {
            fixed (byte* p = pixels) target.CopyPixels(new PixelRect(0, 0, width, height), (IntPtr)p, pixels.Length, stride);
        }
        // RenderTargetBitmap is premultiplied, in the platform's channel order (BGRA on Windows' and
        // macOS's Skia, RGBA elsewhere). Read it as what it is, then flatten it over the opaque
        // background so the painter receives an opaque image, as the shaders expect.
        using var native = new SKBitmap(new SKImageInfo(width, height, ColorTypeOf(target.Format), SKAlphaType.Premul));
        System.Runtime.InteropServices.Marshal.Copy(pixels, 0, native.GetPixels(), pixels.Length);
        using var flat = new SKBitmap(new SKImageInfo(width, height, SKColorType.Bgra8888, SKAlphaType.Opaque));
        using (var canvas = new SKCanvas(flat))
        {
            canvas.Clear(background);
            using var image = SKImage.FromBitmap(native);
            canvas.DrawImage(image, 0, 0, new SKSamplingOptions(SKFilterMode.Nearest));
        }
        return SKImage.FromBitmap(flat) ?? throw new InvalidOperationException("Cannot create the native backdrop image");
    }

    /// <summary>The Skia colour type for an Avalonia bitmap's pixel format (RGBA or BGRA, 8 bits).</summary>
    public static SKColorType ColorTypeOf(PixelFormat? format) =>
        format == PixelFormat.Rgba8888 ? SKColorType.Rgba8888
        : format is null || format == PixelFormat.Bgra8888 ? SKColorType.Bgra8888
        : throw new NotSupportedException($"Unsupported bitmap format {format}");
}

/// <summary>A device-pixel <see cref="WriteableBitmap"/> a Skia callback draws glass into: show it in an
/// <c>Image</c> (Stretch none, at the backdrop's origin) between the backdrop and the labels that sit on
/// the glass. The labels stay ordinary Avalonia controls, with their layout and accessibility.</summary>
public sealed class GlassBitmap : IDisposable
{
    /// <summary>The bitmap to show. Its size is the backdrop's in device pixels.</summary>
    public WriteableBitmap Bitmap { get; }

    public GlassBitmap(int width, int height, double scale = 1)
    {
        if (width <= 0 || height <= 0) throw new ArgumentOutOfRangeException(nameof(width));
        if (!double.IsFinite(scale) || scale <= 0) throw new ArgumentOutOfRangeException(nameof(scale));
        Bitmap = new WriteableBitmap(new PixelSize(width, height), new Vector(96 * scale, 96 * scale), PixelFormat.Bgra8888, AlphaFormat.Premul);
    }

    /// <summary>Clears to transparent and lets <paramref name="draw"/> paint the glass with a
    /// <see cref="LiquidGlass.Skia.GlassPainter"/> (device pixels, the backdrop's coordinates).</summary>
    public void Render(Action<SKCanvas> draw)
    {
        ArgumentNullException.ThrowIfNull(draw);
        using var frame = Bitmap.Lock();
        var info = new SKImageInfo(frame.Size.Width, frame.Size.Height, SKColorType.Bgra8888, SKAlphaType.Premul);
        using var surface = SKSurface.Create(info, frame.Address, frame.RowBytes)
            ?? throw new InvalidOperationException("Cannot wrap the bitmap's frame buffer");
        surface.Canvas.Clear(SKColors.Transparent);
        draw(surface.Canvas);
        surface.Canvas.Flush();
    }

    public void Dispose() => Bitmap.Dispose();
}
