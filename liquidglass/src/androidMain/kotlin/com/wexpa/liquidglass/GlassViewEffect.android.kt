package com.wexpa.liquidglass

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import android.view.View
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

    internal var attachListener: View.OnAttachStateChangeListener? = null
    internal var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    internal var touchListener: View.OnTouchListener? = null

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

        touchListener = null

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
 */
fun View.glassEffect(
    backdrop: GlassBackdrop,
    glass: Glass = Glass.Regular,
    shape: Shape = CapsuleShape,
    dark: Boolean? = null,
    backgroundColor: Color? = null,
    replaceBackground: Boolean = true,
): GlassViewEffect {
    require(!isDescendantOf(this, backdrop)) {
        "LiquidGlass consumer ($this) cannot target an ancestor GlassBackdrop ($backdrop). " +
            "Consumers must be placed outside their source subtree."
    }

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
        background = drawable
    }

    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            require(!isDescendantOf(v, backdrop)) {
                "LiquidGlass consumer ($v) cannot be placed inside its own source backdrop ($backdrop)."
            }
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
        val touchListener = View.OnTouchListener { v, event ->
            if (effect.isDisposed) return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    renderer.touchX = event.x
                    renderer.touchY = event.y
                    renderer.touchAmount = 1f
                    v.invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    renderer.touchX = event.x
                    renderer.touchY = event.y
                    v.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    renderer.touchAmount = 0f
                    v.invalidate()
                }
            }
            false // Preserve native click and touch delivery
        }
        effect.touchListener = touchListener
        setOnTouchListener(touchListener)
    }

    return effect
}
