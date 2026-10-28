package com.wexpa.liquidglass

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Controller for an active glass effect attached to an Android [View] via [View.glassEffect].
 */
class GlassViewEffect internal constructor(
    val view: View,
    val renderer: GlassNativeRenderer,
    val drawable: GlassBackgroundDrawable,
) {
    var glass: Glass
        get() = renderer.glass
        set(value) {
            renderer.glass = value
            view.invalidate()
        }

    var shape: Shape
        get() = renderer.shape
        set(value) {
            renderer.shape = value
            view.invalidate()
        }

    var dark: Boolean
        get() = renderer.dark
        set(value) {
            renderer.dark = value
            view.invalidate()
        }

    var backgroundColor: Color
        get() = renderer.backgroundColor
        set(value) {
            renderer.backgroundColor = value
            view.invalidate()
        }

    fun update() {
        view.invalidate()
    }
}

/**
 * Custom [Drawable] that renders the Liquid Glass effect using [GlassNativeRenderer].
 */
class GlassBackgroundDrawable(
    val renderer: GlassNativeRenderer,
) : Drawable() {

    override fun draw(canvas: Canvas) {
        val b = bounds
        renderer.draw(canvas, b.width(), b.height())
    }

    override fun setAlpha(alpha: Int) {
        // Alpha handled by material/renderer
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Color filter handled by material/renderer
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Convenience API to apply a live Liquid Glass effect behind any Android [View] sampling from [backdrop].
 *
 * Traditional non-glass content and XML layouts execute Android's full native drawing and touch pipeline.
 * The glass effect renders natively via [GlassNativeRenderer] using hardware-accelerated RenderNodes and
 * Android 13+ RuntimeShaders.
 *
 * @param backdrop The [GlassBackdrop] container holding the content to refract.
 * @param glass The material style preset (default: [Glass.Regular]).
 * @param shape The outline shape for the glass panel (default: [CapsuleShape]).
 * @param dark Whether to force dark appearance, light appearance, or null to follow system dark mode.
 * @param backgroundColor Optional ground color behind the glass.
 * @param replaceBackground When true (default), sets the generated [GlassBackgroundDrawable] as the view's background.
 */
fun View.glassEffect(
    backdrop: GlassBackdrop,
    glass: Glass = Glass.Regular,
    shape: Shape = CapsuleShape,
    dark: Boolean? = null,
    backgroundColor: Color? = null,
    replaceBackground: Boolean = true,
): GlassViewEffect {
    val isDark = dark ?: run {
        val nightBits = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightBits == Configuration.UI_MODE_NIGHT_YES
    }
    val bg = backgroundColor ?: if (isDark) Color.Black else Color.White

    val renderer = GlassNativeRenderer(
        targetView = this,
        backdrop = backdrop,
        glass = glass,
        shape = shape,
        dark = isDark,
        backgroundColor = bg,
    )
    val drawable = GlassBackgroundDrawable(renderer)
    val effect = GlassViewEffect(this, renderer, drawable)
    setTag(R.id.liquid_glass_view_effect_tag, effect)

    if (replaceBackground) {
        background = drawable
    }

    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            backdrop.registerGlassConsumer(v)
        }

        override fun onViewDetachedFromWindow(v: View) {
            backdrop.unregisterGlassConsumer(v)
        }
    }
    addOnAttachStateChangeListener(attachListener)
    if (isAttachedToWindow) {
        backdrop.registerGlassConsumer(this)
    }

    return effect
}
