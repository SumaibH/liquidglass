---
description: Liquid Glass for .NET. iOS 26's refracting glass on SkiaSharp for .NET MAUI, WPF, Avalonia and Windows Forms, measured against iOS recordings.
---

# Liquid Glass for .NET: MAUI, WPF, Avalonia, WinForms

The `LiquidGlass` NuGet packages draw iOS 26's Liquid Glass on a SkiaSharp canvas, with the production
shaders the Compose library runs. Each framework has an adapter: the glass refracts your own views,
and the labels on it stay ordinary controls.

![.NET MAUI on a Pixel 7: a light glass card and a dark glass bar over native MAUI views](../assets/maui-pixel7.png)

*Our capture: the MAUI demo on a Pixel 7, mid-scroll.*

| Package | Framework | Runs on |
| :-- | :-- | :-- |
| `LiquidGlass.Maui` | .NET MAUI (`GlassSurface`) | Android (checked on a Pixel 7), Windows (built, not run) |
| `LiquidGlass.Wpf` | WPF | Windows |
| `LiquidGlass.Avalonia` | Avalonia 12 | Windows, macOS, Linux |
| `LiquidGlass.WinForms` | Windows Forms | Windows |
| `LiquidGlass` | any SkiaSharp canvas | anywhere SkiaSharp runs |

```bash
dotnet add package LiquidGlass.Maui
```

```csharp
builder.UseMauiApp<App>().UseLiquidGlass();
var bar = new GlassSurface { Backdrop = new GlassBackdrop(rows), Dark = true, Radius = 36, Content = tabs };
```

`LiquidGlass.Maui` is on NuGet from 1.0.0; the others since 0.2.1. iOS is not a target.

Next: [the .NET guide](../native-dotnet.md) · [Limitations](../limitations.md)
