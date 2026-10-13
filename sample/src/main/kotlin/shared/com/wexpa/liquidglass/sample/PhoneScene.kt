package com.wexpa.liquidglass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wexpa.liquidglass.*

/**
 * Colours for [PhoneScene].
 *
 * [Recorded] matches the owner's iOS reference recordings pixel for pixel in their own,
 * dimmer, brightness space (white labels read about 133 there); the navigation material was
 * calibrated in that space, so side-by-side comparisons use it. [Natural] is the same scene in
 * ordinary display colours for demonstrations.
 */
data class PhonePalette(
    val page: Color, val primary: Color, val secondary: Color, val divider: Color,
    val avatar: Color, val button: Color, val accent: Color, val ink: Color, val call: Color,
) {
    companion object {
        val Recorded = PhonePalette(
            page = Color.Black, primary = Color(0xFF858585), secondary = Color(0xFF646464),
            divider = Color(0xFF323233), avatar = Color(0xFF43404C), button = Color(0xFF222224),
            accent = Color(0xFF5A6A8C), ink = Color(0xFF808080), call = Color(0xFF4F7A4F),
        )
        val Natural = PhonePalette(
            page = Color.Black, primary = Color.White, secondary = Color(0xFF98989F),
            divider = Color(0xFF38383A), avatar = Color(0xFF7D7A91), button = Color(0xFF2C2C2E),
            accent = Color(0xFF0A84FF), ink = Color(0xFFEBEBF5), call = Color(0xFF30D158),
        )
    }
}

/** Geometry of the reference: a 390pt-wide phone, measured from the recordings at 3px/pt. */
object PhoneGeometry {
    val width = 390.dp
    val height = 234.dp
    val barLeft = 21.dp
    val barWidth = 278.dp
    val barHeight = 62.dp
    val barCentreY = 182.dp
    val searchLeft = 307.dp
}

private val Names = listOf("Alex Morgan", "Jordan Lee", "Sam Rivera", "Taylor Kim", "Riley Chen", "Casey Park")

/**
 * A phone-sized navigation scene matching the geometry of the owner's iOS references: a
 * call list (or contacts, or keypad) on black, a three-tab Calm navigation bar and a separate
 * search button. The content is neutral placeholder text, never the private page content.
 */
@Composable
fun PhoneScene(
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    palette: PhonePalette = PhonePalette.Recorded,
    style: GlassTabBarStyle = GlassTabBarStyle.Calm().copy(height = PhoneGeometry.barHeight),
    scroll: Float = 0f,
) {
    val state = rememberLiquidGlassState(palette.page)
    Box(modifier.size(PhoneGeometry.width, PhoneGeometry.height).background(palette.page)) {
        Box(Modifier.fillMaxSize().liquidGlassSource(state)) {
            when (selected) {
                2 -> Keypad(palette)
                else -> CallList(palette, contacts = selected == 1, scroll = scroll)
            }
        }
        val barTop = PhoneGeometry.barCentreY - PhoneGeometry.barHeight / 2
        GlassTabBar(
            state, 3, selected, onSelected,
            Modifier.offset(PhoneGeometry.barLeft, barTop).width(PhoneGeometry.barWidth).testTag("phone-bar"),
            style = style,
            // The search button is separate glass the bar fuses with while pressed.
            trailingAccessory = GlassTabBarAccessory(PhoneGeometry.barHeight,
                gap = PhoneGeometry.searchLeft - PhoneGeometry.barLeft - PhoneGeometry.barWidth),
        ) { index, isSelected -> PhoneTab(index, if (isSelected) palette.accent else palette.ink) }
        Box(
            Modifier.offset(PhoneGeometry.searchLeft, barTop).size(PhoneGeometry.barHeight)
                .liquidGlass(state, CircleShape, style.bar, light = style.light),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(26.dp)) { searchGlyph(palette.ink) }
        }
    }
}

@Composable
private fun PhoneTab(index: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(23.dp)) {
            when (index) {
                0 -> clockGlyph(color)
                1 -> personGlyph(color)
                else -> keypadGlyph(color)
            }
        }
        Spacer(Modifier.height(3.dp))
        BasicText(
            listOf("Calls", "Contacts", "Keypad")[index],
            style = TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun CallList(palette: PhonePalette, contacts: Boolean, scroll: Float) {
    Column(Modifier.fillMaxSize().offset(y = (-scroll).dp).padding(top = 10.dp)) {
        repeat(Names.size) { i ->
            Row(Modifier.fillMaxWidth().height(75.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(45.dp).background(palette.avatar, CircleShape), contentAlignment = Alignment.Center) {
                    BasicText(Names[i].split(" ").joinToString("") { it.take(1) },
                        style = TextStyle(color = palette.primary, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(Names[i], style = TextStyle(color = palette.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                    if (!contacts) BasicText(if (i % 2 == 0) "↗ Audio" else "↙ Audio",
                        style = TextStyle(color = palette.secondary, fontSize = 14.sp))
                }
                if (!contacts) {
                    BasicText("Yesterday", style = TextStyle(color = palette.secondary, fontSize = 14.sp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(40.dp).background(palette.button, CircleShape), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(16.dp)) { drawCircle(palette.accent, size.minDimension / 3f) }
                    }
                }
            }
            Box(Modifier.padding(start = 75.dp).fillMaxWidth().height(1.dp).background(palette.divider))
        }
    }
}

@Composable
private fun Keypad(palette: PhonePalette) {
    // The bottom of the dial pad as the reference shows it: the last digit row cut off at the
    // top, then the call button, centred, above the navigation bar.
    Box(Modifier.fillMaxSize()) {
        listOf(93.dp, 195.dp, 297.dp).forEach { x ->
            Box(Modifier.offset(x - 39.dp, (-71).dp).size(78.dp).background(palette.button, CircleShape))
        }
        Box(Modifier.offset(156.dp, 26.dp).size(78.dp).background(palette.call, CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) { drawCircle(palette.ink, size.minDimension / 3f) }
        }
    }
}

private fun DrawScope.clockGlyph(color: Color) {
    val r = size.minDimension / 2
    drawCircle(color, r)
    val c = center
    drawLine(Color.Black.copy(alpha = .55f), c, Offset(c.x, c.y - r * .55f), r * .16f, StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = .55f), c, Offset(c.x + r * .4f, c.y), r * .16f, StrokeCap.Round)
}

private fun DrawScope.personGlyph(color: Color) {
    val r = size.minDimension / 2
    drawCircle(color, r)
    drawCircle(Color.Black.copy(alpha = .55f), r * .3f, Offset(center.x, center.y - r * .2f))
    drawArc(Color.Black.copy(alpha = .55f), 200f, 140f, false,
        Offset(center.x - r * .6f, center.y + r * .25f), Size(r * 1.2f, r * 1.1f), style = Stroke(r * .22f))
}

private fun DrawScope.keypadGlyph(color: Color) {
    val step = size.minDimension / 3
    for (row in 0..2) for (col in 0..2) drawCircle(color, step * .42f, Offset(step * (col + .5f), step * (row + .5f)))
}

private fun DrawScope.searchGlyph(color: Color) {
    val r = size.minDimension * .32f
    val c = Offset(size.width * .42f, size.height * .42f)
    drawCircle(color, r, c, style = Stroke(size.minDimension * .1f))
    drawLine(color, Offset(c.x + r * .72f, c.y + r * .72f), Offset(size.width * .92f, size.height * .92f),
        size.minDimension * .11f, StrokeCap.Round)
}
