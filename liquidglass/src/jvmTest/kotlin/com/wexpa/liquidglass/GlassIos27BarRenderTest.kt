package com.wexpa.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * [GlassTabBarStyle.Dark] and the measured [GlassTabBarStyle.Ios27] over the same light stripes, for
 * the tab bar guide. Over a light page, iOS 27's contour outside the outline is what shows: the white
 * just beyond the bar's rounded ends darkens under Ios27 and not under Dark.
 */
@OptIn(ExperimentalTestApi::class)
class GlassIos27BarRenderTest {
    private fun shoot(name: String, style: GlassTabBarStyle) = run {
        var image: java.awt.image.BufferedImage? = null
        runComposeUiTest(testTimeout = 4.minutes) {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    val state = rememberLiquidGlassState(Color.White)
                    Box(Modifier.size(360.dp, 120.dp).testTag("scene")) {
                        Row(Modifier.fillMaxSize().liquidGlassSource(state)) {
                            repeat(18) { i ->
                                Box(Modifier.width(20.dp).fillMaxHeight()
                                    .background(if (i % 2 == 0) Color.White else Color(0xFFE35D6A)))
                            }
                        }
                        GlassTabBar(state, 3, 0, {}, Modifier.align(Alignment.Center).width(300.dp), style = style) { _, _ ->
                            Box(Modifier.size(24.dp).background(Color(0xFFDDE6F0), CircleShape))
                        }
                    }
                }
            }
            waitForIdle()
            image = onNodeWithTag("scene").captureToImage().toAwtImage()
        }
        val out = File("build/reports/atlas/ios27-bar").apply { mkdirs() }
        ImageIO.write(image!!, "png", File(out, "$name.png"))
        image!!
    }

    @Test fun ios27DarkensTheWhiteJustOutsideItsEnds() {
        val dark = shoot("bar-dark", GlassTabBarStyle.Dark)
        val ios27 = shoot("bar-ios27", GlassTabBarStyle.Ios27)
        // The page just outside the bar's left end, on its centre line (the bar is 300 dp wide,
        // centred in 360 dp, so its left end is at 30 dp = 60 px; one pixel outside, at x 59).
        fun luma(img: java.awt.image.BufferedImage, x: Int, y: Int): Int {
            val p = img.getRGB(x, y)
            return ((p shr 16 and 0xFF) * 2126 + (p shr 8 and 0xFF) * 7152 + (p and 0xFF) * 722) / 10000
        }
        val y = dark.height / 2
        val outsideDark = (56..59).minOf { luma(dark, it, y) }
        val outsideIos27 = (56..59).minOf { luma(ios27, it, y) }
        println("GLASS_IOS27_BAR outside the left end: Dark=$outsideDark Ios27=$outsideIos27")
        assertTrue(outsideIos27 < outsideDark - 20, "Ios27's contour must darken the page beyond the bar's end")
    }
}
