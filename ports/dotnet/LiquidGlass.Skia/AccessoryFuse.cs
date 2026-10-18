// Apache-2.0. The Calm bar's fusion with a free button beside it (Compose: GlassTabBarAccessory.link,
// GlassFuse.toPx / toMaterialPx, accessoryExtents, glassInscribedStadium, glassAccessoryYield), checked
// against ports/skia/fixtures/accessory-fuse.json. iOS 26's Phone search button joins the bar through a
// neck while pressed and breaks away when pulled (IMG_6756, 15 October).
namespace LiquidGlass.Skia;

/// <summary>A rect in double precision: x, y, width, height.</summary>
public readonly record struct RectD(double X, double Y, double Width, double Height);

public static class AccessoryFuse
{
    /// <summary>The largest stadium (half extents and corner radius) inside an axis-aligned ellipse with half axes a, b.</summary>
    public static double[] InscribedStadium(double a, double b)
    {
        if (a <= 0 || b <= 0) return [a, b, Math.Min(a, b)];
        double longAxis = Math.Max(a, b), shortAxis = Math.Min(a, b);
        if (longAxis - shortAxis < 1e-3) return [a, b, shortAxis];
        bool Inside(double c, double r)
        {
            // The cap circle centred on the long axis at c, radius r, sampled every 7.5 degrees.
            for (var i = 0; i <= 12; i++)
            {
                double t = i * (Math.PI / 24), x = c + r * Math.Cos(t), y = r * Math.Sin(t);
                if (x * x / (longAxis * longAxis) + y * y / (shortAxis * shortAxis) > 1.0001) return false;
            }
            return true;
        }
        double[] best = [shortAxis, shortAxis, 0];
        var bestArea = -1.0;
        for (var i = 0; i <= 8; i++)
        {
            var r = shortAxis * (0.6 + 0.05 * i);
            double lo = 0, hi = longAxis - r;
            if (!Inside(0, r)) continue;
            for (var k = 0; k < 14; k++) { var mid = (lo + hi) / 2; if (Inside(mid, r)) lo = mid; else hi = mid; }
            var area = 4 * lo * r + Math.PI * r * r;
            // 3% inside, for the slant the axis-aligned stadium cannot follow.
            if (area > bestArea) { bestArea = area; best = [(lo + r) * .97, r * .97, r * .97]; }
        }
        return a >= b ? best : [best[1], best[0], best[2]];
    }

    /// <summary>The rect a Calm bar's outline fuses with when a free button sits beside it, as
    /// <see cref="GlassPainter.DrawCalmBar"/>'s <c>fuse</c>: centre x, centre y (from the bar's centre), half
    /// width, half height, corner radius, smoothing; px in the bar's material space. <paramref name="button"/> is
    /// the button's laid-out rect relative to the bar's top-left, <paramref name="transform"/> its
    /// <see cref="FreeButton.Sample"/>, <paramref name="barPressX"/> and <paramref name="barPressY"/> the bar's own
    /// press scale, <paramref name="smoothing"/> the fusion width (times 1 - <see cref="Yield"/> where a lens arrives).</summary>
    public static float[] Compute(double barWidth, double barHeight, RectD button, FreeButtonTransform transform,
        double smoothing, double barPressX = 1, double barPressY = 1)
    {
        if (!new[] { barWidth, barHeight, barPressX, barPressY, button.X, button.Y, button.Width, button.Height, smoothing }.All(double.IsFinite))
            throw new ArgumentOutOfRangeException(nameof(button), "Finite geometry required");
        double hw = button.Width / 2, hh = button.Height / 2;
        // accessoryExtents: the pressed body stretched along the pull (half axis A) and thinned across it (B).
        double a = transform.Angle * Math.PI / 180, c = Math.Cos(a), s = Math.Sin(a);
        double r = (hw * transform.PressScale + hh * transform.PressScale) / 2, along = r * transform.Along, across = r * transform.Across;
        double Reach(double cx, double sx) => 1 / Math.Sqrt(cx * cx / (along * along) + sx * sx / (across * across));
        var stadium = InscribedStadium(Reach(c, s), Reach(s, c));
        double fx = button.X + hw + transform.OffsetX - barWidth / 2, fy = button.Y + hh + transform.OffsetY - barHeight / 2;
        // toMaterialPx for an anchored fuse: undo the bar's own press about its centre.
        double ex = Math.Max(barPressX, 1e-3), ey = Math.Max(barPressY, 1e-3), e = Math.Sqrt(ex * ey);
        return [(float)(fx / ex), (float)(fy / ey), (float)(stadium[0] / ex), (float)(stadium[1] / ey), (float)(stadium[2] / e), (float)(smoothing / e)];
    }

    /// <summary>Compose's GlassTabBarAccessory.lensFusion: a held lens at the bar's end joins the free button
    /// beside it through a neck of its own. Bar-local device px; <paramref name="lensFusion"/> is the neck's
    /// strength in device px (Compose's sample: 12 dp). Returns DrawSelector's fuse, or null while the lens is
    /// not formed enough to take the neck (formation below 0.5).</summary>
    public static float[]? LensFuse(double barWidth, double barHeight, RectD button, double formation, double lensFusion)
    {
        if (!new[] { barWidth, barHeight, button.X, button.Y, button.Width, button.Height, formation, lensFusion }.All(double.IsFinite))
            throw new ArgumentOutOfRangeException(nameof(button), "Finite bar, button, formation and lensFusion required");
        var f = Math.Clamp((formation - .5) / .5, 0, 1); var taken = f * f * (3 - 2 * f);
        if (lensFusion <= 0 || taken <= .01) return null;
        var r = Math.Min(button.Width, button.Height) / 2;
        return [(float)(button.X + button.Width / 2 - barWidth / 2), (float)(button.Y + button.Height / 2 - barHeight / 2),
            (float)(button.Width / 2), (float)(button.Height / 2), (float)r, (float)(lensFusion * taken)];
    }

    /// <summary>How much of the neck a formed lens reaching the bar's end has taken over, 0 to 1
    /// (glassAccessoryYield). <paramref name="endEdge"/> is the lens body's edge on the button's side, bar-local px.</summary>
    public static double Yield(double formation, double endEdge, double barWidth, double barHeight)
    {
        var span = Math.Max(.1 * barHeight, 1e-3);
        var reach = Math.Clamp((endEdge - (barWidth - span)) / (2 * span), 0, 1);
        return Math.Clamp(formation, 0, 1) * reach * reach * (3 - 2 * reach);
    }
}
