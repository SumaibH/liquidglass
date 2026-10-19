---
description: Liquid Glass for Android, Jetpack Compose and Compose Multiplatform. iOS 26's refracting glass, its tab bar lens and glass Material 3 components, measured against iOS recordings.
---

# Liquid Glass for Android and Jetpack Compose

iOS 26's Liquid Glass in Jetpack Compose: real refraction of whatever is behind a surface, the tab bar
whose selection becomes a lens you can drag and throw, and a glass version of every Material 3
component. The same code runs on desktop and on the web through Compose Multiplatform. The material
and the motion are fitted to iOS screen recordings and scored against them.

![A Material 3 app in glass, dark scheme: top bar, buttons, chips, a frosted mini player and the floating tab bar](../assets/material3-dark.png)

*Our render: the Material 3 showcase, desktop Skia, the same shader as Android's AGSL path.*

```kotlin
// build.gradle.kts
plugins { id("dev.shayxo.liquidglass") version "1.0.0" }
```

```kotlin
LiquidGlassTheme { App() }                       // once, as MaterialTheme is

Icon(Icons.Default.Edit, null,
    Modifier.size(44.dp).glassEffect(Glass.Regular.interactive(), CircleShape))
```

Or keep your Material 3 code and change the imports, `androidx.compose.material3.*` to
`com.wexpa.liquidglass.material3.*`; `./gradlew liquidGlassAdopt` does it for you.

- **Android 13 and newer:** the full shader (AGSL), checked on a Pixel 7.
- **Below Android 13:** a tinted fallback with rim lighting, no refraction.
- **Desktop and web:** the same shader on Skia. [Try it in your browser](../demo.md).

Next: [Getting started](../getting-started.md) · [Material 3, in glass](../material3.md) ·
[The tab bar](../tab-bar.md) · [Limitations](../limitations.md)
