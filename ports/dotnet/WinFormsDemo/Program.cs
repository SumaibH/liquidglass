// Apache-2.0. A real Windows Forms control using the shared native painter.
using System.Text.Json;
using System.Diagnostics;
using LiquidGlass.Skia;
using SkiaSharp;
using SkiaSharp.Views.Desktop;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        ApplicationConfiguration.Initialize();
        var capture = args.Length == 2 && args[0] == "--capture" ? Path.GetFullPath(args[1]) : null;
        using var form = new Form { Text = "LiquidGlass · native .NET", ClientSize = new Size(960, 620), MinimumSize = new Size(640, 480) };
        using var view = new GlassView(capture is not null) { Dock = DockStyle.Fill, TabStop = true, AccessibleName = "Glass canvas. Drag the lens or use arrow keys. Hold and pull the card. Plus and minus change magnification." };
        var controls = new FlowLayoutPanel { Dock = DockStyle.Top, Height = 48, Padding = new Padding(12, 9, 8, 4), AutoSize = true };
        var dark = new CheckBox { Text = "Dark glass", Checked = true, AutoSize = true };
        var reduced = new CheckBox { Text = "Reduce transparency", AutoSize = true };
        var contrast = new CheckBox { Text = "Increase contrast", AutoSize = true };
        var motion = new CheckBox { Text = "Reduce motion", AutoSize = true };
        var help = new Label { Text = "Drag lens · hold/pull card · arrows · + / −", AutoSize = true, Padding = new Padding(16, 3, 0, 0) };
        controls.Controls.AddRange([dark, reduced, contrast, motion, help]);
        dark.CheckedChanged += (_, _) => { view.Dark = dark.Checked; view.Invalidate(); };
        reduced.CheckedChanged += (_, _) => { view.ReducedTransparency = reduced.Checked; view.Invalidate(); };
        contrast.CheckedChanged += (_, _) => { view.IncreasedContrast = contrast.Checked; view.Invalidate(); };
        motion.CheckedChanged += (_, _) => view.SetReducedMotion(motion.Checked);
        form.Controls.Add(view); form.Controls.Add(controls);
        if (capture is not null)
        {
            Directory.CreateDirectory(capture);
            var stage = 0;
            var advancing = false;
            view.FrameReady += surface =>
            {
                if (stage >= 9 || advancing) return;
                advancing = true;
                using var image = surface.Snapshot();
                using var data = image.Encode(SKEncodedImageFormat.Png, 100);
                using (var file = File.Create(Path.Combine(capture, $"native-{stage}.png"))) data.SaveTo(file);
                stage++;
                // Advance only after paint completes. These are app-owned snapshots, not OS grabs.
                form.BeginInvoke(() =>
                {
                    if (stage == 1) view.ReplayDrag();
                    else if (stage == 2) view.ReplayCardPress();
                    else if (stage == 3) view.ReplayCardPull();
                    else if (stage == 4) view.ReplayCardRelease();
                    else if (stage == 5) view.ReplayRecovery();
                    else if (stage == 6) { motion.Checked = true; view.ReplayReducedPress(); }
                    else if (stage == 7) { dark.Checked = false; reduced.Checked = true; contrast.Checked = true; }
                    else if (stage == 8) form.ClientSize = new Size(720, 520);
                    else
                    {
                        File.WriteAllText(Path.Combine(capture, "host-verification.json"), JsonSerializer.Serialize(new
                        {
                            backend = "SkiaSharp Windows Forms SKControl (CPU)",
                            captures = stage,
                            input = "app-owned OnMouseDown/Move/Up replay, including capture release",
                            dragPassed = view.DragPassed,
                            calmMotionPassed = view.MotionPassed,
                            phases = new[] { "rest", "lens drag", "early card press", "held card pull", "continuous release", "recovered", "reduced motion press", "accessible material", "resize cancellation" },
                            resizePassed = view.Width < 960 && view.ResizeCancelled,
                            limits = "Nine deterministic app-owned phases; no GPU, complete iOS motion, presented FPS or arbitrary native-widget backdrop claim."
                        }, new JsonSerializerOptions { WriteIndented = true }));
                        if (!view.DragPassed || !view.MotionPassed || !view.ResizeCancelled) Environment.ExitCode = 1;
                        form.Close();
                    }
                    advancing = false;
                    view.Invalidate();
                });
            };
        }
        Application.Run(form);
    }
}

