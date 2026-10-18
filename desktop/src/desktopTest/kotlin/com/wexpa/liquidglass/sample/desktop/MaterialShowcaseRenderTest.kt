package com.wexpa.liquidglass.sample.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.sample.MaterialShowcase
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The shared Material 3 showcase (the screen the Android sample, the desktop app and the web demo run),
 * rendered at a phone's size for the README and the docs: 390 x 844 dp at density 3.
 */
@OptIn(ExperimentalTestApi::class)
class MaterialShowcaseRenderTest {
    private val out = File("build/reports/atlas/showcase").apply { mkdirs() }

    private fun shoot(name: String, dark: Boolean) = runDesktopComposeUiTest(1170, 2532, testTimeout = 6.minutes) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f)) {
                Box(Modifier.size(390.dp, 844.dp).testTag("app")) { MaterialShowcase(onSwitchScreen = {}, startDark = dark) }
            }
        }
        waitForIdle()
        ImageIO.write(onNodeWithTag("app").captureToImage().toAwtImage(), "png", File(out, "$name.png"))
    }

    @Test fun light() = shoot("showcase-light", dark = false)
    @Test fun dark() = shoot("showcase-dark", dark = true)
}
