# apps

The apps built from the library. Their Gradle paths are unchanged by the move here on 20 October 2026
(`settings.gradle.kts` points `:sample`, `:desktop` and `:web` at these folders), so every
`./gradlew :desktop:run`-style command still works. Records written before that date call them
`sample/`, `desktop/` and `web/`.

| Folder | Gradle | What it is |
| :-- | :-- | :-- |
| [`sample`](sample) | `:sample` | the Android sample: the Material 3 showcase, Atlas Studio's Loupe screen, and the deterministic device fixtures |
| [`desktop`](desktop) | `:desktop` | Atlas Studio on the desktop JVM, and the replay and capture harnesses (`:desktop:phoneReplay`) |
| [`web`](web) | `:web` | the same app on Compose for web (Wasm): the [live demo](https://liquidglass.shayxo.dev/demo/) |

`apps/sample/src/main/kotlin/shared` holds the scenes all three share.