internal sealed class GlassView : SKControl
{
    private readonly GlassPainter glass = new();
    private SKImage? source;
    private readonly SKSamplingOptions sampling = new(SKFilterMode.Linear, SKMipmapMode.None);
    private float centreX = .72f, centreY = .38f, magnification = 1.4f;
    private bool dragging;
    private Point grasp;
    private SKPoint graspCentre;
    private readonly System.Windows.Forms.Timer animation = new() { Interval = 16 };
    private readonly bool replay;
    private double replayTime;
    private CalmInteraction? cardMotion;
    private GlassFeedback cardFeedback = GlassFeedback.Identity;
    private SKRect cardBounds;
    private bool cardPressed, reducedMotion;
    private Point cardGrasp;
    private SKRect originalCardBounds;
    private GlassFeedback heldFeedback;
    private double Now => replay ? replayTime : Stopwatch.GetTimestamp() / (double)Stopwatch.Frequency;
    [System.ComponentModel.DefaultValue(true)]
    public bool Dark { get; set; } = true;
    [System.ComponentModel.DefaultValue(false)]
    public bool ReducedTransparency { get; set; }
    [System.ComponentModel.DefaultValue(false)]
    public bool IncreasedContrast { get; set; }
    public bool DragPassed { get; private set; }
    public bool MotionPassed { get; private set; } = true;
    public bool ResizeCancelled => !cardPressed && !dragging && !Capture && cardMotion?.Sample(Now).Held == false;
    public event Action<SKSurface>? FrameReady;

    public GlassView(bool replay = false)
    {
        this.replay = replay;
        animation.Tick += (_, _) => Invalidate();
        PaintSurface += (_, e) =>
        {
            if (e.Info.Width <= 0 || e.Info.Height <= 0) return;
            if (source is null || source.Width != e.Info.Width || source.Height != e.Info.Height)
            {
                using var backdrop = SKSurface.Create(new SKImageInfo(e.Info.Width, e.Info.Height, SKColorType.Rgba8888, SKAlphaType.Opaque));
                PaintPage(backdrop.Canvas, e.Info.Width, e.Info.Height);
                var next = backdrop.Snapshot();
                glass.SetSource(next); source?.Dispose(); source = next;
            }
            var c = e.Surface.Canvas;
            c.DrawImage(source, 0, 0, sampling);
            var scale = e.Info.Width / (float)ClientSize.Width;
            var radius = 86 * scale;
            var x = Math.Clamp(centreX * e.Info.Width, radius + 3, e.Info.Width - radius - 3);
            var y = Math.Clamp(centreY * e.Info.Height, radius + 3, e.Info.Height - radius - 3);
            glass.DrawLens(c, SKRect.Create(x - radius, y - radius, radius * 2, radius * 2), magnification);
            var card = SKRect.Create(30 * scale, e.Info.Height - 140 * scale, Math.Min(450 * scale, e.Info.Width - 60 * scale), 108 * scale);
            cardBounds = SKRect.Create(card.Left / scale, card.Top / scale, card.Width / scale, card.Height / scale);
            var geometry = new CalmGeometry(cardBounds.Width, cardBounds.Height, 24, cardBounds.Left, cardBounds.Top, ClientSize.Width, ClientSize.Height);
            if (cardMotion is null) cardMotion = new(geometry, new(ReducedMotion: reducedMotion));
            else if (cardMotion.Geometry != geometry) { cardMotion.Resize(geometry, Now); cardPressed = false; Capture = false; }
            cardFeedback = cardMotion.Sample(Now);
            if (!cardFeedback.Active) animation.Stop();
            glass.DrawSurface(c, card, 24 * scale, new(Dark: Dark, Density: scale,
                ReducedTransparency: ReducedTransparency ? 1 : 0, IncreasedContrast: IncreasedContrast ? 1 : 0), cardFeedback);
            using var text = new SKPaint { Color = Dark ? new SKColor(229, 243, 244) : new SKColor(21, 42, 58), IsAntialias = true };
            using var title = new SKFont(SKTypeface.Default, 24 * scale);
            using var body = new SKFont(SKTypeface.Default, 14 * scale);
            c.DrawText("A clearer view", card.Left + 22 * scale, card.Top + 40 * scale, SKTextAlign.Left, title, text);
            c.DrawText($"Hold the card · lens at {magnification:P0}", card.Left + 22 * scale, card.Top + 72 * scale, SKTextAlign.Left, body, text);
            c.Flush(); FrameReady?.Invoke(e.Surface);
        };
    }

