package com.wexpa.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The contact shadow drawn into a panel's own backdrop (what its rim bends) must sit where the
 * shadow on the page sits, whatever [rememberLiquidGlassState]'s renderScale is. It is recorded in
 * the scaled frame, so drawing it into the scaled backdrop must not scale it a second time.
 */
@OptIn(ExperimentalTestApi::class)
class GlassContactShadowScaleTest {
    // A clear panel over white: no tint, blur, rim light or bevel, so its interior is the backdrop
    // and the in-backdrop shadow is the only thing darkening it.
    private val clear = GlassStyle().copy(tint = Color.Transparent, tintLift = 0f, blurRadius = 0.dp, wideKernel = 0.dp,
        specular = 0f, legibility = 0f, bevel = 0.dp, dispersion = 0f, counterLight = 0f)

    /** Mean darkening (255 - luma) of each quadrant of the panel's interior: TL, TR, BL, BR. */
    private fun quadrants(renderScale: Float, report: File): List<Double> {
        var out = emptyList<Double>()
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    val glass = rememberLiquidGlassState(background = Color.White, renderScale = renderScale)
                    Box(Modifier.size(600.dp, 400.dp).testTag("scene")) {
                        Box(Modifier.fillMaxSize().liquidGlassSource(glass).background(Color.White))
                        Box(Modifier.align(Alignment.Center).size(320.dp, 180.dp)
                            .liquidGlass(glass, RoundedCornerShape(24.dp), clear))
                    }
                }
            }
            waitForIdle()
            val image: BufferedImage = onNodeWithTag("scene").captureToImage().toAwtImage()
            ImageIO.write(image, "png", File(report, "shadow-rs$renderScale.png"))
            // The panel spans x 140-460, y 110-290; keep 40 px clear of the rim.
            fun dark(x0: Int, x1: Int, y0: Int, y1: Int): Double {
                var sum = 0.0; var n = 0
                for (y in y0 until y1) for (x in x0 until x1) {
                    val rgb = image.getRGB(x, y)
                    sum += 255 - (.299 * ((rgb shr 16) and 255) + .587 * ((rgb shr 8) and 255) + .114 * (rgb and 255)); n++
                }
                return sum / n
            }
            out = listOf(dark(180, 300, 150, 200), dark(300, 420, 150, 200), dark(180, 300, 200, 250), dark(300, 420, 200, 250))
        }
        return out
    }

    @Test fun theShadowInTheBackdropDoesNotMoveWithRenderScale() {
        val report = File("build/reports/glass-shadow-scale").apply { mkdirs() }
        val full = quadrants(1f, report)
        val half = quadrants(.5f, report)
        println("contact shadow darkening TL/TR/BL/BR: renderScale 1 $full, 0.5 $half")
        // The shadow is there to be measured: it darkens the panel's lower half at full scale.
        assertTrue(full[2] > 1.0 && full[3] > 1.0, "no contact shadow under the panel: $full")
        // Left and right agree at either scale (a shadow scaled about the corner piles up top-left).
        assertTrue(abs(half[0] - half[1]) < 1.0 && abs(half[2] - half[3]) < 1.0, "shadow lopsided at renderScale 0.5: $half")
        // And half scale draws the same shadow as full scale.
        for (q in 0..3) assertTrue(abs(half[q] - full[q]) < 1.5, "quadrant $q: renderScale 0.5 ${half[q]} against 1.0 ${full[q]}")
    }
}
