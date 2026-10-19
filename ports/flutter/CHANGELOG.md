# Changelog

The Flutter package follows the liquidglass repository's version: every release of the library
publishes `liquiglass` at the same number. The full history is in the repository's
[CHANGELOG](https://github.com/shayann07/liquidglass/blob/main/CHANGELOG.md).

## 0.3.0-dev

- First pub.dev release. `GlassSurface` draws the liquidglass material over the live backdrop
  through `ImageFilter.shader` on Impeller, its shader generated from the Compose library's
  material. Checked on a Pixel 7 (Vulkan) and the Android 17 emulator (OpenGL ES).
- Published as `liquiglass`: pub.dev reserves `liquidglass` because `liquid_glass` exists. Import
  `package:liquiglass/liquiglass.dart`.
