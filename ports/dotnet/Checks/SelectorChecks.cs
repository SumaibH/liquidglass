// Apache-2.0. The ported Calm selector against trajectories exported by the Kotlin controller.
using System.Runtime.InteropServices;
using System.Text.Json;
using LiquidGlass.Skia;
using SkiaSharp;

internal static class SelectorChecks
{
    private static void Check(bool condition, string message) { if (!condition) throw new Exception(message); }

    public static object Run()
    {
        using var json = JsonDocument.Parse(File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "fixtures", "selector-calm.json")));
        var root = json.RootElement; var b = root.GetProperty("bar");
        var bar = CalmSelectorBar.Create(b.GetProperty("width").GetDouble(), b.GetProperty("height").GetDouble(), b.GetProperty("count").GetInt32(),
            b.GetProperty("inset").GetDouble(), b.GetProperty("pillWidth").GetDouble(), b.GetProperty("pillInset").GetDouble(), b.GetProperty("density").GetDouble());
        var fields = root.GetProperty("frameFields").EnumerateArray().Select(f => f.GetString()!).ToList();
        int F(string name) => fields.IndexOf(name);
        var tolerance = new Dictionary<string, double>
        {
            ["cx"] = .05, ["cy"] = .05, ["left"] = .05, ["right"] = .05, ["top"] = .05, ["bottom"] = .05,
            ["formation"] = 1e-4, ["transit"] = 1e-3, ["heldTravel"] = 1e-4, ["a0"] = 1e-4, ["a1"] = 1e-4, ["a2"] = 1e-4,
            ["a3"] = 1e-4, ["spine"] = .05, ["radius"] = .05, ["band"] = .05,
        };
        var scenarios = 0; var frames = 0; double worstPx = 0;
        foreach (var scenario in root.GetProperty("scenarios").EnumerateArray())
        {
            var name = scenario.GetProperty("name").GetString();
            var calls = scenario.GetProperty("calls").EnumerateArray().Select(c => c.EnumerateArray().ToArray()).ToList();
            var c = new CalmSelectorController(); c.Attach(bar);
            var rest = calls.FirstOrDefault(call => call[1].GetString() == "rest");
            c.SnapToRest(rest is null ? 0 : (int)rest[2].GetDouble());
            var next = 0;
            foreach (var frame in scenario.GetProperty("frames").EnumerateArray())
            {
                var row = frame.EnumerateArray().ToArray(); var tf = row[F("t")].GetDouble();
                while (next < calls.Count && calls[next][0].GetDouble() <= tf)
                {
                    var call = calls[next++]; var t = call[0].GetDouble();
                    double A(int i) => call[2 + i].GetDouble();
                    switch (call[1].GetString())
                    {
                        case "down": c.PointerDown(A(0), A(1), t, true); break;
                        case "move": c.PointerMove(A(0), A(1), t); break;
                        case "drag": c.BeginDrag(A(0), A(1)); break;
                        case "retarget": c.Retarget((int)A(0)); break;
                        case "up": c.PointerUp((int)A(0)); break;
                    }
                }
                c.AdvanceFrameTo(tf);
                var e = c.Extents(); var pose = c.Render();
                Check(c.Mode.ToString() == row[F("mode")].GetString(), $"{name} t={tf}: mode {c.Mode} vs {row[F("mode")]}");
                var ours = new Dictionary<string, double>
                {
                    ["cx"] = c.CentreX, ["cy"] = c.CentreY, ["formation"] = c.Formation, ["transit"] = c.TransitAppearance,
                    ["heldTravel"] = c.HeldTravel, ["left"] = e.Left, ["right"] = e.Right, ["top"] = e.Top, ["bottom"] = e.Bottom,
                    ["a0"] = pose.A[0], ["a1"] = pose.A[1], ["a2"] = pose.A[2], ["a3"] = pose.A[3],
                    ["spine"] = pose.D[0], ["radius"] = pose.D[1], ["band"] = pose.D[3],
                };
                foreach (var (key, tol) in tolerance)
                {
                    var error = Math.Abs(ours[key] - row[F(key)].GetDouble());
                    Check(error <= tol, $"{name} t={tf} {key}: {ours[key]} vs {row[F(key)]}");
                    if (tol == .05) worstPx = Math.Max(worstPx, error);
                }
                frames++;
            }
            scenarios++;
        }

        // The gesture layer: a tap selects the tab under the finger; a vertical scroll cancels.
        var picks = new List<int>();
        var nav = new CalmNavigation(bar, 0, picks.Add);
        nav.Down(700, 93, .1); nav.Up(700, 93, .16);
        var time = .16;
        while (nav.Frame(time) && time < 3) time += 1.0 / 60;
        Check(picks.SequenceEqual([2]) && time < 2 && Math.Abs(nav.Controller.CentreX - bar.CentreOf(2)) < .5, "Native navigation tap did not settle on its tab");
        nav.Down(417, 93, 3); nav.Move(417, 140, 3.05); nav.Up(417, 140, 3.08);
        Check(picks.Count == 1 && nav.Selected == 2, "Vertical scroll selected a tab");

        // Drawing: the resting selector darkens the bar under it and nothing beyond its node.
        const int w = 360, h = 140;
        using var bitmap = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Opaque));
        for (var y = 0; y < h; y++) for (var x = 0; x < w; x++)
            bitmap.SetPixel(x, y, new SKColor((byte)(30 + x % 180), (byte)(25 + y % 180), (byte)((x + y) % 220)));
        using var image = SKImage.FromBitmap(bitmap);
        using var painter = new GlassPainter(); painter.SetSource(image);
        var small = CalmSelectorBar.Create(300, 62, 3, 6, null, 4, 1);
        var selector = new CalmSelectorController(); selector.Attach(small); selector.SnapToRest(0);
        var rect = SKRect.Create(20, 40, 300, 62);
        byte[] Render(bool withSelector)
        {
            using var surface = SKSurface.Create(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            surface.Canvas.DrawImage(image, 0, 0, new SKSamplingOptions(SKFilterMode.Linear, SKMipmapMode.None));
            painter.DrawSurface(surface.Canvas, rect, 31, new GlassSurfaceOptions(Dark: true));
            if (withSelector) painter.DrawSelector(surface.Canvas, rect, selector.Render(), (float)selector.Formation);
            surface.Canvas.Flush();
            using var read = new SKBitmap(new SKImageInfo(w, h, SKColorType.Rgba8888, SKAlphaType.Premul));
            Check(surface.ReadPixels(read.Info, read.GetPixels(), read.RowBytes, 0, 0), "Pixel read failed");
            var pixels = new byte[w * h * 4];
            Marshal.Copy(read.GetPixels(), pixels, 0, pixels.Length);
            return pixels;
        }
        var bare = Render(false); var with = Render(true);
        int Lum(byte[] p, int x, int y) { var i = (y * w + x) * 4; return p[i] + p[i + 1] + p[i + 2]; }
        Check(new[] { (70, 60), (80, 80), (100, 70) }.All(q => Lum(with, q.Item1, q.Item2) - Lum(bare, q.Item1, q.Item2) < -6),
            "Native resting selector did not darken its bar");
        Check(Lum(with, 290, 71) == Lum(bare, 290, 71), "Native selector changed the far end of the bar");

        return new { check = "Calm selector matches Kotlin trajectories; navigation and drawing", scenarios, frames, worstPx, passed = true };
    }
}
