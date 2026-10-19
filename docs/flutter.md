---
description: liquiglass, Liquid Glass for Flutter: GlassSurface refracts the live backdrop on Impeller with the shader measured against iOS recordings.
---

# Flutter

[`liquiglass`](https://pub.dev/packages/liquiglass), liquidglass for Flutter, puts the Compose library's material over the **live** backdrop.
`GlassSurface` refracts whatever Flutter paints behind it, every frame: lists scrolling under it,
images, video. No snapshot is involved. It runs on Impeller, Flutter's renderer on Android. iOS is
not a target; on iOS 26 the system's own glass is the native choice.

![Flutter on the Android emulator: a light glass card and a dark glass bar refracting a scrolling list, at rest and scrolled](assets/flutter-emulator.png)

*Our capture: the example on the Android 17 emulator, Impeller (OpenGL ES), at rest and scrolled.*

```bash
flutter pub add liquiglass
```

!!! note "The name"
    pub.dev refuses `liquidglass` as too close to the existing `liquid_glass`, so the Flutter package
    is `liquiglass`. It follows the library's version (`0.3.0-SNAPSHOT` on `main` is `0.3.0-dev`).
    Until its first upload is on pub.dev, depend on it by path:
    `liquiglass: {path: ../path/to/liquidglass/ports/flutter}`.

```dart
import 'package:liquiglass/liquiglass.dart';

Stack(children: [
  ListView(children: rows),
  Positioned(
    left: 24, right: 24, bottom: 24, height: 72,
    child: GlassSurface(radius: 36, dark: true, child: Row(children: icons)),
  ),
])
```

`GlassSurface` takes the material's options:

- `radius`;
- `dark`, `tintAmount` and `tint`;
- `light` (`Offset(0, -1)` is iOS 27's overhead light);
- `materialize`;
- `reducedTransparency` and `increasedContrast`.

Call `LiquidGlass.ensureInitialized()` in `main` so the shader is ready for the first frame. Where
Impeller is unavailable, glass draws a frosted stand-in. The
[package README](https://github.com/shayann07/liquidglass/blob/main/ports/flutter/README.md) has
every option.

## How it works

The shader is generated, not rewritten. `tool/generate_shader.mjs` translates the material shader's
SkSL export (`ports/skia/shaders/material.sksl`) into the GLSL Flutter compiles. The translation
changes the types, the two texture reads and the entry point; `--check` fails when the copy drifts.
Each `GlassSurface` paints a backdrop filter whose filter is `ImageFilter.shader` running that
shader over the live backdrop.

One part is approximated. Compose and the CanvasKit port read the wide tone kernel from a
quarter-scale, Gaussian-blurred copy of the backdrop. A backdrop filter has a single input, so
Flutter uses thirteen taps of a Gaussian with the same sigma instead. It sets the material's tone,
never its detail.

## How it is checked

`flutter test` checks three things:

- the generated shader compiles with Flutter's shader compiler and loads;
- the Dart material's uniforms equal the CanvasKit port's for three surfaces, to 1e-6;
- the widget lays out and paints where shader filters are unavailable.

CI runs this on Linux. The example app runs on the Android 17 emulator on Impeller's OpenGL ES
backend, and on a Pixel 7 on Impeller's Vulkan backend, which most phones use. See the
[evidence](https://github.com/shayann07/liquidglass/blob/main/review/atlas/flutter-2026-10-19/README.md).

## Limits

- Rounded rectangles only. There is no lens or navigation bar yet, and no touch response.
- The wide tone kernel is approximated (above).
- Impeller only, checked on one phone (Pixel 7, Vulkan) and the emulator (OpenGL ES). Frame timing
  is not measured yet.
