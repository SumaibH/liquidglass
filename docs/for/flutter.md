---
description: Liquid Glass for Flutter. GlassSurface refracts whatever Flutter paints behind it, live, every frame, on Impeller, with a shader measured against iOS 26 recordings.
---

# Liquid Glass for Flutter

`liquiglass` puts iOS 26's Liquid Glass in a Flutter app. `GlassSurface` refracts whatever Flutter
paints behind it, live, every frame: lists scrolling under it, images, video. Its shader is generated
from the Compose library's, whose material is measured against iOS recordings. It runs on Impeller,
Flutter's renderer on Android.

![The Flutter example: a light glass card and a dark glass bar refracting a scrolling list](../assets/flutter-emulator.png)

*Our capture: the example on the Android 17 emulator, Impeller, at rest and scrolled. It also runs on a
Pixel 7 on Impeller's Vulkan backend.*

```bash
flutter pub add liquiglass
```

```dart
import 'package:liquiglass/liquiglass.dart';

Stack(children: [
  ListView(children: rows),
  Positioned(left: 24, right: 24, bottom: 24, height: 72,
    child: GlassSurface(radius: 36, dark: true, child: Row(children: icons))),
])
```

pub.dev reserves `liquidglass` because `liquid_glass` exists, so the package is `liquiglass`. Install it
with `flutter pub add liquiglass`. iOS is not a target: on iOS 26 the system's
own glass is the native choice.

Next: [the Flutter guide](../flutter.md) · [Limitations](../limitations.md)
