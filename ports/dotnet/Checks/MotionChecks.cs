using System.Text.Json;
using LiquidGlass.Skia;

internal static class MotionChecks
{
    private static void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
    private static void Near(double a, double b, double epsilon = 1e-8) => Check(Math.Abs(a - b) <= epsilon, $"Motion differs: {a} versus {b}");
    private static double[] Values(GlassFeedback f) => [f.A, f.B, f.C, f.D, f.OffsetX, f.OffsetY, f.Press];

    public static object Run()
    {
        using var fixture = JsonDocument.Parse(File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "fixtures", "calm-motion.json")));
        var count = 0;
        foreach (var scenario in fixture.RootElement.GetProperty("cases").EnumerateArray())
        {
            var o = scenario.GetProperty("options");
            double Get(string key) => o.GetProperty(key).GetDouble();
            var shape = o.GetProperty("pullShape").GetString() == "adaptive" ? GlassPullShape.Adaptive : GlassPullShape.AreaPreserving;
            var geometry = new CalmGeometry(Get("width"), Get("height"), Get("radius"), Get("x"), Get("y"), Get("viewportWidth"), Get("viewportHeight"));
            var options = shape == GlassPullShape.Adaptive ? new CalmOptions() : new CalmOptions(1.05, 0, shape);
            var controller = new CalmInteraction(geometry, options);
            foreach (var sample in scenario.GetProperty("samples").EnumerateArray())
            {
                var args = sample.GetProperty("args"); var time = sample.GetProperty("time").GetDouble();
                var operation = sample.GetProperty("operation").GetString();
                var actual = operation switch
                {
                    "press" => controller.Press(time, args[0].GetDouble(), args[1].GetDouble()),
                    "sample" => controller.Sample(time),
                    "release" => controller.Release(time),
                    "setReducedMotion" => controller.SetReducedMotion(args[0].GetBoolean(), time),
                    "resize" => controller.Resize(geometry with { Width = args[0].GetProperty("width").GetDouble(), Height = args[0].GetProperty("height").GetDouble() }, time),
                    _ => throw new Exception("Unknown fixture operation")
                };
                var expected = sample.GetProperty("expected");
                var values = expected.GetProperty("matrix").EnumerateArray().Select(n => n.GetDouble())
                    .Concat(expected.GetProperty("offset").EnumerateArray().Select(n => n.GetDouble()))
                    .Append(expected.GetProperty("press").GetDouble()).ToArray();
                for (var i = 0; i < values.Length; i++) Near(Values(actual)[i], values[i]);
                Check(actual.Held == expected.GetProperty("held").GetBoolean() && actual.Active == expected.GetProperty("active").GetBoolean(), "Native motion lifecycle differs");
                count++;
            }
        }

        // Independent outline containment across both shapes, all edges/corners and press/release.
        var outlines = 0;
        foreach (var shape in Enum.GetValues<GlassPullShape>())
        foreach (var size in new[] { (56.0, 56.0, 28.0), (278.0, 62.0, 31.0), (320.0, 180.0, 20.0) })
        foreach (var origin in new[] { (0.0, 0.0), (400 - size.Item1, 0.0), (0.0, 300 - size.Item2), (400 - size.Item1, 300 - size.Item2), (20.0, 20.0) })
        for (var direction = 0; direction < 8; direction++)
        {
            var (w, h, r) = size; var (x, y) = origin; var angle = direction * Math.PI / 4;
            var c = new CalmInteraction(new(w, h, r, x, y, 400, 300), new(PullShape: shape));
            c.Press(0, 1e6 * Math.Cos(angle), 1e6 * Math.Sin(angle));
            foreach (var time in new[] { .03, .1, .3, 1.0, 1.1, 1.5, 4.0 })
            {
                var f = time == 1 ? c.Release(time) : c.Sample(time);
                for (var k = 0; k < 64; k++)
                {
                    var a = k * Math.PI / 32; var ca = Math.Cos(a); var sa = Math.Sin(a);
                    var px = Math.Sign(ca) * (w / 2 - r) + r * ca; var py = Math.Sign(sa) * (h / 2 - r) + r * sa;
                    var tx = x + w / 2 + f.OffsetX + f.A * px + f.B * py;
                    var ty = y + h / 2 + f.OffsetY + f.C * px + f.D * py;
                    Check(tx >= -1e-8 && tx <= 400 + 1e-8 && ty >= -1e-8 && ty <= 300 + 1e-8, "Native outline escaped viewport");
                    outlines++;
                }
            }
            Check(c.Sample(5) == GlassFeedback.Identity, "Released native controller never became idle");
        }

        GlassFeedback Cadence(int hz)
        {
            var motion = new CalmInteraction(new(320, 72, 36, 20, 20, 360, 112));
            var events = Enumerable.Range(0, 121).Select(i => (Time: i / 120.0, Input: true))
                .Concat(Enumerable.Range(0, hz + 1).Select(i => (Time: i / (double)hz, Input: false)))
                .OrderBy(e => e.Time).ThenBy(e => e.Input);
            foreach (var e in events)
                if (e.Input) motion.Press(e.Time, 600 * Math.Sin(e.Time * 4), 200 * Math.Cos(e.Time * 3));
                else motion.Sample(e.Time);
            motion.Release(1); return motion.Sample(1.08);
        }
        var reference = Values(Cadence(120));
        foreach (var hz in new[] { 30, 60, 90, 144 })
        {
            var actual = Values(Cadence(hz)); for (var i = 0; i < actual.Length; i++) Near(actual[i], reference[i]);
        }
        var valid = new CalmGeometry(200, 60, 30);
        var guard = new CalmInteraction(valid); guard.Press(0, 100, 30); var before = guard.Sample(.2);
        void Rejected(Action action)
        {
            try { action(); } catch (ArgumentOutOfRangeException) { Check(guard.Sample(.2) == before, "Invalid input mutated controller"); return; }
            throw new Exception("Invalid motion input accepted");
        }
        Rejected(() => guard.Press(.1)); Rejected(() => guard.Press(.3, double.NaN));
        Rejected(() => guard.Resize(valid with { Width = 0 }, .3)); Rejected(() => guard.Sample(double.NaN));
        Rejected(() => guard.Resize(valid with { ViewportWidth = 400 }, .3));
        Check(guard.Sample(.1) == before, "Stale frame rewound controller");
        Check(guard.SetReducedMotion(true, .2) == GlassFeedback.Identity, "Reduced motion retained displacement");
        Check(guard.Press(.3, double.MaxValue, -double.MaxValue) == GlassFeedback.Identity with { Held = true }, "Reduced motion animated");
        Check(guard.Sample(double.MaxValue).A == 1, "Large time became nonfinite");
        return new { check = "native calm trajectories, containment, frame cadence, recovery and input guards", portableSamples = count, outlinePoints = outlines, passed = true };
    }
}
