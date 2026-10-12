package com.wexpa.liquidglass.sample.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.wexpa.liquidglass.sample.LoupeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing
import java.awt.Toolkit
import java.awt.Component
import java.awt.Container
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

internal fun atlasJsonText(value: String): String = value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

private fun atlasLayer(component: Component): SkiaLayer? = when (component) {
    is SkiaLayer -> component
    is Container -> component.components.firstNotNullOfOrNull { atlasLayer(it) }
    else -> null
}

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    println(">>> Starting Atlas Studio at native resolution: ${screenSize.width}x${screenSize.height}")

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Atlas Studio - LiquidGlass",
        state = windowState,
    ) {
        val motionProbe = remember { AtlasMotionProbe() }
        LaunchedEffect(Unit) {
            // Native redraw can flush Compose continuations. Keep diagnostics outside that
            // dispatcher so neither input nor capture can re-enter its own coroutine.
            withContext(Dispatchers.Swing) {
                try {
                    delay(3000)
                    val nativeLayer = checkNotNull(atlasLayer(window)) { "No native Skia layer" }
                    println("ATLAS_RENDERER requestedGpu=${System.getProperty("skiko.gpu.priority", "auto")} ${nativeLayer.renderInfo}")
                    System.getProperty("atlas.motionCapture")?.let { path ->
                        require(System.getProperty("atlas.capture") == null) { "Choose static or motion capture" }
                        require((System.getProperty("atlas.scene")?.toIntOrNull() ?: 0) == 0) {
                            "Motion capture starts with atlas.scene=0"
                        }
                        val readback = System.getProperty("atlas.motionReadback")?.toBooleanStrict() ?: true
                        val shortTaps = System.getProperty("atlas.motionShortTaps")?.toBooleanStrict() ?: false
                        captureAtlasMotion(window, motionProbe, File(path), readback, shortTaps)
                        exitApplication()
                        return@withContext
                    }
                    System.getProperty("atlas.capture")?.let { path ->
                        // Read this application's render buffer. Desktop screen grabs can capture
                        // another app when the owner switches windows and are never safe evidence.
                        val skia = nativeLayer
                        repeat(10) { skia.renderImmediately() }
                        val samples = ArrayList<Double>()
                        repeat(120) {
                            delay(16)
                            val start = System.nanoTime()
                            skia.renderImmediately()
                            samples.add((System.nanoTime() - start) / 1_000_000.0)
                        }
                        samples.sort()
                        skia.renderImmediately()
                        val file = File(path).absoluteFile
                        file.parentFile.mkdirs()
                        checkNotNull(skia.screenshot()).use { bitmap ->
                            Image.makeFromBitmap(bitmap).use { image ->
                                checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
                                    file.writeBytes(data.bytes)
                                }
                            }
                            println("ATLAS_CAPTURE ${file.path} ${bitmap.width}x${bitmap.height} ${skia.renderApi}")
                            File(file.parentFile, file.nameWithoutExtension + "-timing.json").writeText(
                                """{"metric":"native static-scene forced redraw; GPU submission, not presented FPS", "backend":"${skia.renderApi}", "requestedGpu":"${System.getProperty("skiko.gpu.priority", "auto")}", "rendererInfo":"${atlasJsonText(skia.renderInfo)}", "width":${bitmap.width}, "height":${bitmap.height}, "samples":120, "warmup":10, "p50_ms":${samples[59]}, "p95_ms":${samples[113]}, "max_ms":${samples.last()}}"""
                            )
                        }
                        exitApplication()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    if (System.getProperty("atlas.capture") != null || System.getProperty("atlas.motionCapture") != null)
                        kotlin.system.exitProcess(1)
                }
            }
        }
        LoupeScreen(initialScene = System.getProperty("atlas.scene")?.toIntOrNull() ?: 0,
            onNavigationLayout = { motionProbe.navigation = it },
            onSceneObserved = { motionProbe.scene = it })
    }
}
