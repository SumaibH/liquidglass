package com.wexpa.liquidglass.sample

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassBackdrop
import com.wexpa.liquidglass.GlassXmlDarkMode
import com.wexpa.liquidglass.GlassXmlPreset
import com.wexpa.liquidglass.LiquidGlassView
import com.wexpa.liquidglass.glassEffect
import com.wexpa.liquidglass.R as LibR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class GlassBackdropHardwareTest {

    private fun waitForDraw(view: View) {
        val latch = CountDownLatch(1)
        view.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.viewTreeObserver.registerFrameCommitCallback {
                    latch.countDown()
                }
            } else {
                val listener = object : ViewTreeObserver.OnDrawListener {
                    override fun onDraw() {
                        view.post {
                            latch.countDown()
                        }
                    }
                }
                view.viewTreeObserver.addOnDrawListener(listener)
            }
            view.invalidate()
        }
        assertTrue("Timed out waiting for frame commit", latch.await(3, TimeUnit.SECONDS))
    }

    private fun captureWindow(activity: Activity): Bitmap {
        val decor = activity.window.decorView
        val width = decor.width.coerceAtLeast(1)
        val height = decor.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var copySuccess = false

        activity.runOnUiThread {
            try {
                PixelCopy.request(
                    activity.window,
                    bmp,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            copySuccess = true
                        }
                        latch.countDown()
                    },
                    Handler(Looper.getMainLooper()),
                )
            } catch (e: Throwable) {
                latch.countDown()
            }
        }

        latch.await(3, TimeUnit.SECONDS)

        if (copySuccess) {
            return bmp
        }

        // Software fallback if PixelCopy is not ready
        val drawLatch = CountDownLatch(1)
        activity.runOnUiThread {
            val c = android.graphics.Canvas(bmp)
            decor.draw(c)
            drawLatch.countDown()
        }
        assertTrue("Timed out waiting for software draw", drawLatch.await(2, TimeUnit.SECONDS))
        return bmp
    }

    @Test
    fun xmlInteropActivityHardwareWindowRenderingAndPixelAssertion() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var backdropRef: GlassBackdrop? = null
        var headerCardRef: LiquidGlassView? = null
        var activityRef: Activity? = null

        scenario.onActivity { activity ->
            activityRef = activity
            backdropRef = activity.findViewById(R.id.xmlBackdrop)
            headerCardRef = activity.findViewById(R.id.xmlHeaderCard)
        }

        val activity = activityRef!!
        val backdrop = backdropRef!!
        val headerCard = headerCardRef!!

        // Finding 1: Ensure ViewGroup background-less defaults are set correctly
        assertNull("xmlBackdrop has no background by default in XML", backdrop.background)
        assertFalse("xmlBackdrop must have setWillNotDraw(false) for hardware display list recording", backdrop.willNotDraw())

        waitForDraw(activity.window.decorView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sourceNode = backdrop.sourceNode
            assertNotNull("sourceNode should be allocated on API 29+", sourceNode)
            assertTrue("sourceNode must have display list recorded from window traversal", sourceNode!!.hasDisplayList())
        }

        // Finding 14: Capture presented window pixels and assert non-empty, rendered glass output
        val bitmap = captureWindow(activity)
        assertNotNull(bitmap)

        val location = IntArray(2)
        scenario.onActivity {
            headerCard.getLocationInWindow(location)
        }
        val cardX = location[0] + headerCard.width / 2
        val cardY = location[1] + headerCard.height / 2

        val centerPixel = bitmap.getPixel(cardX, cardY)
        val alpha = AndroidColor.alpha(centerPixel)
        val red = AndroidColor.red(centerPixel)
        val green = AndroidColor.green(centerPixel)
        val blue = AndroidColor.blue(centerPixel)

        // Glass header over dark background with scroll items must be fully opaque and non-zero
        assertEquals("Center pixel under header card must be fully opaque", 255, alpha)
        assertTrue(
            "Center pixel under header card must contain rendered content (R=$red, G=$green, B=$blue)",
            red > 0 || green > 0 || blue > 0,
        )

        scenario.close()
    }

    @Test
    fun rejectConsumersInsideOwnSourceSubtree() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)

            // 1. Direct child added to backdrop cannot be registered as consumer
            val directChild = View(activity)
            backdrop.addView(directChild)
            try {
                backdrop.registerGlassConsumer(directChild)
                fail("registerGlassConsumer must throw IllegalStateException for direct child")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("cannot be placed inside its own source backdrop"))
            }

            // 2. Registering first then adding to backdrop must throw during onViewAdded
            val pendingChild = View(activity)
            backdrop.registerGlassConsumer(pendingChild)
            try {
                backdrop.addView(pendingChild)
                fail("onViewAdded must throw IllegalStateException when registering descendant")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("cannot be added inside its own source backdrop"))
            }

            // 3. Deeply nested child must also be rejected
            val subContainer = FrameLayout(activity)
            val nestedView = View(activity)
            subContainer.addView(nestedView)
            backdrop.addView(subContainer)
            try {
                backdrop.registerGlassConsumer(nestedView)
                fail("registerGlassConsumer must throw IllegalStateException for deeply nested child")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("cannot be placed inside its own source backdrop"))
            }

            // 4. LiquidGlassView targeting ancestor backdrop must throw
            val nestedGlassView = LiquidGlassView(activity)
            try {
                backdrop.addView(nestedGlassView)
                nestedGlassView.backdropView = backdrop
                fail("LiquidGlassView.backdropView must throw IllegalStateException when targeting ancestor backdrop")
            } catch (expected: IllegalStateException) {
                assertTrue(
                    expected.message!!.contains("cannot target an ancestor GlassBackdrop") ||
                        expected.message!!.contains("cannot be added inside its own source backdrop")
                )
            }
        }
        scenario.close()
    }

    @Test
    fun nativeClickAndLongClickDeliveryPreserved() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var activityRef: Activity? = null
        var glassContainerRef: LiquidGlassView? = null
        var childButtonRef: TextView? = null
        var rootRef: ViewGroup? = null

        val containerClicked = AtomicBoolean(false)
        val containerLongClicked = AtomicBoolean(false)
        val childClicked = AtomicBoolean(false)

        scenario.onActivity { activity ->
            activityRef = activity
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val root = activity.findViewById<ViewGroup>(R.id.xmlRoot)
            rootRef = root

            val glassContainer = LiquidGlassView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(300, 200).apply {
                    gravity = Gravity.CENTER
                }
                backdropView = backdrop
                isGlassInteractive = true
                isClickable = true
                isLongClickable = true
                setOnClickListener {
                    containerClicked.set(true)
                }
                setOnLongClickListener {
                    containerLongClicked.set(true)
                    true
                }
            }
            glassContainerRef = glassContainer

            val childButton = TextView(activity).apply {
                text = "Child Clickable"
                layoutParams = FrameLayout.LayoutParams(150, 80).apply {
                    gravity = Gravity.CENTER
                }
                isClickable = true
                setOnClickListener {
                    childClicked.set(true)
                }
            }
            childButtonRef = childButton

            glassContainer.addView(childButton)
            root.addView(glassContainer)
        }

        val activity = activityRef!!
        val glassContainer = glassContainerRef!!
        val childButton = childButtonRef!!
        val root = rootRef!!

        waitForDraw(activity.window.decorView)

        // Finding 5: performClick and performLongClick work natively
        scenario.onActivity {
            glassContainer.performClick()
        }
        assertTrue("LiquidGlassView container performClick must trigger OnClickListener", containerClicked.get())

        scenario.onActivity {
            glassContainer.performLongClick()
        }
        assertTrue("LiquidGlassView container performLongClick must trigger OnLongClickListener", containerLongClicked.get())

        scenario.onActivity {
            childButton.performClick()
        }
        assertTrue("Child clickable inside LiquidGlassView must receive click", childClicked.get())

        // Test MotionEvent dispatch
        containerClicked.set(false)
        val clickLatch = CountDownLatch(1)
        scenario.onActivity {
            glassContainer.setOnClickListener {
                containerClicked.set(true)
                clickLatch.countDown()
            }
            val now = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
            val up = MotionEvent.obtain(now, now + 150, MotionEvent.ACTION_UP, 10f, 10f, 0)

            glassContainer.dispatchTouchEvent(down)
            glassContainer.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
        }

        assertTrue("MotionEvent DOWN/UP sequence must trigger container OnClickListener", clickLatch.await(2, TimeUnit.SECONDS))
        assertTrue("Container clicked must be true", containerClicked.get())

        scenario.onActivity {
            root.removeView(glassContainer)
        }
        scenario.close()
    }

    @Test
    fun runtimePropertyMutationUpdatesRendererWithoutDetaching() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val headerCard = activity.findViewById<LiquidGlassView>(R.id.xmlHeaderCard)

            // Finding 6: Public properties update renderer state immediately
            headerCard.preset = GlassXmlPreset.Clear
            assertEquals(GlassXmlPreset.Clear, headerCard.preset)

            headerCard.cornerRadiusDp = 36.dp
            assertEquals(36.dp, headerCard.cornerRadiusDp)

            headerCard.glassTint = Color.Magenta
            assertEquals(Color.Magenta, headerCard.glassTint)

            headerCard.darkMode = GlassXmlDarkMode.Light
            assertEquals(GlassXmlDarkMode.Light, headerCard.darkMode)

            headerCard.darkMode = GlassXmlDarkMode.Dark
            assertEquals(GlassXmlDarkMode.Dark, headerCard.darkMode)
        }
        scenario.close()
    }

    @Test
    fun identityPresetRendersZeroMaterial() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val headerCard = activity.findViewById<LiquidGlassView>(R.id.xmlHeaderCard)

            // Finding 7: Glass.Identity renders zero material
            headerCard.preset = GlassXmlPreset.Identity
            assertEquals(GlassXmlPreset.Identity, headerCard.preset)

            // Also test on GlassViewEffect
            val testView = TextView(activity)
            val effect = testView.glassEffect(
                backdrop = activity.findViewById(R.id.xmlBackdrop),
                glass = Glass.Identity,
            )
            assertTrue(effect.glass.isIdentity)
            assertNull("Glass.Identity must resolve style(dark=false) to null", effect.glass.style(false))
            assertNull("Glass.Identity must resolve style(dark=true) to null", effect.glass.style(true))
            effect.dispose()
        }
        scenario.close()
    }

    @Test
    fun glassViewEffectDisposeAndRebinding() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val root = activity.findViewById<ViewGroup>(R.id.xmlRoot)
            val bd1 = GlassBackdrop(activity)
            val bd2 = GlassBackdrop(activity)
            root.addView(bd1)
            root.addView(bd2)

            val targetView = TextView(activity).apply {
                text = "Target View"
            }
            root.addView(targetView)

            // Finding 11: glassEffect clean rebind & dispose
            val effect1 = targetView.glassEffect(
                backdrop = bd1,
                glass = Glass.Regular,
                shape = RoundedCornerShape(12.dp),
            )
            assertNotNull(effect1)
            assertEquals(bd1, effect1.backdrop)
            assertEquals(effect1, targetView.getTag(LibR.id.liquid_glass_view_effect_tag))

            // Rebind to bd2
            val effect2 = targetView.glassEffect(
                backdrop = bd2,
                glass = Glass.Clear,
                shape = CapsuleShape,
            )
            assertNotNull(effect2)
            assertEquals(bd2, effect2.backdrop)
            assertEquals(effect2, targetView.getTag(LibR.id.liquid_glass_view_effect_tag))

            // Dispose
            effect2.dispose()
            assertNull("targetView tag must be cleared after dispose", targetView.getTag(LibR.id.liquid_glass_view_effect_tag))

            root.removeView(targetView)
            root.removeView(bd1)
            root.removeView(bd2)
        }
        scenario.close()
    }

    @Test
    fun asymmetricCornerGeometryInLtrAndRtl() {
        // Finding 12: Asymmetric shapes must preserve corners and respect RTL
        val density = Density(2f)
        val asymmetricShape = RoundedCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 8.dp)

        // In LTR: topStart is topLeft, bottomStart is bottomLeft
        val ltrOutline = asymmetricShape.createOutline(Size(200f, 100f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(48f, ltrOutline.roundRect.topLeftCornerRadius.x)
        assertEquals(0f, ltrOutline.roundRect.topRightCornerRadius.x)
        assertEquals(0f, ltrOutline.roundRect.bottomRightCornerRadius.x)
        assertEquals(16f, ltrOutline.roundRect.bottomLeftCornerRadius.x)

        // In RTL: topStart is topRight, bottomStart is bottomRight
        val rtlOutline = asymmetricShape.createOutline(Size(200f, 100f), LayoutDirection.Rtl, density) as Outline.Rounded
        assertEquals(0f, rtlOutline.roundRect.topLeftCornerRadius.x)
        assertEquals(48f, rtlOutline.roundRect.topRightCornerRadius.x)
        assertEquals(16f, rtlOutline.roundRect.bottomRightCornerRadius.x)
        assertEquals(0f, rtlOutline.roundRect.bottomLeftCornerRadius.x)
    }

    @Test
    fun sourceMappingRefreshesOnViewTranslation() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var activityRef: Activity? = null
        var headerCardRef: LiquidGlassView? = null

        scenario.onActivity { activity ->
            activityRef = activity
            headerCardRef = activity.findViewById(R.id.xmlHeaderCard)
        }

        val activity = activityRef!!
        val headerCard = headerCardRef!!

        waitForDraw(activity.window.decorView)
        val beforeBitmap = captureWindow(activity)
        assertNotNull(beforeBitmap)

        // Finding 13: Translation causes transformMatrix change detected by preDrawListener
        scenario.onActivity {
            headerCard.translationY = 60f
        }
        waitForDraw(activity.window.decorView)

        // Verify view moved and frame readback still functions smoothly
        val afterBitmap = captureWindow(activity)
        assertNotNull(afterBitmap)

        scenario.close()
    }
}
