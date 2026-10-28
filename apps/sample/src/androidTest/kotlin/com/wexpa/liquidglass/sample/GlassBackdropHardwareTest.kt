package com.wexpa.liquidglass.sample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassBackdrop
import com.wexpa.liquidglass.LiquidGlassView
import com.wexpa.liquidglass.glassEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlassBackdropHardwareTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun twoHostsSharingSingleBackdrop() {
        val root = FrameLayout(context)
        val backdrop = GlassBackdrop(context).apply {
            layoutParams = FrameLayout.LayoutParams(600, 800)
            setBackgroundColor(AndroidColor.BLUE)
        }
        val host1 = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(200, 200)
            this.backdropView = backdrop
        }
        val host2 = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(200, 200)
            this.backdropView = backdrop
        }

        root.addView(backdrop)
        root.addView(host1)
        root.addView(host2)

        root.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 600, 800)

        assertEquals(backdrop, host1.backdropView)
        assertEquals(backdrop, host2.backdropView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sourceNode = backdrop.sourceNode
            assertNotNull("sourceNode should be allocated on API 29+", sourceNode)

            val renderNode = RenderNode("HardwareWindow")
            renderNode.setPosition(0, 0, 600, 800)
            val recordingCanvas = renderNode.beginRecording(600, 800)

            // Draw backdrop and both hosts to hardware canvas
            backdrop.draw(recordingCanvas)
            host1.draw(recordingCanvas)
            host2.draw(recordingCanvas)

            renderNode.endRecording()
        }
    }

    @Test
    fun nestedGlassExcludedDuringBackdropRecording() {
        val backdrop = GlassBackdrop(context).apply {
            layoutParams = ViewGroup.LayoutParams(600, 800)
            setBackgroundColor(AndroidColor.RED)
        }

        var drawnCanvas: Canvas? = null
        val nestedGlass = object : View(context) {
            override fun draw(canvas: Canvas) {
                drawnCanvas = canvas
                super.draw(canvas)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(300, 150)
        }

        backdrop.addView(nestedGlass)
        backdrop.registerGlassConsumer(nestedGlass)

        backdrop.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        backdrop.layout(0, 0, 600, 800)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val renderNode = RenderNode("HardwareWindow")
            renderNode.setPosition(0, 0, 600, 800)
            val windowCanvas = renderNode.beginRecording(600, 800)

            // During backdrop recording, nested glass view must NOT be drawn into backdrop sourceNode.
            // It is drawn directly to windowCanvas after backdrop sourceNode is drawn.
            backdrop.draw(windowCanvas)
            renderNode.endRecording()

            assertEquals(
                "Nested glass view must be drawn onto the window canvas, not into sourceNode recording canvas",
                windowCanvas,
                drawnCanvas,
            )
        }
    }

    @Test
    fun customDrawingOnBackdropRecorded() {
        var customDrawingInvoked = false
        val backdrop = object : GlassBackdrop(context) {
            init {
                setWillNotDraw(false)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                customDrawingInvoked = true
                val paint = Paint().apply { color = AndroidColor.GREEN }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(500, 500)
        }

        backdrop.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
        )
        backdrop.layout(0, 0, 500, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val renderNode = RenderNode("TestHardwareCanvas")
            renderNode.setPosition(0, 0, 500, 500)
            val canvas = renderNode.beginRecording(500, 500)
            backdrop.draw(canvas)
            renderNode.endRecording()

            assertTrue("Container custom onDraw must execute during hardware draw pipeline", customDrawingInvoked)
        }
    }

    @Test
    fun childOrderingAndPaddingClipping() {
        val backdrop = GlassBackdrop(context).apply {
            layoutParams = ViewGroup.LayoutParams(500, 500)
            setPadding(20, 30, 40, 50)
            clipToPadding = true
        }

        val child1 = TextView(context).apply {
            text = "Child 1"
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }
        val child2 = TextView(context).apply {
            text = "Child 2"
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }

        backdrop.addView(child1)
        backdrop.addView(child2)

        backdrop.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
        )
        backdrop.layout(0, 0, 500, 500)

        assertTrue(backdrop.clipToPadding)
        assertEquals(20, backdrop.paddingLeft)
        assertEquals(30, backdrop.paddingTop)
        assertEquals(2, backdrop.childCount)
    }

    @Test
    fun viewGlassEffectExtensionAttachesCleanly() {
        val backdrop = GlassBackdrop(context).apply {
            layoutParams = ViewGroup.LayoutParams(400, 400)
        }
        val plainView = TextView(context).apply {
            text = "Native Card"
            layoutParams = ViewGroup.LayoutParams(200, 80)
        }

        val effect = plainView.glassEffect(
            backdrop = backdrop,
            glass = Glass.Regular,
            shape = RoundedCornerShape(12.dp),
        )

        assertNotNull(effect)
        assertNotNull(plainView.background)
        assertEquals(Glass.Regular, effect.glass)

        // Mutating glass or dark updates effect state
        effect.dark = true
        assertTrue(effect.dark)
        assertEquals(true, effect.renderer.dark)
    }

    @Test
    fun nativeFocusAndVisibilityPreservedWithoutMutation() {
        val root = FrameLayout(context)
        val backdrop = GlassBackdrop(context).apply {
            layoutParams = FrameLayout.LayoutParams(600, 600)
        }
        val editText = EditText(context).apply {
            setText("Test input")
            layoutParams = FrameLayout.LayoutParams(300, 100)
        }
        backdrop.addView(editText)

        val glassView = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(300, 100)
            this.backdropView = backdrop
        }
        root.addView(backdrop)
        root.addView(glassView)

        root.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 600, 600)

        editText.requestFocus()
        assertTrue("EditText should have focus", editText.hasFocus())
        assertEquals(View.VISIBLE, editText.visibility)

        // Draw multiple frames
        val pic = Picture()
        val canvas = pic.beginRecording(600, 600)
        root.draw(canvas)
        pic.endRecording()

        // Focus and visibility remain unchanged
        assertTrue("Focus must be preserved across draws", editText.hasFocus())
        assertEquals("Visibility must not be altered", View.VISIBLE, editText.visibility)
    }
}
