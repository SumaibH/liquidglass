// Apache-2.0. The owner's device check of 18 October, ported: a held lens's neck to the search button
// (AccessoryFuse.LensFuse), light Calm's held-lens shadow, and the items sharing the bar's press only on a hold.
using System.Runtime.InteropServices;
using LiquidGlass.Skia;
using SkiaSharp;

internal static class OwnerChecks
{
    private static void Check(bool condition, string message) { if (!condition) throw new Exception(message); }

    private static CalmSelectorController HeldLens(CalmSelectorBar bar, int tab)
    {
        var c = new CalmSelectorController(); c.Attach(bar); c.SnapToRest(tab);
        c.PointerDown(bar.CentreOf(tab), bar.Height / 2, 0, true);
        for (var t = 0.0; t <= .8; t += 1 / 60.0) c.AdvanceFrameTo(t);
        return c;
    }

    public static object Run()
    {
        // LensFuse: none for a resting or tapped lens, the full neck for a formed one.
        var button = new RectD(308, 0, 62, 62);
        Check(AccessoryFuse.LensFuse(300, 62, button, .3, 36) is null, "A lens that is not formed took the neck");
        Check(AccessoryFuse.LensFuse(300, 62, button, 1, 0) is null, "A zero lensFusion drew a neck");
        var full = AccessoryFuse.LensFuse(300, 62, button, 1, 36)!;
        Check(full.SequenceEqual(new float[] { 308 + 31 - 150, 0, 31, 31, 31, 36 }), $"LensFuse {string.Join(",", full)}");
        Check(AccessoryFuse.LensFuse(300, 62, button, .75, 36)![5] < 36, "The neck did not grow with formation");

        // Items share the bar's press only on a hold.
        var navBar = CalmSelectorBar.Create(300, 62, 3);
        var nav = new CalmNavigation(navBar);
        nav.Down(navBar.CentreOf(1), 31, 0); nav.Frame(.05); nav.Up(navBar.CentreOf(1), 31, .1); nav.Frame(.15);
        Check(nav.ItemFollow == 0, $"A tap moved the items ({nav.ItemFollow})");
        nav.Down(navBar.CentreOf(1), 31, 2);
        for (var t = 2.0; t <= 2.9; t += 1 / 60.0) { nav.Move(navBar.CentreOf(1), 31, t); nav.Frame(t); }
        Check(nav.ItemFollow > .9, $"A hold did not carry the items ({nav.ItemFollow})");

        const int w = 420, h = 200;
        var bar = CalmSelectorBar.Create(300, 62, 3);
        var c = HeldLens(bar, 2);
        Check(c.Formation > .9, $"The hold did not form the lens ({c.Formation})");
        var rect = SKRect.Create(20, 70, 300, 62);
        byte[] Draw(SKColor ground, bool dark, float[]? fuse, bool shadow)
        {
            using var bitmap = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Opaque));
            bitmap.Erase(ground);
            using var image = SKImage.FromBitmap(bitmap);
            using var painter = new GlassPainter(); painter.SetSource(image);
            using var surface = SKSurface.Create(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            surface.Canvas.DrawImage(image, 0, 0, new SKSamplingOptions(SKFilterMode.Linear, SKMipmapMode.None));
            painter.DrawCalmBar(surface.Canvas, rect, 0, new GlassSurfaceOptions(Dark: dark));
            using var through = surface.Snapshot();
            painter.DrawSelector(surface.Canvas, rect, c.Render(), (float)c.Formation, new GlassSurfaceOptions(Dark: dark),
                through: through, fuse: fuse, shadow: shadow);
            surface.Canvas.Flush();
            using var read = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            Check(surface.ReadPixels(read.Info, read.GetPixels(), read.RowBytes, 0, 0), "Pixel read failed");
            var pixels = new byte[w * h * 4]; Marshal.Copy(read.GetPixels(), pixels, 0, pixels.Length); return pixels;
        }
        static double Lum(byte[] p, int x, int y) { var i = (y * w + x) * 4; return (p[i] + p[i + 1] + p[i + 2]) / 3.0; }

        // Light Calm's held lens casts its shadow below it, none above.
        var e = c.Extents();
        int cx = (int)Math.Round(rect.Left + (e.Left + e.Right) / 2), below = (int)Math.Round(rect.Top + e.Bottom + 2),
            above = (int)Math.Round(rect.Top + e.Top - 6);
        var lit = Draw(SKColors.White, false, null, true); var plain = Draw(SKColors.White, false, null, false);
        Check(Lum(lit, cx, below) < Lum(plain, cx, below) - 5, $"No shadow below the lens: {Lum(lit, cx, below)} / {Lum(plain, cx, below)}");
        Check(Math.Abs(Lum(lit, cx, above) - Lum(plain, cx, above)) <= 1, "The shadow reached above the lens");

        // Dark held lens at the end: its neck fills the gap to the button.
        var neck = AccessoryFuse.LensFuse(rect.Width, rect.Height, new RectD(rect.Width + 8, 0, 62, 62), c.Formation, 36);
        var ground = new SKColor(20, 20, 20);
        var open = Draw(ground, true, null, true); var fused = Draw(ground, true, neck, true);
        foreach (var dx in new[] { 8, 12 })
        {
            int x = (int)(rect.Right + dx), y = (int)(rect.Top + 31);
            Check(Lum(fused, x, y) > Lum(open, x, y) + 4, $"No neck at {x}: {Lum(fused, x, y)} / {Lum(open, x, y)}");
        }
        var threw = false;
        try { Draw(ground, true, [1, 2, 3], true); } catch (ArgumentOutOfRangeException) { threw = true; }
        Check(threw, "A malformed fuse was accepted");
        return new { check = "owner device check ported: lens neck, light lens shadow, items follow only on a hold", passed = true };
    }
}
