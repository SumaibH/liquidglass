package com.wexpa.liquidglass

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Collections
import java.util.WeakHashMap

/**
 * Idiomatic Android XML container that records ordinary non-glass content into a shared
 * [RenderNode] during normal native drawing.
 *
 * Traditional Android layouts (`FrameLayout`, `ConstraintLayout`, `ScrollView`, `RecyclerView`)
 * placed inside [GlassBackdrop] execute Android's full native 6-step draw pipeline
 * (background, `onDraw`, child dispatch with Z-ordering and `clipToPadding`, foreground).
 *
 * Glass surfaces (such as [LiquidGlassView] or views using [View.glassEffect]) sample directly
 * from [sourceNode] without ancestor tree traversal, visibility mutations, or re-entry issues.
 */
open class GlassBackdrop @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        // ViewGroup sets WILL_NOT_DRAW by default when no background is set.
        // Clearing WILL_NOT_DRAW ensures Android's hardware display list traversal calls draw(canvas)
        // rather than skipping directly to dispatchDraw, ensuring sourceNode is reliably recorded.
        setWillNotDraw(false)
    }

    /**
     * Shared [RenderNode] holding the recorded backdrop display list on API 29+.
     * Returns null on older Android versions or when hardware acceleration is disabled.
     */
    val sourceNode: RenderNode? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNode("LiquidGlassBackdrop")
    } else {
        null
    }

    /**
     * Set of glass consumer views registered against this backdrop container.
     */
    internal val nestedGlassViews: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

    @Volatile
    internal var isRecordingBackdrop: Boolean = false
        private set

    fun registerGlassConsumer(glassView: View) {
        if (isDescendantOf(glassView, this)) {
            throw IllegalStateException(
                "LiquidGlass consumer ($glassView) cannot be placed inside its own source backdrop ($this). " +
                    "Consumers must be placed outside their source subtree."
            )
        }
        nestedGlassViews.add(glassView)
    }

    fun unregisterGlassConsumer(glassView: View) {
        nestedGlassViews.remove(glassView)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (containsGlassConsumer(child)) {
            removeView(child)
            throw IllegalStateException(
                "LiquidGlass consumer ($child) cannot be added inside its own source backdrop ($this). " +
                    "Consumers must be placed outside their source subtree."
            )
        }
    }

    private fun containsGlassConsumer(view: View): Boolean {
        if (targetsThisBackdrop(view)) return true
        val group = view as? ViewGroup ?: return false
        for (i in 0 until group.childCount) {
            if (containsGlassConsumer(group.getChildAt(i))) return true
        }
        return false
    }

    private fun targetsThisBackdrop(view: View): Boolean {
        if (view in nestedGlassViews) return true
        val effect = view.getTag(R.id.liquid_glass_view_effect_tag) as? GlassViewEffect
        if (effect != null && effect.backdrop === this) return true
        val host = view as? LiquidGlassHostView
        if (host != null) {
            val bd = host.backdropView
            if (bd === this) return true
            if (bd == null && (host.backdropViewId == id || host.backdropViewId == View.NO_ID)) {
                return true
            }
        }
        return false
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return super.drawChild(canvas, child, drawingTime)
    }

    override fun draw(canvas: Canvas) {
        val node = sourceNode
        if (node != null && canvas.isHardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val w = width
            val h = height
            if (w > 0 && h > 0) {
                node.setPosition(0, 0, w, h)
                val recordingCanvas = node.beginRecording(w, h)
                isRecordingBackdrop = true
                currentRecordingBackdrop = this
                try {
                    super.draw(recordingCanvas)
                } finally {
                    currentRecordingBackdrop = null
                    isRecordingBackdrop = false
                    node.endRecording()
                }
                canvas.drawRenderNode(node)
                return
            }
        }
        super.draw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val node = sourceNode
        // If draw() was skipped or dispatchDraw was called directly while not recording,
        // ensure sourceNode is still captured on hardware-accelerated canvas.
        if (!isRecordingBackdrop && node != null && canvas.isHardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val w = width
            val h = height
            if (w > 0 && h > 0) {
                node.setPosition(0, 0, w, h)
                val recordingCanvas = node.beginRecording(w, h)
                isRecordingBackdrop = true
                currentRecordingBackdrop = this
                try {
                    super.dispatchDraw(recordingCanvas)
                } finally {
                    currentRecordingBackdrop = null
                    isRecordingBackdrop = false
                    node.endRecording()
                }
                canvas.drawRenderNode(node)
                return
            }
        }
        super.dispatchDraw(canvas)
    }

    companion object {
        @Volatile
        internal var currentRecordingBackdrop: GlassBackdrop? = null

        internal fun isGlassView(view: View): Boolean {
            return view is LiquidGlassView || view is LiquidGlassHostView || view.getTag(R.id.liquid_glass_view_effect_tag) != null
        }
    }
}

internal fun isDescendantOf(view: View, ancestor: View): Boolean {
    var p = view.parent
    while (p != null) {
        if (p === ancestor) return true
        p = (p as? View)?.parent
    }
    return false
}
