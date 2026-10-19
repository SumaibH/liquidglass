# Flutter — 19 October 2026

`liquidglass` for Flutter (`ports/flutter`) draws the Compose library's material over the live
backdrop: `GlassSurface` is a backdrop filter running the production shader, generated from
`ports/skia/shaders/material.sksl`.

![The example at rest (left) and scrolled (right): a light glass card at the top and a dark glass bar at the bottom refracting the list](flutter-emulator.png)

*Our capture: the example app (`ports/flutter/example`, release build) on the Android 17 emulator
(`lg_verify_37`, x86_64, 1080 × 2400 at 420 dpi). Impeller on its OpenGL ES backend. Flutter
3.47.7. `adb exec-out screencap`; left at rest, right after a 400 px swipe, scaled to 45%. The page is
an ordinary `ListView`; the glass refracts it live.*

| Crop | |
| :-- | :-- |
| ![The light card refracting the list's first rows, with the fold copying the row's text at the rim](flutter-card-crop.png) | the light card (light `[0, -1]`) |
| ![The dark bar refracting row 8, with its icons above it](flutter-bar-crop.png) | the dark bar, icons as ordinary Flutter widgets |

*Full-resolution crops from the scrolled capture.*

## Pixel 7 (Vulkan) — 20 October

![The example on a Pixel 7 at rest (left) and scrolled (right)](flutter-pixel7.png)

*Our capture: the same example (`ports/flutter/example`, profile build, source revision `0f52457`) on a
Pixel 7 running Android 17. Logcat at launch: "Using the Impeller rendering backend (Vulkan)". Flutter
3.47.7. `adb exec-out screencap`; left at rest, right after a swipe, scaled to 45%.*

![The dark bar on the Pixel 7 refracting row 6, its icons above the glass](flutter-pixel7-bar-crop.png)

*Full-resolution crop from the capture at rest: the bar refracts row 6 the right way up, so the
generated shader needs no flip on Vulkan either.*

[flutter-pixel7.mp4](flutter-pixel7.mp4): 6 s of `adb shell screenrecord` (scaled to 540 px wide)
while `adb shell input swipe` scrolls the list three times and back. The glass follows the list every
frame. This is a visual check only; frame timing was not measured.

## Checks

- `flutter test`:
  - the generated shader compiles with Flutter's shader compiler and loads;
  - the Dart material's 75 uniforms equal the CanvasKit port's for three surfaces, to 1e-6
    (`test/material_fixture.json`, generated from `ports/skia/material.mjs`);
  - the widget lays out and paints without Impeller.
- `node tool/generate_shader.mjs --check` and `node tool/generate_fixture.mjs --check` fail when
  either generated file drifts from its source.

## Found on the way

Flutter documents a y flip on OpenGL ES for `ImageFilter.shader`. Applied to a backdrop filter's
input, it turned the page upside down on the emulator: the bar showed the page's title. Without the
flip, the backdrop is the right way up. The generated shader has no flip.

## Limits

- One phone: the Pixel 7 (Vulkan) and the emulator (OpenGL ES). Frame timing is not measured.
- The wide tone kernel is approximated with thirteen Gaussian taps (see the package README).
- Rounded rectangles only. There is no lens or navigation bar, and no touch response.
