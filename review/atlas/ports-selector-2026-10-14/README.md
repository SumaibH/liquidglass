# Calm navigation selector in the CanvasKit and .NET ports (14 October 2026)

![The CanvasKit selector at rest, in transit after a tap, held, and held while moving](canvaskit-selector.png)

*CanvasKit 0.42 CPU raster in Node, `painter.drawSurface` for the bar and `painter.drawSelector`
for the body, over a synthetic checkerboard; 834 × 186 px bar at 3 px/dp. Not a performance
measurement.*

The ports had the material, scenes and whole-surface feedback but no navigation selector. Both now
have the Compose Calm selector's motion and gesture protocol:

- `ports/skia/selector.mjs` (`CalmSelectorController`, `calmSelectorBar`, `createCalmNavigation`)
  and `scene.addNavigation`; `ports/dotnet/LiquidGlass.Skia/CalmSelector.cs`
  (`CalmSelectorController`, `CalmNavigation`) and `GlassPainter.DrawSelector`.
- A line-by-line port of `GlassPoseDynamics.kt` / `GlassPoseGeometry.kt` with the `GlassPoseSpec.Calm`
  preset, and of `GlassTabBar`'s gesture protocol (touch-down aims, hold or horizontal slop starts a
  drag, a vertical scroll before the hold cancels, a flick carries one tab further). The velocity
  estimate is a 100 ms least-squares slope standing in for Compose's `VelocityTracker`.

**Verification.** `GlassSelectorPortFixtureTest` exports five scenarios from the Kotlin controller at
60 Hz (adjacent tap, far tap, held throw into the wall, held reversals, stationary hold;
`ports/skia/fixtures/selector-calm.json`). The JS port matches every frame's mode, centre, extents,
formation, tap appearance, held travel and pose matrix within 0.05 px and 1e-4; the .NET port within
0.035 px over 456 frames. Event times are compared exactly as Kotlin does (a 1e-9 tolerance there
shifted one input a frame and diverged by 14 px until removed). CanvasKit 31/31 tests, .NET 13/13
checks, including drawing checks that the resting selector darkens its bar and leaves the bar's far
end untouched.

**Limits.** The selector is drawn through the production shader's pose body with the in-app material
blended toward the held-lens profile by formation. That is an approximation of the Compose Calm
selector's tuned material (its measured grey levels, rim and band glow are not reproduced), and the
ports draw no selected ink: labels, ink inside the lens and accessibility stay with the host.
Accommodation to item bounds is supported when the host passes them; the scene layer does not.
