package com.wexpa.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A tuning surface for the material.
 *
 * The material can only be judged against content worth refracting. Over a near-black app the
 * lensing has nothing to bend and every style looks the same; over hard edges and saturated
 * colour the differences between the styles are obvious, and so are the mistakes. This exists
 * to be screenshotted and compared, and doubles as the library's sample.
 */
@Composable
fun GlassGallery(modifier: Modifier = Modifier) {
    // Everything, backdrop and glass, scrolls together; the glass follows its place on every frame.
    LiquidGlassTheme(dark = false, background = Color(0xFF1B2A6B)) {
        Box(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GalleryBackdrop(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                GlassPanel("Clear · capsule", Glass.Clear, CircleShape, 64.dp)
                GlassPanel("Regular · card", Glass(GlassStyle.Regular), RoundedCornerShape(28.dp), 96.dp)
                GlassPanel("Thick · sheet", Glass(GlassStyle.Thick), RoundedCornerShape(32.dp), 150.dp)

                // Apple's actual corner geometry, with the material's field matching the clip.
                GlassPanel("Squircle · n=4", Glass(GlassStyle.Regular),
                    GlassSquircleShape(cornerRadius = 34.dp, power = 4f), 110.dp)

                // No closed form: the host measures this one and the shader reads the measurement.
                GlassPanel("Path · star", Glass(GlassStyle.Regular), StarShape, 190.dp)

                Label("Press it - scale, glow, magnifier", size = 13)
                GlassPanel("Interactive", Glass(GlassStyle.Regular).interactive(response = GlassInteraction.Default),
                    RoundedCornerShape(30.dp), 92.dp)

                Spacer(Modifier.height(4.dp))
                Label("Container - members fusing", size = 13)
                GlassEffectContainer(Modifier.fillMaxWidth().height(110.dp), spacing = 40.dp, glass = Glass(GlassStyle.Regular)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(96.dp).glassEffect(shape = CircleShape))
                        Box(Modifier.size(96.dp).glassEffect(shape = CircleShape))
                        Box(Modifier.width(120.dp).height(84.dp).glassEffect(shape = RoundedCornerShape(28.dp)))
                    }
                }

                Spacer(Modifier.height(4.dp))
                Label("Reduce Transparency · Increase Contrast", size = 13)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassTheme(dark = false, reduceTransparency = 1f) {
                        GlassPanel("Frosted", Glass(GlassStyle.Regular), RoundedCornerShape(24.dp), 84.dp, Modifier.weight(1f))
                    }
                    LiquidGlassTheme(dark = false, increaseContrast = 1f) {
                        GlassPanel("Contrast", Glass(GlassStyle.Regular), RoundedCornerShape(24.dp), 84.dp, Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun GlassPanel(
    label: String,
    glass: Glass,
    shape: Shape,
    height: Dp,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Box(
        modifier = modifier
            .height(height)
            .glassEffect(glass, shape),
        contentAlignment = Alignment.Center,
    ) {
        Label(label)
    }
}

@Composable
private fun Label(text: String, size: Int = 15) {
    BasicText(
        text = text,
        style = TextStyle(color = Color.White, fontSize = size.sp, fontWeight = FontWeight.Medium),
    )
}

/** A five-pointed star: no closed-form distance field, so it exercises the measured path. */
private object StarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = minOf(cx, cy) * 0.96f
        val inner = outer * 0.44f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = (-PI / 2f + i * PI / 5f).toFloat()
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Saturated colour, hard edges and fine text — the three things that make refraction visible.
 * A smooth gradient alone hides almost every flaw in a lensing shader.
 */
@Composable
private fun GalleryBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1B2A6B), Color(0xFFB3245F), Color(0xFFE86A2C),
                    Color(0xFFF2C744), Color(0xFF128A6B), Color(0xFF2A1B6B),
                ),
                start = Offset.Zero,
                end = Offset(1400f, 2600f),
            )
        )
    ) {
        // Hard stripes: any displacement at the rim shows up as a visible kink in a straight line.
        Column(Modifier.fillMaxSize()) {
            repeat(48) { i ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (i % 2 == 0) 46.dp else 40.dp)
                        .background(if (i % 2 == 0) Color(0x33000000) else Color(0x22FFFFFF))
                )
            }
        }
        Row(Modifier.fillMaxSize()) {
            repeat(9) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (i % 2 == 0) Color(0x1AFFFFFF) else Color.Transparent)
                )
            }
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            repeat(48) {
                BasicText(
                    text = "REFRACTION TEST 0123456789 — fine text shows displacement",
                    style = TextStyle(color = Color.White, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
