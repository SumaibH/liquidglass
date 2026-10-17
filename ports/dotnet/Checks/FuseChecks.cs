// Apache-2.0. The bar's fusion with a free button against Kotlin's own geometry
// (ports/skia/fixtures/accessory-fuse.json, GlassAccessoryFusePortFixtureTest), and the neck drawn.
using System.Runtime.InteropServices;
using System.Text.Json;
using LiquidGlass.Skia;
using SkiaSharp;

internal static class FuseChecks
{
    private static void Check(bool condition, string message) { if (!condition) throw new Exception(message); }

    public static object Run()
    {
        using var fixture = JsonDocument.Parse(File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "fixtures", "accessory-fuse.json")));
        var root = fixture.RootElement;
        var bar = root.GetProperty("bar").EnumerateArray().Select(v => v.GetDouble()).ToArray();
        var b = root.GetProperty("button").EnumerateArray().Select(v => v.GetDouble()).ToArray();
        var smoothing = root.GetProperty("smoothing").GetDouble();
        double worst = 0; var cases = 0;
        foreach (var c in root.GetProperty("cases").EnumerateArray())
        {
            double G(string key) => c.GetProperty(key).GetDouble();
            var transform = new FreeButtonTransform(G("pressScale"), G("along"), G("across"), G("angle"), G("tx"), G("ty"), 0, 0, 0);
            var output = AccessoryFuse.Compute(bar[0], bar[1], new RectD(b[0], b[1], b[2] - b[0], b[3] - b[1]), transform, smoothing,
                G("barPressX"), G("barPressY"));
            var expected = c.GetProperty("fuse").EnumerateArray().Select(v => v.GetDouble()).ToArray();
            for (var i = 0; i < 6; i++) worst = Math.Max(worst, Math.Abs(output[i] - expected[i]));
            cases++;
        }
        Check(cases >= 18 && worst < 2e-3, $"Accessory fuse differs from Kotlin by {worst} px");
        foreach (var y in root.GetProperty("yields").EnumerateArray())
        {
            var got = AccessoryFuse.Yield(y.GetProperty("formation").GetDouble(), y.GetProperty("endEdge").GetDouble(), bar[0], bar[1]);
            Check(Math.Abs(got - y.GetProperty("yield").GetDouble()) < 1e-5, $"Accessory yield differs: {y}");
        }

        // Drawn: a 278 x 62 bar with a pressed 62 px button 8 px to its right fills the gap with glass and
        // leaves the bar's middle alone.
        const int w = 420, h = 120;
        using var bitmap = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Opaque));
        bitmap.Erase(SKColors.Black);
        using var image = SKImage.FromBitmap(bitmap);
        using var painter = new GlassPainter(); painter.SetSource(image);
        var rect = SKRect.Create(20, 30, 278, 62);
        var fuse = AccessoryFuse.Compute(278, 62, new RectD(286, 0, 62, 62), new FreeButtonTransform(1.26, 1, 1, 0, 0, 0, 0, 0, 0), 6);
        byte[] Render(float[]? f)
        {
            using var surface = SKSurface.Create(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            surface.Canvas.DrawImage(image, 0, 0, new SKSamplingOptions(SKFilterMode.Linear, SKMipmapMode.None));
            painter.DrawCalmBar(surface.Canvas, rect, 0, new GlassSurfaceOptions(Dark: true), fuse: f);
            surface.Canvas.Flush();
            using var read = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            Check(surface.ReadPixels(read.Info, read.GetPixels(), read.RowBytes, 0, 0), "Pixel read failed");
            var pixels = new byte[w * h * 4]; Marshal.Copy(read.GetPixels(), pixels, 0, pixels.Length); return pixels;
        }
        var plain = Render(null); var fused = Render(fuse);
        int Lum(byte[] p, int x, int y) { var i = (y * w + x) * 4; return p[i] + p[i + 1] + p[i + 2]; }
        Check(Lum(plain, 302, 61) == 0, "The gap was not empty without the fuse");
        Check(Lum(fused, 302, 61) > 60, $"No neck in the gap: {Lum(fused, 302, 61)}");
        Check(Math.Abs(Lum(fused, 150, 61) - Lum(plain, 150, 61)) <= 3, "The fuse changed the bar");
        var threw = false;
        try { Render([1, 2, 3]); } catch (ArgumentOutOfRangeException) { threw = true; }
        Check(threw, "A malformed fuse was accepted");
        return new { check = "bar fusion with a free button (accessory fuse) against Kotlin; neck drawn", cases, worst, passed = true };
    }
}
