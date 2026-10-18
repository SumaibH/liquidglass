package com.wexpa.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * The public components under [LiquidGlassTheme] (Phase 2, 18 October): [GlassTabBar] composes in the
 * theme's overlay through a portal, laid out where it is called, and takes its own touches; a nested
 * theme changes the accessibility settings for its subtree over the same recording.
 */
@OptIn(ExperimentalTestApi::class)
class GlassThemeComponentsTest {
    private val out = File("build/reports/atlas/glass-effect").apply { mkdirs() }

    @Test fun theTabBarDrawsWhereItIsLaidOutAndTakesItsOwnTaps() = runComposeUiTest(testTimeout = 4.minutes) {
        var selected by mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LiquidGlassTheme(dark = true, background = Color(0xFF808080)) {
                    Box(Modifier.size(400.dp, 200.dp).background(Color(0xFF808080)).testTag("scene")) {
                        GlassTabBar(3, selected, { selected = it },
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).width(300.dp).testTag("bar")) { _, isSelected ->
                            Box(Modifier.size(18.dp).background(if (isSelected) Color.Blue else Color.White))
                        }
                    }
                }
            }
        }
        waitForIdle()
        // The placeholder carries the caller's modifiers: aligned, padded and 300 wide.
        val bounds = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        assertEquals(50f, bounds.left, 1f); assertEquals(350f, bounds.right, 1f)
        assertEquals(180f, bounds.bottom, 1f)
        val img = onNodeWithTag("scene").captureToImage().toAwtImage()
        ImageIO.write(img, "png", File(out, "tabbar-portal.png"))
        // The bar is drawn there (it changes the grey page), and nowhere above it.
        val y = ((bounds.top + bounds.bottom) / 2).toInt()
        val onBar = (img.getRGB(200, y) shr 8) and 0xFF
        val above = (img.getRGB(200, 30) shr 8) and 0xFF
        println("GLASS_TABBAR bounds=$bounds onBar=$onBar above=$above")
        assertTrue(abs(onBar - 128) > 4, "no bar drawn at its place ($onBar)")
        assertTrue(abs(above - 128) <= 2, "the bar drew away from its place ($above)")
        // A tap on the third tab selects it: the overlay bar takes its own touches.
        val tab = onNodeWithTag(GlassTabBarSemantics.TAG, useUnmergedTree = true)
        tab.performTouchInput { val w = width / 3f; down(Offset(w * 2.5f, height / 2f)); advanceEventTime(60); up() }
        waitForIdle()
        assertEquals(2, selected, "the tap did not reach the bar")
    }

    @Test fun aNestedThemeChangesAccessibilityForItsSubtree() {
        fun level(reduce: Float): Int {
            lateinit var image: java.awt.image.BufferedImage
            runComposeUiTest(testTimeout = 4.minutes) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        LiquidGlassTheme(dark = true, background = Color.Black) {
                            Box(Modifier.size(200.dp, 120.dp).background(Color(0xFF808080)).testTag("scene")) {
                                LiquidGlassTheme(dark = true, reduceTransparency = reduce) {
                                    Box(Modifier.offset(40.dp, 20.dp).size(120.dp, 80.dp)
                                        .glassEffect(Glass.Regular, RoundedCornerShape(20.dp)))
                                }
                            }
                        }
                    }
                }
                waitForIdle()
                image = onNodeWithTag("scene").captureToImage().toAwtImage()
            }
            ImageIO.write(image, "png", File(out, "nested-reduce-$reduce.png"))
            return (image.getRGB(100, 60) shr 8) and 0xFF
        }
        val clear = level(0f)
        val frosted = level(1f)
        println("GLASS_NESTED reduceTransparency 0 -> $clear, 1 -> $frosted")
        assertTrue(abs(clear - frosted) > 4, "a nested reduceTransparency changed nothing ($clear vs $frosted)")
    }
}
