package com.wexpa.liquidglass

import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Shared native renderer that renders the Liquid Glass effect by sampling from
 * a [GlassBackdrop]'s [RenderNode] using Android's native [RenderEffect] and [android.graphics.RuntimeShader].
 *
 * Reuses the library's exact optics, uniforms, and fallback rendering pipeline without
 * spinning up internal `ComposeView`s per surface.
 */
class GlassNativeRenderer(
    val targetView: View,
    var backdrop: GlassBackdrop? = null,
    var glass: Glass = Glass.Regular,
    var shape: Shape = CapsuleShape,
    var dark: Boolean = false,
    var backgroundColor: Color = if (dark) Color.Black else Color.White,
) {
    var touchX: Float = -1e5f
    var touchY: Float = -1e5f
    var touchAmount: Float = 0f

    val glassNode: RenderNode? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNode("LiquidGlassSurface")
    } else {
        null
    }

    private val viewLocation = IntArray(2)
    private val backdropLocation = IntArray(2)
    private val androidOutline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        android.graphics.Outline()
    } else {
        null
    }
    private val drawScope = CanvasDrawScope()

    private var lastWidth = -1
    private var lastHeight = -1
    private var lastDx = Float.NaN
    private var lastDy = Float.NaN
    private var lastBackdropNode: RenderNode? = null

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val node = glassNode
        val bd = backdrop
        val sourceNode = bd?.sourceNode
        val density = Density(targetView.context.resources.displayMetrics.density)

        if (node != null && sourceNode != null && canvas.isHardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            targetView.getLocationInWindow(viewLocation)
            bd.getLocationInWindow(backdropLocation)
            val dx = (backdropLocation[0] - viewLocation[0]).toFloat()
            val dy = (backdropLocation[1] - viewLocation[1]).toFloat()

            node.setPosition(0, 0, width, height)

            // Setup outline & clipping
            val composeOutline = shape.createOutline(
                Size(width.toFloat(), height.toFloat()),
                LayoutDirection.Ltr,
                density,
            )
            val outline = androidOutline
            if (outline != null) {
                when (composeOutline) {
                    is Outline.Rectangle -> {
                        outline.setRect(0, 0, width, height)
                        node.setOutline(outline)
                        node.setClipToOutline(true)
                    }
                    is Outline.Rounded -> {
                        val rr = composeOutline.roundRect
                        outline.setRoundRect(
                            rr.left.toInt(),
                            rr.top.toInt(),
                            rr.right.toInt(),
                            rr.bottom.toInt(),
                            rr.topLeftCornerRadius.x,
                        )
                        node.setOutline(outline)
                        node.setClipToOutline(true)
                    }
                    is Outline.Generic -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            outline.setPath(composeOutline.path.asAndroidPath())
                            node.setOutline(outline)
                            node.setClipToOutline(true)
                        } else {
                            node.setClipToOutline(false)
                        }
                    }
                }
            }

            // Re-record glassNode if geometry or source changed
            val needsRecord = width != lastWidth || height != lastHeight || dx != lastDx || dy != lastDy || sourceNode !== lastBackdropNode
            if (needsRecord) {
                val recordingCanvas = node.beginRecording(width, height)
                try {
                    recordingCanvas.translate(dx, dy)
                    recordingCanvas.drawRenderNode(sourceNode)
                } finally {
                    node.endRecording()
                }
                lastWidth = width
                lastHeight = height
                lastDx = dx
                lastDy = dy
                lastBackdropNode = sourceNode
            }

            // Apply shader / blur effect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val press = if (touchAmount > 0f) GlassPress(touchX, touchY, touchAmount) else GlassPress.None
                val uniforms = buildGlassPanelUniforms(
                    density = density,
                    shape = shape,
                    size = Size(width.toFloat(), height.toFloat()),
                    glass = glass,
                    dark = dark,
                    background = backgroundColor,
                    deltaX = 0f,
                    deltaY = 0f,
                    tiltX = 0f,
                    tiltY = 0f,
                    ambientAlpha = 1f,
                    pressAlpha = touchAmount,
                    press = press,
                    tintField = null,
                )
                val effect = createAndroidGlassRenderEffect(uniforms)
                node.setRenderEffect(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurPx = glass.style(dark)?.blurRadius?.value?.let { it * density.density } ?: 0f
                val blurEffect = if (blurPx > 0f) {
                    RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                } else {
                    null
                }
                node.setRenderEffect(blurEffect)
            }

            canvas.drawRenderNode(node)

            // On API 31-32 without RuntimeShader, draw lit rim / tint stroke on top
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.S until Build.VERSION_CODES.TIRAMISU) {
                drawFallbackOverlay(canvas, width, height, density)
            }
            return
        }

        // Software canvas or API < 31 fallback
        drawFallbackOverlay(canvas, width, height, density)
    }

    private fun drawFallbackOverlay(canvas: Canvas, width: Int, height: Int, density: Density) {
        val style = glass.style(dark) ?: GlassStyle.Regular
        val light = glass.light(dark)
        val radii = shape.glassRadii(Size(width.toFloat(), height.toFloat()), LayoutDirection.Ltr, density)
        val bevelPx = with(density) { style.bevel.toPx() }

        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = ComposeCanvas(canvas),
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            drawGlassFallback(style, light, radii, bevelPx)
        }
    }
}
