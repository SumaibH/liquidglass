package com.wexpa.liquidglass

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified

/**
 * Controller for an active glass effect attached to an Android [View] via [View.glassEffect].
 */
class GlassViewEffect internal constructor(
    val view: View,
    val renderer: GlassNativeRenderer,
    val drawable: GlassBackgroundDrawable,
    val backdrop: GlassBackdrop,
) {
    internal var isDisposed = false
        private set

    internal var previousBackground: android.graphics.drawable.Drawable? = null
    internal var attachListener: View.OnAttachStateChangeListener? = null
    internal var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    var touchListener: View.OnTouchListener? = null
        internal set

    private val lastMatrix = android.graphics.Matrix()
    private val currentMatrix = android.graphics.Matrix()

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

    /**
     * Deterministically disposes this effect, detaching observers and registrations.
     */
    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        attachListener?.let { view.removeOnAttachStateChangeListener(it) }
        attachListener = null

        val vto = view.viewTreeObserver
        preDrawListener?.let {
            if (vto.isAlive) {
                vto.removeOnPreDrawListener(it)
            }
        }
        preDrawListener = null

        (touchListener as? GlassTouchListener)?.clear()
        touchListener = null

        if (view.background === drawable) {
            val pad = getRawPadding(view)
            view.background = previousBackground
            pad.applyTo(view)
        }
        previousBackground = null

        backdrop.unregisterGlassConsumer(view)

        if (view.getTag(R.id.liquid_glass_view_effect_tag) === this) {
            view.setTag(R.id.liquid_glass_view_effect_tag, null)
        }
    }

    internal fun checkGeometryChanged(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentMatrix.reset()
            backdrop.transformMatrixToGlobal(currentMatrix)
            view.transformMatrixToLocal(currentMatrix)
            if (currentMatrix != lastMatrix) {
                lastMatrix.set(currentMatrix)
                return true
            }
        }
        return false
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
 *
 * Note: If [glass] is interactive ([Glass.isInteractive]), this function installs a [View.OnTouchListener]
 * on the view to drive interaction state while returning false to preserve click dispatch. This takes
 * ownership of the view's [View.setOnTouchListener] slot.
 */
fun View.glassEffect(
    backdrop: GlassBackdrop,
    glass: Glass = Glass.Regular,
    shape: Shape = CapsuleShape,
    dark: Boolean? = null,
    backgroundColor: Color? = null,
    replaceBackground: Boolean = true,
): GlassViewEffect {
    checkNoCycleWithAncestors(this, backdrop)

    // Finding 11: Release previous attachment when glassEffect is reapplied
    (getTag(R.id.liquid_glass_view_effect_tag) as? GlassViewEffect)?.dispose()

    val isDark = dark ?: run {
        val nightBits = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightBits == Configuration.UI_MODE_NIGHT_YES
    }
    val bg = backgroundColor?.takeIf { it.isSpecified } ?: if (isDark) Color.Black else Color.White

    val renderer = GlassNativeRenderer(
        targetView = this,
        backdrop = backdrop,
        glass = glass,
        shape = shape,
        dark = isDark,
        backgroundColor = bg,
    )
    val drawable = GlassBackgroundDrawable(renderer)
    val effect = GlassViewEffect(this, renderer, drawable, backdrop)
    setTag(R.id.liquid_glass_view_effect_tag, effect)

    if (replaceBackground) {
        effect.previousBackground = background
        val pad = getRawPadding(this)
        background = drawable
        pad.applyTo(this)
    }

    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            checkNoCycleWithAncestors(v, backdrop)
            backdrop.registerGlassConsumer(v)
            val vto = v.viewTreeObserver
            effect.preDrawListener?.let { l ->
                if (vto.isAlive) vto.addOnPreDrawListener(l)
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            backdrop.unregisterGlassConsumer(v)
            val vto = v.viewTreeObserver
            effect.preDrawListener?.let { l ->
                if (vto.isAlive) vto.removeOnPreDrawListener(l)
            }
        }
    }
    effect.attachListener = attachListener
    addOnAttachStateChangeListener(attachListener)

    // Finding 13: Refresh source mapping when a glass View moves
    val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (effect.isDisposed) return@OnPreDrawListener true
        if (effect.checkGeometryChanged()) {
            invalidate()
        }
        true
    }
    effect.preDrawListener = preDrawListener

    if (isAttachedToWindow) {
        backdrop.registerGlassConsumer(this)
        val vto = viewTreeObserver
        if (vto.isAlive) {
            vto.addOnPreDrawListener(preDrawListener)
        }
    }

    // Finding 9: Wire shared interaction state while preserving native gesture dispatch
    if (glass.isInteractive) {
        val touchListener = GlassTouchListener(effect, renderer)
        effect.touchListener = touchListener
        setOnTouchListener(touchListener)
    }

    return effect
}

