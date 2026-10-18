# LiquidGlass for .NET

iOS 26-style refracting glass drawn on a SkiaSharp `SKCanvas` by the same production shaders as
[LiquidGlass for Compose](https://github.com/shayann07/liquidglass): the measured material, the lens,
and the tab bar selector with its motion. No Compose, browser or JavaScript.

- `LiquidGlass`: `GlassPainter` and the tab bar selector, for any SkiaSharp host (.NET 8).
- `LiquidGlass.WinForms`: glass over native Windows Forms controls (.NET 10, Windows 10 19041+).
- `LiquidGlass.Wpf`: glass over native WPF content, drawn into a `WriteableBitmap` (.NET 10, Windows 10 19041+).

```csharp
using LiquidGlass.Skia;
using SkiaSharp;

// One painter per rendering thread or context.
using var glass = new GlassPainter();
glass.SetSource(backdropImage);   // the page behind the glass: an opaque sRGB SKImage you own

// Draw the page first, then the glass over it. Bounds are in backdrop pixels.
glass.DrawLens(canvas, SKRect.Create(360, 50, 180, 180), magnification: 1.4f);
glass.DrawSurface(canvas, SKRect.Create(30, 260, 300, 100), radius: 24,
    options: new(Dark: true, TintAmount: 50, Density: 1));
// Then draw ordinary labels: they keep their host layout and accessibility.
```

On Linux, add `SkiaSharp.NativeAssets.Linux` and install fontconfig and a font.

The shaders are embedded in the assembly; nothing is fetched at runtime. Optical constants are
measured from iOS 26 recordings; motion parameters are authored. The research record says which is
which: <https://liquidglass.shayxo.dev/research/>.

Guide, Windows Forms example and limits:
<https://github.com/shayann07/liquidglass/blob/main/ports/dotnet/README.md>.

Apache-2.0. Not affiliated with or endorsed by Apple.
