---
description: Liquid Glass for React Native on Android and the web. GlassSurface and GlassLens draw iOS 26's refracting glass with React Native Skia, measured against iOS recordings.
---

# Liquid Glass for React Native

`@shayxo.dev/liquidglass-react-native` draws iOS 26's Liquid Glass in React Native on **Android** and
**React Native Web**, through React Native Skia, with the production shaders: the same SkSL the
Compose library runs, measured against iOS recordings. On iOS 26 itself, use the system's glass, for
example through `expo-glass-effect` or `@callstack/liquid-glass`. This package covers the platforms
those don't.

![Glass rendered by React Native Skia: a lens bending lines, a light and a dark surface](../assets/react-native-glass.png)

*Our render: React Native Skia's headless build. On Android it runs on a Pixel 7 and the emulator.*

```bash
npm install @shayxo.dev/liquidglass-react-native @shopify/react-native-skia
```

```jsx
import {GlassSurface, GlassLens} from '@shayxo.dev/liquidglass-react-native';

<Canvas style={StyleSheet.absoluteFill} pointerEvents="none">
  <GlassSurface source={backdrop} x={16} y={height - 100} width={width - 32} height={84} radius={28} dark />
  <GlassLens source={backdrop} x={40} y={120} width={140} magnification={1.5} />
</Canvas>
```

`backdrop` is a snapshot of the views behind the glass (`makeImageFromView`), taken again when they
change.

Next: [the React Native guide](../react-native.md) · [Limitations](../limitations.md)