private class SavedPadding(
    val isRelative: Boolean,
    val a: Int,
    val top: Int,
    val b: Int,
    val bottom: Int,
) {
    fun applyTo(view: View) {
        if (isRelative) {
            view.setPaddingRelative(a, top, b, bottom)
        } else {
            view.setPadding(a, top, b, bottom)
        }
    }
}

private fun getRawPadding(view: View): SavedPadding {
    val isRelative = view.isPaddingRelative
    val scrollBarSize = if (view.scrollBarStyle == View.SCROLLBARS_INSIDE_INSET) {
        ViewConfiguration.get(view.context).scaledScrollBarSize
    } else {
        0
    }
    val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL

    if (isRelative) {
        var start = view.paddingStart
        val top = view.paddingTop
        var end = view.paddingEnd
        var bottom = view.paddingBottom

        if (scrollBarSize > 0) {
            val onLeft = view.verticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT ||
                (view.verticalScrollbarPosition == View.SCROLLBAR_POSITION_DEFAULT && isRtl)
            if (view.isVerticalScrollBarEnabled) {
                if (onLeft) {
                    if (isRtl) {
                        end = (end - scrollBarSize).coerceAtLeast(0)
                    } else {
                        start = (start - scrollBarSize).coerceAtLeast(0)
                    }
                } else {
                    if (isRtl) {
                        start = (start - scrollBarSize).coerceAtLeast(0)
                    } else {
                        end = (end - scrollBarSize).coerceAtLeast(0)
                    }
                }
            }
            if (view.isHorizontalScrollBarEnabled) {
                bottom = (bottom - scrollBarSize).coerceAtLeast(0)
            }
        }
        return SavedPadding(isRelative = true, a = start, top = top, b = end, bottom = bottom)
    } else {
        var left = view.paddingLeft
        val top = view.paddingTop
        var right = view.paddingRight
        var bottom = view.paddingBottom

        if (scrollBarSize > 0) {
            val onLeft = view.verticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT ||
                (view.verticalScrollbarPosition == View.SCROLLBAR_POSITION_DEFAULT && isRtl)
            if (view.isVerticalScrollBarEnabled) {
                if (onLeft) {
                    left = (left - scrollBarSize).coerceAtLeast(0)
                } else {
                    right = (right - scrollBarSize).coerceAtLeast(0)
                }
            }
            if (view.isHorizontalScrollBarEnabled) {
                bottom = (bottom - scrollBarSize).coerceAtLeast(0)
            }
        }
        return SavedPadding(isRelative = false, a = left, top = top, b = right, bottom = bottom)
    }
}

private class GlassTouchListener(
    effect: GlassViewEffect,
    renderer: GlassNativeRenderer,
) : View.OnTouchListener {
    private var effectRef: java.lang.ref.WeakReference<GlassViewEffect>? = java.lang.ref.WeakReference(effect)
    private var rendererRef: java.lang.ref.WeakReference<GlassNativeRenderer>? = java.lang.ref.WeakReference(renderer)

    fun clear() {
        effectRef = null
        rendererRef = null
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val effect = effectRef?.get() ?: return false
        val r = rendererRef?.get() ?: return false
        if (effect.isDisposed) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                r.touchX = event.x
                r.touchY = event.y
                r.touchAmount = 1f
                v.invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                r.touchX = event.x
                r.touchY = event.y
                v.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                r.touchAmount = 0f
                v.invalidate()
            }
        }
        return false // Preserve native click and touch delivery
    }
}
