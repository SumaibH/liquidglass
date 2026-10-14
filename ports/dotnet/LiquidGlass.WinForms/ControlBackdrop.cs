// Apache-2.0. In-process WinForms rendering only; no desktop capture or foreign window handles.
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using SkiaSharp;

namespace LiquidGlass.WinForms;

/// <summary>Render an application's own control subtree into a caller-owned opaque Skia image.
/// Invoke on the control's UI thread after layout/handle creation. Capture a backdrop-only subtree,
/// with glass overlays outside it. Refresh after content/layout changes, not every glass animation.</summary>
public static class ControlBackdrop
{
    /// <summary>Includes the control border; output origin is its outer top-left in device pixels.
    /// The caller keeps the returned image alive while GlassPainter borrows it, then disposes it.
    /// DrawToBitmap limitations apply: overlapping container order, hidden children and custom
    /// controls must be verified by the host. RichTextBox/ActiveX are rejected, never replaced
    /// with a screen grab. Capture failure does not mutate an existing painter or snapshot.</summary>
    public static SKImage Capture(Control backdrop, Color background)
    {
        ArgumentNullException.ThrowIfNull(backdrop);
        ObjectDisposedException.ThrowIf(backdrop.IsDisposed, backdrop);
        if (!backdrop.IsHandleCreated || backdrop.InvokeRequired)
            throw new InvalidOperationException("Capture a created control on its owning UI thread after layout");
        if (!backdrop.Visible) throw new InvalidOperationException("Capture a visible backdrop subtree");
        if (background.A != 255) throw new ArgumentException("Supply an opaque background", nameof(background));
        var width = backdrop.Width; var height = backdrop.Height;
        if (width <= 0 || height <= 0 || (long)width * height > int.MaxValue / 4)
            throw new ArgumentOutOfRangeException(nameof(backdrop), "Invalid bitmap dimensions");
        ValidateSubtree(backdrop);
        using var bitmap = new Bitmap(width, height, PixelFormat.Format32bppRgb);
        using (var graphics = Graphics.FromImage(bitmap)) graphics.Clear(background);
        backdrop.DrawToBitmap(bitmap, new Rectangle(0, 0, width, height));
        var bits = bitmap.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.ReadOnly, PixelFormat.Format32bppRgb);
        try
        {
            using var native = new SKBitmap(new SKImageInfo(width, height, SKColorType.Bgra8888, SKAlphaType.Opaque));
            var row = new byte[checked(width * 4)];
            for (var y = 0; y < height; y++)
            {
                Marshal.Copy(IntPtr.Add(bits.Scan0, checked(y * bits.Stride)), row, 0, row.Length);
                // Format32bppRgb reserves the fourth byte; make its opacity explicit.
                for (var x = 3; x < row.Length; x += 4) row[x] = 255;
                Marshal.Copy(row, 0, IntPtr.Add(native.GetPixels(), checked(y * native.RowBytes)), row.Length);
            }
            return SKImage.FromBitmap(native) ?? throw new InvalidOperationException("Cannot create the native backdrop image");
        }
        finally { bitmap.UnlockBits(bits); }
    }

    private static void ValidateSubtree(Control root)
    {
        if (root is AxHost or RichTextBox)
            throw new NotSupportedException($"{root.GetType().Name} is not supported by the WinForms bitmap backdrop path");
        foreach (Control child in root.Controls)
            if (child.Visible) ValidateSubtree(child);
    }
}
