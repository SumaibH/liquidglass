# LiquidGlass

Apple-style Liquid Glass for Compose Multiplatform: real refraction of the content behind a
surface, a navigation selector that turns into a moving lens, and calm touch response, from one
shader that runs on Android 13+ and desktop JVM, with ports for CanvasKit and SkiaSharp.

[![CI](https://github.com/shayann07/liquidglass/actions/workflows/ci.yml/badge.svg)](https://github.com/shayann07/liquidglass/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/dev.shayxo.liquidglass/liquidglass)](https://central.sonatype.com/artifact/dev.shayxo.liquidglass/liquidglass)

[Documentation](https://liquidglass.shayxo.dev/) · [Getting started](docs/getting-started.md) ·
[API](docs/api-reference.md) · [How it is verified](#how-it-is-verified)

<p align="center">
  <img src="docs/assets/navigation.gif" width="780" alt="Calm navigation bar: a tapped selector travels as glass, a held selector lifts into a lens, a thrown lens lands against the end of the bar">
</p>
<p align="center"><sub>Rendered by the library (Calm navigation, desktop Skia, deterministic 60 fps replay of
an authored tap, hold and throw; shown at 30 fps). Placeholder content.</sub></p>

> **Status.** `0.1.0` is on Maven Central. The scene API, continuous magnifier and Calm
> navigation shown here are on this branch and **unreleased**; their APIs may still change.
> Motion and material are compared against real iOS 26 recordings and are close but **not yet
> identical** — the [current differences](review/atlas/reference-replay-2026-10-13/README.md#remaining-differences)
> are listed openly.

## What you get

- **Refraction, not blur-and-border.** Each surface bends, disperses and lights the actual pixels
  behind it, through a signed-distance bevel with Snell refraction and a measured rim.
- **A selector that behaves like the iOS one.** At rest it is a tinted inset. Tapped, it travels
  as clear glass and lands back into the inset; held, it lifts into a lens that magnifies the
  tab under the finger; thrown, it stays glass until it settles against the end of the bar.
  A separate search button beside the bar joins it through a liquid neck while the bar is held.
- **Calm interaction.** Press and drag responses are restrained, large cards respond less than
  small controls, and layout and hit targets never move — only the material does.
- **One backdrop, many surfaces.** Record the page once; every bar, card and lens on top samples it.

## Quick start

```kotlin
GlassScene(
    background = Color(0xFF09111E),
    modifier = Modifier.fillMaxSize(),
    backdrop = { YourPage() },          // what the glass refracts; keep it opaque
) {
    // A floating toolbar: ordinary Compose content on a glass surface.
    Row(
        Modifier.align(Alignment.BottomCenter).padding(24.dp)
            .glass(style = GlassStyle.DarkChrome)
            .padding(16.dp),
    ) { YourToolbarItems() }

    // A magnifier: one continuous lens over the same recorded page.
    Box(
        Modifier.align(Alignment.Center).size(180.dp)
            .glass(CircleShape, GlassStyle.clearLens(magnification = 1.25f)),
    )
}
```

Navigation with the iOS-style selector:

```kotlin
val glass = rememberLiquidGlassState(background = Color.Black)
Box {
    Page(Modifier.fillMaxSize().liquidGlassSource(glass))
    GlassTabBar(
        state = glass, itemCount = 3, selectedIndex = selected, onSelected = { selected = it },
        style = GlassTabBarStyle.Calm(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).width(280.dp),
    ) { index, isSelected -> Tab(index, tint = if (isSelected) Accent else Ink) }
}
```

Draw each tab the same way whether or not it is selected and change only its colour: the row is
drawn twice, once on the bar and once inside the lens, and the two must line up.
More in the [tab bar guide](docs/tab-bar.md) and [getting started](docs/getting-started.md).

## Install

```kotlin
implementation("dev.shayxo.liquidglass:liquidglass:0.1.0")   // published, original API
```

The APIs above (`GlassScene`, `GlassStyle.clearLens`, `GlassTabBarStyle.Calm`) are not in `0.1.0`
yet. To use them, build this branch with `./gradlew publishToMavenLocal` and depend on the
`VERSION_NAME` in [gradle.properties](gradle.properties) with `mavenLocal()` scoped to
`dev.shayxo.liquidglass` — [step by step](docs/getting-started.md).

## Platforms

| Platform | Status |
| :-- | :-- |
| Android 13+ | Full shader (AGSL) |
| Android below 13 | Tinted fallback with rim lighting; no refraction |
| Desktop JVM | Full shader (Skia). Tested on Windows and Linux; macOS untested |
| Web | [CanvasKit port](ports/skia/README.md) with the production shaders, scene API and input controller; [small WebGL lens preview](ports/web/README.md) |
| .NET | [SkiaSharp painter and WinForms adapter](ports/dotnet/README.md); no navigation selector yet |
| iOS (Compose or SwiftUI) | Not supported yet |
| Anything else with Skia | Shaders are portable; follow the [porting contract](docs/porting.md) |

## Atlas Studio

A desktop demo of the material: a movable magnifier over sky, type and line backdrops, a calm
note card and the navigation bar.

```sh
./gradlew :desktop:run
```

## How it is verified

The optical constants are measured from iOS captures and kept separate from authored motion
parameters; [the research record](docs/research/README.md) says which is which. Motion is checked
by replaying the gestures of two iOS screen recordings (taps, holds, throws into both walls,
held reversals, soft landings) through the real renderer at a fixed 60 fps and comparing the
sequences side by side — [latest comparison](review/atlas/reference-replay-2026-10-13/README.md).
`./gradlew :liquidglass:jvmTest` compiles the production shaders and checks rendered pixels and
gesture behaviour.

Known limits: [docs/limitations.md](docs/limitations.md). Work in progress:
[docs/development-status.md](docs/development-status.md).

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE). Not affiliated with or endorsed by
Apple. No Apple code, shaders or assets are used; reference recordings are research material
and are not distributed. [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) ·
[Changelog](CHANGELOG.md)