    private static void PaintPage(SKCanvas canvas, int width, int height)
    {
        using var paint = new SKPaint { IsAntialias = true };
        using var gradient = SKShader.CreateLinearGradient(new(0, 0), new(width, height),
            [new SKColor(16, 36, 57), new SKColor(46, 100, 113)], null, SKShaderTileMode.Clamp);
        paint.Shader = gradient; canvas.DrawPaint(paint); paint.Shader = null;
        paint.Color = new SKColor(144, 188, 202, 100); paint.StrokeWidth = 1;
        var step = Math.Max(20, width / 30f);
        for (float x = 0; x < width; x += step) canvas.DrawLine(x, 0, x, height, paint);
        for (float y = 0; y < height; y += step) canvas.DrawLine(0, y, width, y, paint);
        paint.Color = new SKColor(163, 220, 220, 85); canvas.DrawCircle(width * .66f, height * .30f, width * .12f, paint);
    }

    protected override void OnMouseDown(MouseEventArgs e)
    {
        base.OnMouseDown(e); if (e.Button != MouseButtons.Left) return;
        if (cardBounds.Contains(e.X, e.Y) && cardMotion is not null)
        {
            Focus(); cardPressed = true; cardGrasp = e.Location; Capture = true;
            cardMotion.Press(Now); Animate(); return;
        }
        var dx = e.X - centreX * ClientSize.Width; var dy = e.Y - centreY * ClientSize.Height;
        if (dx * dx + dy * dy > 86 * 86) return;
        Focus(); dragging = true; Capture = true; grasp = e.Location; graspCentre = new(centreX, centreY);
    }
    protected override void OnMouseMove(MouseEventArgs e)
    {
        base.OnMouseMove(e);
        if (cardPressed) { cardMotion!.Press(Now, e.X - cardGrasp.X, e.Y - cardGrasp.Y); Animate(); return; }
        if (!dragging) return;
        centreX = Math.Clamp(graspCentre.X + (e.X - grasp.X) / (float)ClientSize.Width, 89f / ClientSize.Width, 1 - 89f / ClientSize.Width);
        centreY = Math.Clamp(graspCentre.Y + (e.Y - grasp.Y) / (float)ClientSize.Height, 89f / ClientSize.Height, 1 - 89f / ClientSize.Height);
        Invalidate();
    }
    private void Animate() { if (!replay) animation.Start(); Invalidate(); }
    private void ReleaseCard()
    {
        if (!cardPressed) return;
        cardPressed = false; cardMotion!.Release(Now); Animate();
    }
    public void SetReducedMotion(bool value)
    {
        reducedMotion = value; cardMotion?.SetReducedMotion(value, Now);
        cardPressed = false; dragging = false; Capture = false; animation.Stop(); Invalidate();
    }
    protected override void OnMouseUp(MouseEventArgs e)
    {
        base.OnMouseUp(e); if (e.Button != MouseButtons.Left) return;
        ReleaseCard(); dragging = false; Capture = false;
    }
    protected override void OnMouseCaptureChanged(EventArgs e)
    {
        base.OnMouseCaptureChanged(e); if (!Capture) { dragging = false; ReleaseCard(); }
    }
    protected override void OnLostFocus(EventArgs e) { base.OnLostFocus(e); ReleaseCard(); dragging = false; Capture = false; }
    protected override void OnResize(EventArgs e)
    {
        base.OnResize(e); dragging = false; cardPressed = false; Capture = false;
        if (cardMotion is not null) cardMotion.Resize(cardMotion.Geometry, Now);
        if (ClientSize.Width > 178 && ClientSize.Height > 178)
        {
            centreX = Math.Clamp(centreX, 89f / ClientSize.Width, 1 - 89f / ClientSize.Width);
            centreY = Math.Clamp(centreY, 89f / ClientSize.Height, 1 - 89f / ClientSize.Height);
        }
    }
    protected override bool IsInputKey(Keys keyData) => keyData is Keys.Left or Keys.Right or Keys.Up or Keys.Down || base.IsInputKey(keyData);
    protected override void OnKeyDown(KeyEventArgs e)
    {
        base.OnKeyDown(e);
        switch (e.KeyCode)
        {
            case Keys.Left: centreX -= 12f / ClientSize.Width; break;
            case Keys.Right: centreX += 12f / ClientSize.Width; break;
            case Keys.Up: centreY -= 12f / ClientSize.Height; break;
            case Keys.Down: centreY += 12f / ClientSize.Height; break;
            case Keys.Add: case Keys.Oemplus: magnification = Math.Min(2.5f, magnification + .05f); break;
            case Keys.Subtract: case Keys.OemMinus: magnification = Math.Max(1, magnification - .05f); break;
            default: return;
        }
        centreX = Math.Clamp(centreX, 89f / ClientSize.Width, 1 - 89f / ClientSize.Width);
        centreY = Math.Clamp(centreY, 89f / ClientSize.Height, 1 - 89f / ClientSize.Height);
        e.Handled = true; Invalidate();
    }
    public void ReplayDrag()
    {
        var from = new Point((int)(centreX * ClientSize.Width), (int)(centreY * ClientSize.Height));
        OnMouseDown(new(MouseButtons.Left, 1, from.X, from.Y, 0));
        OnMouseMove(new(MouseButtons.Left, 0, from.X - 150, from.Y + 60, 0));
        OnMouseUp(new(MouseButtons.Left, 1, from.X - 150, from.Y + 60, 0));
        DragPassed = !dragging && !Capture && Math.Abs(centreX * ClientSize.Width - (from.X - 150)) <= 1;
    }
    public void ReplayCardPress()
    {
        originalCardBounds = cardBounds; replayTime = .1;
        OnMouseDown(new(MouseButtons.Left, 1, (int)cardBounds.MidX, (int)cardBounds.MidY, 0));
        replayTime = .14;
        var f = cardMotion!.Sample(Now);
        MotionPassed &= cardPressed && f.Press is > 0 and < .2 && cardBounds == originalCardBounds;
    }
    public void ReplayCardPull()
    {
        replayTime = .2;
        OnMouseMove(new(MouseButtons.Left, 0, cardGrasp.X + 1200, cardGrasp.Y - 900, 0));
        replayTime = .6; heldFeedback = cardMotion!.Sample(Now);
        MotionPassed &= heldFeedback.Held && heldFeedback.A > 1 && cardBounds == originalCardBounds;
    }
    public void ReplayCardRelease()
    {
        OnMouseUp(new(MouseButtons.Left, 1, cardGrasp.X + 1200, cardGrasp.Y - 900, 0));
        var f = cardMotion!.Sample(Now);
        MotionPassed &= !cardPressed && !Capture && f.A == heldFeedback.A && f.D == heldFeedback.D && cardBounds == originalCardBounds;
    }
    public void ReplayRecovery()
    {
        replayTime = 3;
        MotionPassed &= cardMotion!.Sample(Now) == GlassFeedback.Identity && cardBounds == originalCardBounds;
    }
    public void ReplayReducedPress()
    {
        replayTime = 3.1;
        OnMouseDown(new(MouseButtons.Left, 1, (int)cardBounds.MidX, (int)cardBounds.MidY, 0));
        OnMouseMove(new(MouseButtons.Left, 0, cardGrasp.X - 1200, cardGrasp.Y + 900, 0));
        replayTime = 4;
        MotionPassed &= cardMotion!.Sample(Now) == GlassFeedback.Identity with { Held = true };
    }
    protected override void Dispose(bool disposing)
    {
        if (disposing) { animation.Stop(); animation.Dispose(); glass.Dispose(); source?.Dispose(); source = null; }
        base.Dispose(disposing);
    }
}
