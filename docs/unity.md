---
description: Liquid Glass for Unity, Built-in render pipeline and URP. The iOS-measured production material as a Unity shader with a GlassSurface component for quads and UI.
---

# Unity

`com.shayxo.liquidglass` is the liquidglass material as a Unity shader, for the Built-in render pipeline
(through a GrabPass) and URP (through the opaque texture), with a `GlassSurface` component that sets the
material from the glass's size on screen. It works on quads and on UI Images on Screen Space - Camera or
World Space canvases. iOS is not a target.

!!! warning "Not yet run in Unity"
    Unity is not installed where this package is developed. The generated shader is compiled by FXC
    (`ps_5_0`, Unity's Direct3D 11 profile) and rendered on Direct3D 11's WARP device in CI, and the
    material's numbers are checked; the ShaderLab wrappers, GrabPass, URP and `GlassSurface.cs` have not
    been run. [What is verified](https://github.com/shayann07/liquidglass/tree/main/ports/unity#what-is-verified).
    Reports welcome.

![The generated shader on Direct3D 11 WARP: a light card and a dark bar over colour bands and a checker row](https://raw.githubusercontent.com/shayann07/liquidglass/main/review/atlas/unity-2026-10-20/unity-warp.png)

*Our render on D3D11 WARP, not a Unity capture.
[Evidence and limits](https://github.com/shayann07/liquidglass/blob/main/review/atlas/unity-2026-10-20/README.md).*

Package Manager → **Add package from git URL**:

```
https://github.com/shayann07/liquidglass.git?path=/ports/unity/com.shayxo.liquidglass
```

Then make a material with **LiquidGlass/Surface** (Built-in) or **LiquidGlass/Surface (URP)**, put it on
a quad or a UI Image, and add **LiquidGlass → Glass Surface**. The
[package README](https://github.com/shayann07/liquidglass/tree/main/ports/unity) has every option and
how it works.
