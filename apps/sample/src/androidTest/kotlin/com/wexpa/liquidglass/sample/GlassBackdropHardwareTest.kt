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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
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
import com.wexpa.liquidglass.LiquidGlassHostView
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
        assertTrue("Window must be hardware accelerated", decor.isHardwareAccelerated)
        val width = decor.width.coerceAtLeast(1)
        val height = decor.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var copyResult = -1

        activity.runOnUiThread {
            try {
                PixelCopy.request(
                    activity.window,
                    bmp,
                    { result ->
                        copyResult = result
                        latch.countDown()
                    },
                    Handler(Looper.getMainLooper()),
                )
            } catch (e: Throwable) {
                latch.countDown()
            }
        }

        assertTrue("Timed out waiting for PixelCopy", latch.await(3, TimeUnit.SECONDS))
        assertEquals("PixelCopy must succeed with SUCCESS (code 0)", PixelCopy.SUCCESS, copyResult)
        return bmp
    }

    private fun safeClose(scenario: ActivityScenario<*>) {
        try {
            safeClose(scenario)
        } catch (_: Throwable) {
        }
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

        safeClose(scenario)
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
        safeClose(scenario)
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
        safeClose(scenario)
    }

    @Test
    fun runtimePropertyMutationUpdatesRendererWithoutDetaching() {
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
        val location = IntArray(2)
        scenario.onActivity {
            headerCard.getLocationInWindow(location)
        }
        val cardX = location[0] + headerCard.width / 2
        val cardY = location[1] + headerCard.height / 2
        val pixelBefore = beforeBitmap.getPixel(cardX, cardY)

        scenario.onActivity {
            headerCard.preset = GlassXmlPreset.Clear
            assertEquals(GlassXmlPreset.Clear, headerCard.preset)

            headerCard.cornerRadiusDp = 36.dp
            assertEquals(36.dp, headerCard.cornerRadiusDp)

            headerCard.glassTint = Color.Magenta
            assertEquals(Color.Magenta, headerCard.glassTint)
        }
        waitForDraw(activity.window.decorView)
        val afterBitmap = captureWindow(activity)
        val pixelAfter = afterBitmap.getPixel(cardX, cardY)

        assertTrue("Mutating glassTint to Magenta must alter rendered pixels", pixelBefore != pixelAfter)
        assertTrue("Magenta tint must produce red and blue components", AndroidColor.red(pixelAfter) > 0 && AndroidColor.blue(pixelAfter) > 0)

        scenario.onActivity {
            headerCard.darkMode = GlassXmlDarkMode.Light
            assertEquals(GlassXmlDarkMode.Light, headerCard.darkMode)

            headerCard.darkMode = GlassXmlDarkMode.Dark
            assertEquals(GlassXmlDarkMode.Dark, headerCard.darkMode)
        }
        safeClose(scenario)
    }

    @Test
    fun identityPresetRendersZeroMaterial() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var activityRef: Activity? = null
        var testGlassRef: LiquidGlassView? = null

        scenario.onActivity { activity ->
            activityRef = activity
            val root = activity.findViewById<ViewGroup>(R.id.xmlRoot)
            val backdrop = GlassBackdrop(activity).apply {
                layoutParams = FrameLayout.LayoutParams(400, 400)
            }
            val greenContent = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(400, 400)
                setBackgroundColor(AndroidColor.GREEN)
            }
            backdrop.addView(greenContent)
            root.addView(backdrop)

            val glassView = LiquidGlassView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(200, 200).apply {
                    leftMargin = 50
                    topMargin = 50
                }
                this.backdropView = backdrop
                preset = GlassXmlPreset.Identity
            }
            root.addView(glassView)
            testGlassRef = glassView
        }
        val activity = activityRef!!
        val glassView = testGlassRef!!

        waitForDraw(activity.window.decorView)
        val bmp = captureWindow(activity)
        assertNotNull(bmp)

        val location = IntArray(2)
        scenario.onActivity {
            glassView.getLocationInWindow(location)
        }
        val centerX = location[0] + glassView.width / 2
        val centerY = location[1] + glassView.height / 2
        val pixel = bmp.getPixel(centerX, centerY)

        // Identity preset has style == null: zero material distortion, preserving exact backdrop green
        assertEquals("Identity preset renders zero glass material, matching pure backdrop green", AndroidColor.GREEN, pixel)

        // Also test on GlassViewEffect: Identity represents zero glass material (no blur/refraction)
        scenario.onActivity {
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
        safeClose(scenario)
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
        safeClose(scenario)
    }

    @Test
    fun asymmetricCornerGeometryInLtrAndRtl() {
        // Finding 12: Asymmetric shapes must preserve corners and respect RTL in GlassNativeRenderer
        val density = Density(2f)
        val asymmetricShape = RoundedCornerShape(topStart = 40.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 8.dp)

        // In LTR: topStart is topLeft (40dp = 80px), topEnd is topRight (0px)
        val ltrOutline = asymmetricShape.createOutline(Size(200f, 100f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(80f, ltrOutline.roundRect.topLeftCornerRadius.x)
        assertEquals(0f, ltrOutline.roundRect.topRightCornerRadius.x)

        val ltrPath = android.graphics.Path().apply {
            val rr = ltrOutline.roundRect
            val rArray = floatArrayOf(
                rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y,
                rr.topRightCornerRadius.x, rr.topRightCornerRadius.y,
                rr.bottomRightCornerRadius.x, rr.bottomRightCornerRadius.y,
                rr.bottomLeftCornerRadius.x, rr.bottomLeftCornerRadius.y,
            )
            addRoundRect(rr.left, rr.top, rr.right, rr.bottom, rArray, android.graphics.Path.Direction.CW)
        }
        val ltrRegion = android.graphics.Region().apply {
            setPath(ltrPath, android.graphics.Region(0, 0, 200, 100))
        }
        assertFalse("LTR: Top-left corner (2,2) must be outside clipPath due to 40dp radius", ltrRegion.contains(2, 2))
        assertTrue("LTR: Top-right corner (198,2) must be inside clipPath due to 0dp radius", ltrRegion.contains(198, 2))

        // In RTL: topStart is topRight (80px), topEnd is topLeft (0px)
        val rtlOutline = asymmetricShape.createOutline(Size(200f, 100f), LayoutDirection.Rtl, density) as Outline.Rounded
        assertEquals(0f, rtlOutline.roundRect.topLeftCornerRadius.x)
        assertEquals(80f, rtlOutline.roundRect.topRightCornerRadius.x)

        val rtlPath = android.graphics.Path().apply {
            val rr = rtlOutline.roundRect
            val rArray = floatArrayOf(
                rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y,
                rr.topRightCornerRadius.x, rr.topRightCornerRadius.y,
                rr.bottomRightCornerRadius.x, rr.bottomRightCornerRadius.y,
                rr.bottomLeftCornerRadius.x, rr.bottomLeftCornerRadius.y,
            )
            addRoundRect(rr.left, rr.top, rr.right, rr.bottom, rArray, android.graphics.Path.Direction.CW)
        }
        val rtlRegion = android.graphics.Region().apply {
            setPath(rtlPath, android.graphics.Region(0, 0, 200, 100))
        }
        assertTrue("RTL: Top-left corner (2,2) must be inside clipPath due to 0dp topEnd", rtlRegion.contains(2, 2))
        assertFalse("RTL: Top-right corner (198,2) must be outside clipPath due to 40dp topStart", rtlRegion.contains(198, 2))
    }

    @Test
    fun sourceMappingRefreshesOnViewTranslation() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var testGlassViewRef: LiquidGlassView? = null
        var activityRef: Activity? = null

        scenario.onActivity { activity ->
            activityRef = activity
            val root = activity.findViewById<ViewGroup>(R.id.xmlRoot)

            val backdrop = GlassBackdrop(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 800)
            }
            val redStripe = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 400).apply {
                    topMargin = 0
                }
                setBackgroundColor(AndroidColor.RED)
            }
            val blueStripe = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 400).apply {
                    topMargin = 400
                }
                setBackgroundColor(AndroidColor.BLUE)
            }
            backdrop.addView(redStripe)
            backdrop.addView(blueStripe)
            root.addView(backdrop)

            val glassView = LiquidGlassView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(300, 200).apply {
                    leftMargin = 100
                    topMargin = 100
                }
                this.backdropView = backdrop
                preset = GlassXmlPreset.Clear
            }
            root.addView(glassView)
            testGlassViewRef = glassView
        }

        val activity = activityRef!!
        val glassView = testGlassViewRef!!

        waitForDraw(activity.window.decorView)
        val beforeBitmap = captureWindow(activity)
        assertNotNull(beforeBitmap)

        val location = IntArray(2)
        scenario.onActivity {
            glassView.getLocationInWindow(location)
        }
        val origCenterX = location[0] + glassView.width / 2
        val origCenterY = location[1] + glassView.height / 2
        val pixelBefore = beforeBitmap.getPixel(origCenterX, origCenterY)

        // Glass is over RED stripe (0xFFFF0000)
        assertTrue(
            "Before translation, glass over RED stripe must sample high red (was R=${AndroidColor.red(pixelBefore)}, B=${AndroidColor.blue(pixelBefore)})",
            AndroidColor.red(pixelBefore) > 150 && AndroidColor.blue(pixelBefore) < 80,
        )

        // Finding 13: Translation causes transformMatrix change detected by preDrawListener
        // Translate down by 350px so glassView is now completely over the BLUE stripe (y in 450..650)
        scenario.onActivity {
            glassView.translationY = 350f
        }
        waitForDraw(activity.window.decorView)

        val afterBitmap = captureWindow(activity)
        assertNotNull(afterBitmap)

        val newCenterY = origCenterY + 350
        val pixelAtOldCenter = afterBitmap.getPixel(origCenterX, origCenterY)
        val pixelAtNewCenter = afterBitmap.getPixel(origCenterX, newCenterY)

        // Old center now reveals the red stripe underneath without glass
        assertTrue("Old center must remain red stripe", AndroidColor.red(pixelAtOldCenter) > 200)

        // New center now samples the blue stripe underneath through translated glass mapping
        assertTrue(
            "After translation, glass over BLUE stripe must sample high blue (was R=${AndroidColor.red(pixelAtNewCenter)}, B=${AndroidColor.blue(pixelAtNewCenter)})",
            AndroidColor.blue(pixelAtNewCenter) > 150 && AndroidColor.red(pixelAtNewCenter) < 80,
        )

        safeClose(scenario)
    }

    @Test
    fun composeHostSourceMappingRefreshesOnViewTranslation() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var testHostRef: LiquidGlassHostView? = null
        var activityRef: Activity? = null

        scenario.onActivity { activity ->
            activityRef = activity
            val root = activity.findViewById<ViewGroup>(R.id.xmlRoot)

            val backdrop = GlassBackdrop(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 800)
            }
            val redStripe = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 400).apply {
                    topMargin = 0
                }
                setBackgroundColor(AndroidColor.RED)
            }
            val blueStripe = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(600, 400).apply {
                    topMargin = 400
                }
                setBackgroundColor(AndroidColor.BLUE)
            }
            backdrop.addView(redStripe)
            backdrop.addView(blueStripe)
            root.addView(backdrop)

            val hostView = LiquidGlassHostView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(300, 200).apply {
                    leftMargin = 100
                    topMargin = 100
                }
                this.backdropView = backdrop
            }
            hostView.setThemedGlassContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glassEffect(glass = Glass.Clear, shape = RoundedCornerShape(12.dp)),
                )
            }
            root.addView(hostView)
            testHostRef = hostView
        }

        val activity = activityRef!!
        val hostView = testHostRef!!

        waitForDraw(activity.window.decorView)
        val beforeBitmap = captureWindow(activity)
        assertNotNull(beforeBitmap)

        val location = IntArray(2)
        scenario.onActivity {
            hostView.getLocationInWindow(location)
        }
        val origCenterX = location[0] + hostView.width / 2
        val origCenterY = location[1] + hostView.height / 2
        val pixelBefore = beforeBitmap.getPixel(origCenterX, origCenterY)

        assertTrue(
            "Before translation, compose host over RED stripe must sample high red (was R=${AndroidColor.red(pixelBefore)}, B=${AndroidColor.blue(pixelBefore)})",
            AndroidColor.red(pixelBefore) > 150 && AndroidColor.blue(pixelBefore) < 80,
        )

        // Translate host down by 350px so hostView is now completely over the BLUE stripe (y in 450..650)
        scenario.onActivity {
            hostView.translationY = 350f
        }
        waitForDraw(activity.window.decorView)

        val afterBitmap = captureWindow(activity)
        assertNotNull(afterBitmap)

        val newCenterY = origCenterY + 350
        val pixelAtOldCenter = afterBitmap.getPixel(origCenterX, origCenterY)
        val pixelAtNewCenter = afterBitmap.getPixel(origCenterX, newCenterY)

        assertTrue("Old center must remain red stripe", AndroidColor.red(pixelAtOldCenter) > 200)
        assertTrue(
            "After translation, compose host over BLUE stripe must sample high blue (was R=${AndroidColor.red(pixelAtNewCenter)}, B=${AndroidColor.blue(pixelAtNewCenter)})",
            AndroidColor.blue(pixelAtNewCenter) > 150 && AndroidColor.red(pixelAtNewCenter) < 80,
        )

        safeClose(scenario)
    }

    @Test
    fun effectDisposalRestoresOriginalBackground() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val testView = View(activity)
            val originalBg = android.graphics.drawable.ColorDrawable(android.graphics.Color.RED)
            testView.background = originalBg

            val effect = testView.glassEffect(backdrop)
            assertEquals(effect.drawable, testView.background)

            effect.dispose()
            assertEquals(originalBg, testView.background)
        }
        safeClose(scenario)
    }

    @Test
    fun backdropRejectsNestedConsumerInsideWrapperViewGroup() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val wrapper = FrameLayout(activity)
            val child = View(activity)

            backdrop.registerGlassConsumer(child)
            wrapper.addView(child)

            var rejected = false
            try {
                backdrop.addView(wrapper)
            } catch (e: IllegalStateException) {
                rejected = true
            }
            assertTrue("GlassBackdrop must reject wrapper containing a registered consumer", rejected)
            assertEquals("Rejected wrapper must be removed from backdrop", -1, backdrop.indexOfChild(wrapper))
        }
        safeClose(scenario)
    }

    @Test
    fun backdropAllowsConsumerOfDifferentBackdrop() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop1 = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val backdrop2 = GlassBackdrop(activity)
            val foreignConsumer = View(activity)
            val effect = foreignConsumer.glassEffect(backdrop2)

            val wrapper = FrameLayout(activity)
            wrapper.addView(foreignConsumer)

            // Must NOT throw because foreignConsumer targets backdrop2, not backdrop1
            backdrop1.addView(wrapper)
            assertTrue(backdrop1.indexOfChild(wrapper) != -1)

            backdrop1.removeView(wrapper)
            effect.dispose()
        }
        safeClose(scenario)
    }

    @Test
    fun effectDisposalPreservesCustomPadding() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val testView = View(activity)
            testView.setPadding(12, 14, 16, 18)

            val effect = testView.glassEffect(backdrop)
            assertEquals(12, testView.paddingLeft)
            assertEquals(14, testView.paddingTop)
            assertEquals(16, testView.paddingRight)
            assertEquals(18, testView.paddingBottom)

            effect.dispose()
            assertEquals(12, testView.paddingLeft)
            assertEquals(14, testView.paddingTop)
            assertEquals(16, testView.paddingRight)
            assertEquals(18, testView.paddingBottom)
        }
        safeClose(scenario)
    }

    @Test
    fun backdropRejectsCrossBackdropDependencyCycle() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdropA = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val backdropB = GlassBackdrop(activity)

            // Consumer B targets backdrop B, placed in backdrop A
            val consumerB = View(activity)
            consumerB.glassEffect(backdropB)
            backdropA.addView(consumerB)

            // Attempting to place consumer A (targeting backdrop A) into backdrop B must create a cycle and be rejected
            val consumerA = View(activity)
            consumerA.glassEffect(backdropA)

            var cycleRejected = false
            try {
                backdropB.addView(consumerA)
            } catch (e: IllegalStateException) {
                cycleRejected = true
            }
            assertTrue("Cross-backdrop dependency cycle A <-> B must be rejected", cycleRejected)
            assertEquals("Rejected cyclical consumer must be removed from backdropB", -1, backdropB.indexOfChild(consumerA))

            backdropA.removeView(consumerB)
        }
        safeClose(scenario)
    }

    @Test
    fun effectPreservesRelativePaddingInRtl() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val testView = View(activity)
            testView.layoutDirection = View.LAYOUT_DIRECTION_RTL
            testView.setPaddingRelative(20, 10, 40, 30)

            val effect = testView.glassEffect(backdrop)
            assertEquals(20, testView.paddingStart)
            assertEquals(40, testView.paddingEnd)

            effect.dispose()
            assertEquals(20, testView.paddingStart)
            assertEquals(40, testView.paddingEnd)
        }
        safeClose(scenario)
    }
    @Test
    fun backdropRejectsCrossBackdropDependencyCycleOnBindingPath() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        var decorView: View? = null
        scenario.onActivity { activity ->
            decorView = activity.window.decorView
            val backdropA = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
            val backdropB = GlassBackdrop(activity).apply { id = View.generateViewId() }
            val backdropC = GlassBackdrop(activity).apply { id = View.generateViewId() }
            contentRoot.addView(backdropB)
            contentRoot.addView(backdropC)

            // Consumer B targets backdrop B, placed in backdrop A (so A depends on B)
            val consumerB = View(activity)
            consumerB.glassEffect(backdropB)
            backdropA.addView(consumerB)

            // View is already inside backdrop B
            val consumerA = View(activity)
            backdropB.addView(consumerA)

            // Binding consumerA to backdrop A creates cycle (B -> A -> B) and must throw
            var cycleRejected = false
            try {
                consumerA.glassEffect(backdropA)
            } catch (e: IllegalStateException) {
                cycleRejected = true
            }
            assertTrue("Binding glassEffect that creates cycle with ancestor backdrop must be rejected", cycleRejected)

            // Also test LiquidGlassHostView inside backdrop B targeting backdrop A
            val hostA = LiquidGlassHostView(activity)
            hostA.backdropView = backdropC
            backdropB.addView(hostA)

            // 1. Failed retarget via backdropView must throw and restore previous resolved/explicit backdrop
            var hostCycleRejected = false
            try {
                hostA.backdropView = backdropA
            } catch (e: IllegalStateException) {
                hostCycleRejected = true
            }
            assertTrue("Setting backdropView on host that creates cycle with ancestor backdrop must be rejected", hostCycleRejected)
            assertEquals("Failed retarget via backdropView must restore previous backdrop", backdropC, hostA.backdropView)

            // 2. Failed retarget via backdropViewId must throw and restore previous backdrop
            hostA.backdropViewId = backdropC.id
            hostA.backdropView = null
            assertEquals(backdropC, hostA.backdropView)

            var idCycleRejected = false
            try {
                hostA.backdropViewId = backdropA.id
            } catch (e: IllegalStateException) {
                idCycleRejected = true
            }
            assertTrue("Setting backdropViewId on host that creates cycle with ancestor backdrop must be rejected", idCycleRejected)
            assertEquals("Failed retarget via backdropViewId must restore previous id", backdropC.id, hostA.backdropViewId)
            assertEquals("Failed retarget via backdropViewId must restore previous resolved backdrop", backdropC, hostA.backdropView)

            backdropB.removeView(hostA)
            backdropB.removeView(consumerA)
            backdropA.removeView(consumerB)
            contentRoot.removeView(backdropB)
            contentRoot.removeView(backdropC)
        }

        // Verify the next attached hardware frame renders cleanly after failed retargets
        waitForDraw(decorView!!)
        safeClose(scenario)
    }

    @Test
    fun effectPreservesAbsolutePaddingWhenSwitchingToRtl() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val testView = View(activity)
            // Set asymmetric absolute padding: left=10, top=20, right=30, bottom=40
            testView.setPadding(10, 20, 30, 40)
            assertFalse("Initial padding should not be relative", testView.isPaddingRelative)

            val effect = testView.glassEffect(backdrop)
            assertFalse("isPaddingRelative must remain false after effect installation", testView.isPaddingRelative)
            assertEquals(10, testView.paddingLeft)
            assertEquals(30, testView.paddingRight)

            // Switch to RTL: physical left and right padding must NOT swap
            testView.layoutDirection = View.LAYOUT_DIRECTION_RTL
            assertEquals("paddingLeft must remain 10 in RTL for absolute padding", 10, testView.paddingLeft)
            assertEquals("paddingRight must remain 30 in RTL for absolute padding", 30, testView.paddingRight)

            effect.dispose()
            assertFalse("isPaddingRelative must remain false after disposal", testView.isPaddingRelative)
            assertEquals("paddingLeft must remain 10 after disposal", 10, testView.paddingLeft)
            assertEquals("paddingRight must remain 30 after disposal", 30, testView.paddingRight)
        }
        safeClose(scenario)
    }

    @Test
    fun backdropRejectsSelfAsConsumer() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val origBg = backdrop.background
            var rejected = false
            try {
                backdrop.glassEffect(backdrop)
            } catch (e: IllegalStateException) {
                rejected = true
                assertTrue(e.message?.contains("cannot be its own consumer") == true)
            }
            assertTrue("Self-targeting backdrop.glassEffect(backdrop) must be rejected", rejected)
            assertEquals("Backdrop background must remain intact upon rejection", origBg, backdrop.background)
            assertNull("Backdrop effect tag must remain null upon rejection", backdrop.getTag(LibR.id.liquid_glass_view_effect_tag))
        }
        safeClose(scenario)
    }

    @Test
    fun interactiveDisposalAndNonInteractiveRebindClearsTouchListener() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val view = View(activity)

            val effect1 = view.glassEffect(backdrop, glass = Glass.Regular.interactive(true))
            assertNotNull("Interactive effect must have touchListener installed", effect1.touchListener)

            effect1.dispose()
            assertNull("TouchListener must be null after dispose", effect1.touchListener)

            val effect2 = view.glassEffect(backdrop, glass = Glass.Regular.interactive(false))
            assertNull("Non-interactive rebind must not install touchListener", effect2.touchListener)
            effect2.dispose()
        }
        safeClose(scenario)
    }

    @Test
    fun consumerRebindingAcrossBackdropsAndMovingUnderPreviousBackdrop() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
            val backdropA = GlassBackdrop(activity)
            val backdropB = GlassBackdrop(activity)
            contentRoot.addView(backdropA)
            contentRoot.addView(backdropB)

            val consumer = LiquidGlassView(activity)
            consumer.backdropView = backdropA
            contentRoot.addView(consumer)

            assertTrue("consumer must be registered in backdropA", backdropA.isRegisteredConsumer(consumer))

            consumer.backdropView = backdropB
            assertFalse("consumer must be unregistered from backdropA after rebind", backdropA.isRegisteredConsumer(consumer))
            assertTrue("consumer must be registered in backdropB after rebind", backdropB.isRegisteredConsumer(consumer))

            contentRoot.removeView(consumer)
            backdropA.addView(consumer)
            assertEquals("consumer must be cleanly added under backdropA", backdropA, consumer.parent)

            backdropA.removeView(consumer)
            contentRoot.removeView(backdropA)
            contentRoot.removeView(backdropB)
        }
        safeClose(scenario)
    }

    @Test
    fun genericOutlineCustomShapeBuildsPathField() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val triangleShape = object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    return Outline.Generic(path)
                }
            }
            val view = View(activity).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
            }
            val effect = view.glassEffect(
                backdrop = backdrop,
                glass = Glass.Regular,
                shape = triangleShape,
            )
            val renderer = effect.renderer
            val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            renderer.draw(canvas, 200, 200)
            assertNotNull(bmp)
            effect.dispose()
        }
        safeClose(scenario)
    }

    @Test
    fun interactiveViewProcessesDownMoveUpCancelGestures() {
        val scenario = ActivityScenario.launch(XmlInteropActivity::class.java)
        scenario.onActivity { activity ->
            val backdrop = activity.findViewById<GlassBackdrop>(R.id.xmlBackdrop)
            val view = LiquidGlassView(activity).apply {
                isGlassInteractive = true
                backdropView = backdrop
            }
            activity.findViewById<ViewGroup>(android.R.id.content).addView(view)

            val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
            view.dispatchTouchEvent(downEvent)
            assertEquals(1f, view.renderer?.touchAmount ?: 0f, 0.01f)

            val moveEvent = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_MOVE, 80f, 90f, 0)
            view.dispatchTouchEvent(moveEvent)
            assertEquals(80f, view.renderer?.touchX ?: 0f, 0.01f)
            assertEquals(90f, view.renderer?.touchY ?: 0f, 0.01f)

            val upEvent = MotionEvent.obtain(0L, 20L, MotionEvent.ACTION_UP, 80f, 90f, 0)
            view.dispatchTouchEvent(upEvent)
            assertEquals(0f, view.renderer?.touchAmount ?: 0f, 0.01f)

            val cancelEvent = MotionEvent.obtain(0L, 30L, MotionEvent.ACTION_CANCEL, 80f, 90f, 0)
            view.dispatchTouchEvent(cancelEvent)
            assertEquals(0f, view.renderer?.touchAmount ?: 0f, 0.01f)

            activity.findViewById<ViewGroup>(android.R.id.content).removeView(view)
        }
        safeClose(scenario)
    }
}
