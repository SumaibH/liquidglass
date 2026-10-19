---
description: LiquidGlass documentation: Liquid Glass for every stack, measured against iOS. Guides for Compose, Material 3, the web, React Native, Flutter and .NET.
---

# LiquidGlass

**Liquid Glass for every stack, measured against iOS.** Apple's iOS 26 glass for Android, desktop,
web, React Native, Flutter and .NET: real refraction of whatever is behind it, drawn in one shader whose
levels and motion are fitted to iOS screen recordings. [Try the live demo](demo.md).

| Your stack | Start here |
| :-- | :-- |
| Android, Jetpack Compose, Compose Multiplatform | [Liquid Glass for Android & Compose](for/android.md) |
| Flutter | [Liquid Glass for Flutter](for/flutter.md) |
| React Native | [Liquid Glass for React Native](for/react-native.md) |
| The web: React, Vue, Svelte, HTML | [Liquid Glass for the web](for/web.md) |
| .NET: MAUI, WPF, Avalonia, WinForms | [Liquid Glass for .NET](for/dotnet.md) |

The [development status and visual guide](development-status.md) covers every path, with its evidence
and remaining gaps.

If you are here to use it, start with [Getting started](getting-started.md). If you are here to
change the optics, read [How it works](how-it-works.md) and then
[the research](research/README.md). If you are here to decide whether it fits your app, read
[Limitations](limitations.md) first — it is deliberately the most detailed document here.

The current development snapshot replaces the public API with `LiquidGlassTheme` at the root and
`Modifier.glassEffect` on any element (named after SwiftUI's), and adds a continuous free magnifier
and a calm navigation preset. Explore them in [Atlas Studio](atlas-studio.md), or read the
[porting contract](porting.md) for other UI stacks. These are authored additions, not a new claim of
full Apple parity.

## Guides

| | |
| :--- | :--- |
| [Getting started](getting-started.md) | Wrap the app once, make any element glass, and pick a variant. |
| [Shapes](shapes.md) | Rounded rects, capsules, squircles, and arbitrary paths. |
| [Generic interaction](generic-interaction.md) | Shared anchored press/pull, custom controls and gesture ownership. |
| [Interaction](interaction.md) | Press, materialize, morphing between styles, merging. |
| [Tab bar](tab-bar.md) | `GlassTabBar`: the measured selection lens, and what each number in it comes from. |
| [Roadmap](roadmap.md) | What is planned, what the competitive audit changed, and what is deliberately not being done. |
| [Adaptation](adaptation.md) | Light/dark inversion, legibility, shadows, the scroll edge effect. |
| [Accessibility](accessibility.md) | Reduce Transparency, Increase Contrast, Reduce Motion. |
| [How it works](how-it-works.md) | The optical model, layer by layer, and why each part is there. |
| [Performance](performance.md) | What it costs, measured, and what to do if it costs too much. |
| [Limitations](limitations.md) | What it cannot do. Read before promising anyone parity with iOS. |
| [API reference](api-reference.md) | Every public symbol, with signatures. |

## Reference

- [Research](research/README.md) — the historical derivation and its provenance: the optical model, every parameter with its provenance, the motion model, and an
  honest account of where Android cannot match Apple.
- [Reference measurements](research/reference-measurements.md) — the one first-hand source:
  numbers read off a recording of a real iOS 26 tab bar, and the three places they overturned a
  derivation.
- [Publishing](publishing.md) — maintainer notes: coordinates, Maven Central, signing, and the
  release workflow.

## The one-paragraph version

Since 0.2 the default material is **measured, not tuned**: an inward fold lens, a two-kernel
tone and a per-appearance tint fitted from calibration captures of iOS 27 (see
[the measured model](research/measured-model.md)). The 0.1 framing below survives as the
`Legacy` profile.

Liquid Glass was first modelled here as **displacement-first, blur-second**. Apple's own framing is that inversion:
earlier materials scattered light, this one bends and concentrates it. A thin bevel around the
rim bends a *sharp* backdrop inward, compressing the surroundings into a legible band at the
edge, while the flat interior scatters and tints. Almost everything that makes an
implementation look wrong comes from getting that ordering backwards — blur the backdrop before
the shader sees it and you have destroyed the detail the rim exists to bend, and no amount of
tuning gets it back.
