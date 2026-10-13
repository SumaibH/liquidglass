package com.wexpa.liquidglass.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class AtlasDesktopTest {
    @Test fun boundsRemainValidAtEdgesAndInTinyWindows() {
        for (viewport in listOf(IntSize(1000, 700), IntSize(320, 600), IntSize(40, 20))) {
            for (point in listOf(Offset(-1e6f, -1e6f), Offset(1e6f, 1e6f), Offset(-1e6f, 1e6f))) {
                val bounded = boundedLens(point, viewport, 104f, 10f)
                assertTrue(bounded.x in 0f..viewport.width.toFloat())
                assertTrue(bounded.y in 0f..viewport.height.toFloat())
            }
        }
        assertEquals(Offset(114f, 114f), boundedLens(Offset.Zero, IntSize(1000, 700), 104f, 10f))
    }

    @Test fun actualAtlasSceneSwitchingAndControlsRecover() = runComposeUiTest(testTimeout = 12.minutes) {
        val report = File("build/reports/atlas").apply { mkdirs() }
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                // Reduced motion: switching is checked without rendering every transition frame on the
                // CPU test raster (an animated tap costs minutes there); motion is covered by the replay.
                // 640 x 420 keeps the top bar, navigation and magnifier; the wide-only card is covered by the replay.
                Box(Modifier.size(640.dp, 420.dp)) { LoupeScreen(reduceMotion = true, animateBackdrop = false) }
            }
        }
        waitForIdle()
        fun capture(name: String) {
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(report, "$name.png"))
        }
        // The navigation bar is the only scene switcher: tap each third of it.
        for (scene in 0..2) {
            onNodeWithTag("atlas-navigation").performTouchInput {
                click(Offset(width * (scene + .5f) / 3f, height / 2f))
            }
            waitForIdle()
            capture("scene-$scene")
        }
        onNodeWithTag("zoom-in").performClick()
        onNodeWithTag("atlas-lens").assertContentDescriptionEquals("Magnifier, 150 percent")
        onNodeWithTag("lens-toggle").performClick()
        onNodeWithTag("atlas-lens").assertDoesNotExist()
        onNodeWithTag("lens-toggle").performClick()
        onNodeWithTag("atlas-lens").assertExists()
        onNodeWithTag("motion").performClick()
        onNodeWithText("Motion reduced").assertExists()
        capture("recovered")
    }
    @Test fun actualAtlasExtremeDragsRemainAnchoredAndRecover() = runComposeUiTest(testTimeout = 12.minutes) {
        val report = File("build/reports/atlas").apply { mkdirs() }
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                // Small and without elastic motion: every frame is shaded on the CPU test raster.
                Box(Modifier.size(640.dp, 420.dp)) { LoupeScreen(initialScene = 2, reduceMotion = true, animateBackdrop = false) }
            }
        }
        waitForIdle()
        fun capture(name: String) {
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(report, "$name.png"))
        }
        val canvas = onNodeWithTag("atlas-canvas").fetchSemanticsNode().boundsInRoot
        val lens = onNodeWithTag("atlas-lens")
        lens.performTouchInput { down(center) }
        for ((direction, point) in listOf("right" to Offset(3000f, 0f), "left" to Offset(-3000f, 0f),
            "top" to Offset(0f, -3000f), "bottom" to Offset(0f, 3000f), "diagonal" to Offset(3000f, 3000f))) {
            lens.performTouchInput { moveTo(point, 150) }
            waitForIdle()
            val bounds = lens.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= canvas.left && bounds.right <= canvas.right, "$direction horizontal escape")
            assertTrue(bounds.top >= canvas.top && bounds.bottom <= canvas.bottom, "$direction vertical escape")
        }
        lens.performTouchInput { up() }
        waitForIdle()
        val recovered = lens.fetchSemanticsNode().boundsInRoot
        assertTrue(recovered.left >= canvas.left && recovered.right <= canvas.right)
        assertTrue(recovered.top >= canvas.top && recovered.bottom <= canvas.bottom)
        capture("extreme-recovered")
    }

}
