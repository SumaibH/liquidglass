# React Native — 19 October 2026

`liquidglass-react-native` (`ports/react-native`) draws the production glass with React Native Skia,
through a small adapter that lets the CanvasKit painter run on React Native Skia's API unchanged.

![A lens bending white lines over diagonal colour bands, a light glass surface top right and a dark one bottom left](react-native-glass.png)

*Our render: `npm run example` in `ports/react-native`, on React Native Skia 2.14.0's headless build
(its JSI API over CanvasKit 0.41, software rendering in Node). 960 × 640 at pixel ratio 2. The page
is an image drawn in Skia. `GlassLens` (magnification 1.5), and `GlassSurface` light with the overhead
light and dark with the default light, are the components an app puts in a `<Canvas>`.*

## Checks (`npm test`)

- Two surfaces and a lens drawn through the adapter equal the same glass drawn by the painter on
  CanvasKit directly: no channel differs by more than 1.
- In a React Native Skia element tree, `GlassSurface` and `GlassLens` change the page under them
  (mean difference over 10 and over 5 levels) and leave it alone elsewhere (under 1).
- Disposal is idempotent, and a shader that fails to compile is reported.
- `tsc` compiles the declared types.

## Limits

- Not yet run on a phone. React Native Skia's native Android implementation takes the same calls, but
  that is unconfirmed here.
- The backdrop is a snapshot (`makeImageFromView`, or an image drawn in the canvas). No live view
  capture and no touch response yet.
