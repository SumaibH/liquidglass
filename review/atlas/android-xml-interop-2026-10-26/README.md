# Android XML View Interop — Physical Device Verification

## Scope

Verifies `LiquidGlassView`, `LiquidGlassHostView`, `LiveViewBackdrop`, and `Modifier.glassHitTarget`
on a physical Android device running the AGSL shader path over an Android `ScrollView` and `LinearLayout`
view hierarchy (`XmlInteropActivity` in `:sample`).

## Environment & Method

- **Device**: Samsung Galaxy S24+ (`SM-S926U`), Android 16 (API 36, AGSL `RuntimeShader` backend)
- **Resolution & Density**: 1440 x 3120 px, 560 dpi
- **Target Commit**: `af48c89209c987955ca04b414c572bd3a0776db8`
- **Scene**: `com.wexpa.liquidglass.sample/.XmlInteropActivity` (`apps/sample/src/main/res/layout/activity_xml_interop.xml`)
- **Capture Sequence**:
  1. `./gradlew :sample:assembleDebug :sample:assembleDebugAndroidTest :liquidglass:checkKotlinAbi`
  2. `adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp install -r apps/sample/build/outputs/apk/debug/sample-debug.apk`
  3. `adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp install -r apps/sample/build/outputs/apk/androidTest/debug/sample-debug-androidTest.apk`
  4. `adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp shell am start -W -n com.wexpa.liquidglass.sample/.XmlInteropActivity`
  5. `adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp shell input swipe 540 1200 540 920 350` (scrolls the underlying Android `ScrollView` through the `LiquidGlassHostView` touch pass-through region)
  6. `adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp shell screencap -p /sdcard/xml-interop-s24plus.png && adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp pull /sdcard/xml-interop-s24plus.png review/atlas/android-xml-interop-2026-10-26/xml-interop-s24plus.png`

## Hardware Instrumentation Test Results (Samsung Galaxy S24+)

```bash
adb -s adb-RZCX727QWCX-JoEwpS._adb-tls-connect._tcp shell am instrument -w -r \
  -e class com.wexpa.liquidglass.sample.GlassBackdropHardwareTest \
  com.wexpa.liquidglass.sample.test/androidx.test.runner.AndroidJUnitRunner
```

```
Time: 17.099

OK (24 tests)
```
Pass Rate: 100% (24 of 24 tests passed under strict hardware `PixelCopy.SUCCESS` assertions on physical device surface).

## What This Proves

![XmlInteropActivity on Samsung Galaxy S24+ after scrolling the underlying ScrollView](xml-interop-s24plus.png)

*Our render on a Samsung Galaxy S24+ (SM-S926U, Android 16, API 36, AGSL RuntimeShader backend at commit af48c89209c987955ca04b414c572bd3a0776db8): a declarative XML `LiquidGlassView` rounded card at the top and a `LiquidGlassHostView` hosting `GlassTabBar` at the bottom, captured via hardware screencap after swipe scrolling the underlying `ScrollView`. Limits: demonstrates single still scene capture over Android View hierarchy; does not measure continuous 60/120 Hz frame percentiles across third-party SurfaceView or TextureView video planes.*

1. **`LiquidGlassView` (`wrap_content` XML container)**: Wraps ordinary Android XML child views (`LinearLayout` and `TextView`s) inside a rounded `Glass.Regular.interactive()` surface configured via XML attributes (`app:liquidGlassBackdrop`, `app:liquidGlassPreset="regular"`, `app:liquidGlassShape="rounded"`, `app:liquidGlassCornerRadius="26dp"`, `app:liquidGlassInteractive="true"`), sizing to its XML children without expanding `wrap_content` to the parent frame.
2. **`LiquidGlassHostView` + `Modifier.glassHitTarget`**: Hosts a Compose `GlassTabBar` over an Android XML layout while passing touches outside the registered `glassHitTarget` bounds through to the underlying `ScrollView`.
3. **Scroll-aware `LiveViewBackdrop`**: Captures scrolled `View` viewports (`scrollX` / `scrollY`) and ancestor `ViewGroup` hierarchies while skipping `LiquidGlassHostView` / `LiquidGlassView` subtrees to prevent self-sampling and recursive `RenderNode.beginRecording()` calls.
4. **Hardware Lifecycle & Touch Parity**: Confirmed unowned application touch listeners remain preserved on disposal, custom triangle generic outlines and asymmetric corner geometry render with correct clipping/refraction on physical hardware, and green optical barrier tests verify true glass refraction.

## What This Does Not Prove

- It does not measure continuous 60/120 Hz frame-time percentiles across arbitrary third-party `SurfaceView` or hardware `TextureView` backdrops (hardware video/camera planes that bypass standard `View.draw(Canvas)` are outside the `Canvas` capture path).
