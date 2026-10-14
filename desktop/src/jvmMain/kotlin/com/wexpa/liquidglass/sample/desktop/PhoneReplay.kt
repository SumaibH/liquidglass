package com.wexpa.liquidglass.sample.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import com.wexpa.liquidglass.GlassTabBarStyle
import com.wexpa.liquidglass.sample.LoupeScreen
import com.wexpa.liquidglass.sample.PhoneGeometry
import com.wexpa.liquidglass.sample.PhonePalette
import com.wexpa.liquidglass.sample.PhoneScene
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Deterministic reference replay on the GPU.
 *
 * Renders [PhoneScene] at exactly 60 frames per scripted second through Atlas's own Skiko layer
 * (Direct3D on Windows) and reads each frame back from that layer. Time is a virtual clock: frame
 * N is composed at N/60 s however long it takes to draw, and input arrives at its scripted
 * timestamp. The output is therefore reproducible and says nothing about interactive frame rate,
 * input latency or display presentation.
 *
 * Usage: `PhoneReplay <script> <outDir> [calm|v3|calm-natural]`. Script lines: `<ms> down|move|up <x> <y>`
 * in scene pixels at 3px/pt, optional `select <index>`, and `end <ms>`.
 */
fun main(args: Array<String>) {
    try { replay(args) } catch (e: Throwable) {
        File(args.getOrElse(1) { "." }, "replay-error.txt").apply { parentFile.mkdirs() }.writeText(e.stackTraceToString())
        e.printStackTrace()
        kotlin.system.exitProcess(1)
    }
}

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun replay(args: Array<String>) {
    val script = File(args[0])
    val out = File(args[1]).apply { mkdirs() }
    val variant = args.getOrElse(2) { "calm" }
    val events = script.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { it.split(Regex("\\s+")) }
    val end = events.first { it[0] == "end" }[1].toDouble()
    val initial = events.firstOrNull { it[0] == "select" }?.get(1)?.toInt() ?: 0
    val pointer = events.filter { it[0] !in setOf("end", "select") }
        .map { Triple(it[0].toDouble(), it[1], Offset(it[2].toFloat(), it[3].toFloat())) }
    // "atlas" renders Atlas Studio at 1200 x 750 dp (density 1.5) instead of the phone scene.
    val atlas = variant.startsWith("atlas")
    val sceneWidth = if (atlas) 1800 else 1170
    val sceneHeight = if (atlas) 1125 else 702
    val style = when (variant) {
        "v3" -> GlassTabBarStyle.V3().copy(height = PhoneGeometry.barHeight)
        // The 2026-10-13 scene geometry (slot-wide pill, 6 dp ends), for before/after renders.
        "calm-slot" -> GlassTabBarStyle.Calm().copy(height = PhoneGeometry.barHeight)
        else -> PhoneGeometry.style(GlassTabBarStyle.Calm())
    }
    // "natural" renders ordinary display colours for presentation; comparisons use the
    // recordings' own brightness space.
    val palette = if (variant.endsWith("natural")) PhonePalette.Natural else PhonePalette.Recorded

    lateinit var scene: ImageComposeScene
    lateinit var composeScene: ComposeScene
    lateinit var recomposer: FrameRecomposer
    var nanos = 0L
    var captured: Bitmap? = null
    var armed = false
    val layer = SkiaLayer()
    val frame = JFrame("LiquidGlass reference replay")
    SwingUtilities.invokeAndWait {
        scene = ImageComposeScene(sceneWidth, sceneHeight, Density(if (atlas) 1.5f else 3f), Dispatchers.Unconfined) {
            if (atlas) LoupeScreen(initialScene = initial)
            else {
                var selected by remember { mutableIntStateOf(initial) }
                PhoneScene(selected, { selected = it }, palette = palette, style = style)
            }
        }
        // ImageComposeScene draws into a CPU raster. Reuse its scene and frame clock, but draw
        // into this layer's GPU canvas instead.
        composeScene = ImageComposeScene::class.java.getDeclaredField("scene").apply { isAccessible = true }
            .get(scene) as ComposeScene
        recomposer = ImageComposeScene::class.java.getDeclaredField("frameRecomposer").apply { isAccessible = true }
            .get(scene) as FrameRecomposer
        layer.renderDelegate = object : SkikoRenderDelegate {
            var target: Surface? = null
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                // Draw into an offscreen render target on the layer's own GPU context and read it
                // back directly. SkiaLayer.screenshot() re-rasterizes on the CPU (seconds per frame)
                // and reading the swap-chain canvas stalls on Direct3D.
                if (!armed) return
                val surface = target ?: Surface.makeRenderTarget(checkNotNull(gpuContext(layer)) { "No GPU context" }, false,
                    ImageInfo.makeN32Premul(sceneWidth, sceneHeight)).also { target = it }
                surface.canvas.clear(Color.BLACK)
                recomposer.performFrame(nanos)
                composeScene.measureAndLayout()
                composeScene.draw(surface.canvas.asComposeCanvas())
                captured = Bitmap().apply {
                    allocPixels(ImageInfo.makeN32Premul(sceneWidth, sceneHeight))
                    check(surface.readPixels(this, 0, 0)) { "GPU readback failed" }
                }
                surface.makeImageSnapshot().use { canvas.drawImage(it, 0f, 0f) }
            }
        }
        frame.contentPane.add(layer)
        frame.isUndecorated = true
        // Never take focus or cover the owner's work: the frame only hosts the GPU surface.
        frame.focusableWindowState = false
        frame.isAutoRequestFocus = false
        frame.type = java.awt.Window.Type.UTILITY
        frame.pack()
        frame.setLocation(-8000, -8000)
        frame.isVisible = true
        val scale = layer.contentScale
        layer.preferredSize = Dimension(ceil(sceneWidth / scale).toInt(), ceil(sceneHeight / scale).toInt())
        frame.pack()
    }
    Thread.sleep(500)
    println("REPLAY renderer ${layer.renderApi} ${layer.renderInfo.lines().firstOrNull { "Video card" in it } ?: ""}")
    val trace = File(out, "trace.csv").bufferedWriter()
    trace.write("frame,ms,selectorX,bodyWidth,held,formation,protrusion,dragIndex\n")
    var next = 0
    var pressed = false
    val frames = (end * 60 / 1000).toInt()
    armed = true
    val started = System.nanoTime()
    for (index in 0..frames) {
        val target = (index * 1000.0 / 60).roundToLong()
        SwingUtilities.invokeAndWait {
            while (next < pointer.size && pointer[next].first <= target) {
                val (ms, kind, at) = pointer[next++]
                val type = when (kind) { "down" -> PointerEventType.Press; "up" -> PointerEventType.Release; else -> PointerEventType.Move }
                pressed = kind != "up"
                scene.sendPointerEvent(
                    type, listOf(ComposeScenePointer(PointerId(1), at, pressed, PointerType.Touch)),
                    timeMillis = ms.roundToLong(),
                )
            }
            nanos = target * 1_000_000
            val t0 = System.nanoTime()
            layer.renderImmediately()
            val t1 = System.nanoTime()
            val bitmap = checkNotNull(captured).also { captured = null }
            val t2 = System.nanoTime()
            Image.makeFromBitmap(bitmap).use { image ->
                File(out, "f%05d.png".format(index)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            bitmap.close()
            val t3 = System.nanoTime()
            if (index % 600 == 0) println("REPLAY cost render=${(t1 - t0) / 1e6} readback=${(t2 - t1) / 1e6} encode=${(t3 - t2) / 1e6}")
            val owner = scene.semanticsOwners.first()
            val node = owner.unmergedRootSemanticsNode.let { root ->
                fun find(n: androidx.compose.ui.semantics.SemanticsNode): androidx.compose.ui.semantics.SemanticsNode? =
                    if (n.config.any { it.key.name == "GlassTabBarSelectorX" }) n
                    else n.children.firstNotNullOfOrNull { find(it) }
                find(root)
            }
            val c = node?.config?.associate { it.key.name to it.value } ?: emptyMap()
            fun v(name: String) = c["GlassTabBar$name"]
            trace.write("$index,$target,${v("SelectorX")},${v("BodyWidth")},${v("Held")},${v("Formation")}," +
                "${v("Protrusion")},${v("DragIndex")}\n")
        }
        if (index % 120 == 0) { trace.flush(); println("REPLAY frame $index/$frames ${(System.nanoTime() - started) / 1_000_000} ms") }
    }
    trace.close()
    SwingUtilities.invokeAndWait { frame.dispose(); scene.close() }
    println("REPLAY done $frames frames in ${(System.nanoTime() - started) / 1_000_000} ms")
    kotlin.system.exitProcess(0)
}

/** The DirectContext behind [layer]'s current redrawer. Skiko does not expose it publicly. */
private fun gpuContext(layer: SkiaLayer): DirectContext? {
    val redrawer = checkNotNull(layer.javaClass.getMethod("getRedrawer\$skiko").invoke(layer)) { "No redrawer" }
    val handler = redrawer.javaClass.getDeclaredField("contextHandler").apply { isAccessible = true }.get(redrawer)
    var type: Class<*>? = handler.javaClass
    while (type != null) {
        type.declaredFields.firstOrNull { it.name == "context" }?.let {
            it.isAccessible = true
            return it.get(handler) as DirectContext?
        }
        type = type.superclass
    }
    error("No DirectContext on ${handler.javaClass.name}")
}
