package com.wexpa.liquidglass

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.util.AttributeSet
import android.view.View
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
     * Set of nested glass consumer views registered inside this backdrop container.
     * Nested glass consumers are excluded from [sourceNode] recording to prevent recursive
     * self-sampling and are drawn directly to the target canvas after the backdrop.
     */
    internal val nestedGlassViews: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

    @Volatile
    internal var isRecordingBackdrop: Boolean = false
        private set

    fun registerGlassConsumer(glassView: View) {
        nestedGlassViews.add(glassView)
    }

    fun unregisterGlassConsumer(glassView: View) {
        nestedGlassViews.remove(glassView)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        // If we are recording the non-glass backdrop into sourceNode, exclude registered nested glass views.
        if (isRecordingBackdrop && (child in nestedGlassViews || isGlassView(child))) {
            return false
        }
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

                // Draw direct nested glass surfaces on top of the rendered backdrop into the real window canvas.
                val count = childCount
                for (i in 0 until count) {
                    val child = getChildAt(i) ?: continue
                    if ((child in nestedGlassViews || isGlassView(child)) && child.visibility == View.VISIBLE) {
                        drawChild(canvas, child, drawingTime)
                    }
                }
                return
            }
        }
        super.draw(canvas)
    }

    companion object {
        @Volatile
        internal var currentRecordingBackdrop: GlassBackdrop? = null

        private fun isGlassView(view: View): Boolean {
            return view is LiquidGlassView || view is LiquidGlassHostView || view.getTag(R.id.liquid_glass_view_effect_tag) != null
        }
    }
}
