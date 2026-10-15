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
import androidx.compose.ui.graphics.Brush
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
 * [Natural] is the iOS Phone page in display colours, measured on the owner's recordings decoded
 * correctly from HDR10 (PQ, BT.2020) to sRGB (research/README.md II.4; docs/2.MP4 at 0.2 s and
 * IMG_6756 at 3 s, 2026-10-15): names white, detail text 145, dividers 42, call buttons 15, keypad
 * keys 32, the avatar a vertical gradient. Comparisons use it.
 *
 * [Recorded] is the earlier palette that matched the recordings decoded as if they were sRGB
 * (white about 130): a decode error, kept only to reproduce passes 1-14.
 */
data class PhonePalette(
    val page: Color, val primary: Color, val secondary: Color, val divider: Color,
    val avatar: Color, val button: Color, val accent: Color, val ink: Color, val call: Color,
    val avatarEnd: Color = avatar, val key: Color = button, val missed: Color = primary,
) {
    companion object {
        val Recorded = PhonePalette(
            page = Color.Black, primary = Color(0xFF858585), secondary = Color(0xFF646464),
            divider = Color(0xFF323233), avatar = Color(0xFF43404C), button = Color(0xFF222224),
            accent = Color(0xFF5A6A8C), ink = Color(0xFF808080), call = Color(0xFF4F7A4F),
        )
        val Natural = PhonePalette(
            page = Color.Black, primary = Color.White, secondary = Color(0xFF919191),
            divider = Color(0xFF2A2A2A), avatar = Color(0xFF514C61), button = Color(0xFF0F0F0F),
            accent = Color(0xFF369CFE), ink = Color.White, call = Color(0xFF177F2F),
            avatarEnd = Color(0xFF342C4A), key = Color(0xFF202020), missed = Color(0xFFF74E53),
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
    /** Measured in video2 at eight resting landings (2026-10-14): the resting inset is 285 px
     * (95 pt) wide and the end insets sit 8 pt from the bar's ends (centres 218/480/742 px). */
    val pillWidth = 95.dp
    val itemInset = 8.dp

    /** [base] with this scene's bar height, end insets and resting pill width. */
    fun style(base: GlassTabBarStyle): GlassTabBarStyle =
        base.copy(height = barHeight, contentPadding = itemInset, pillWidth = pillWidth)
}

// Neutral placeholders. The row behind the bar has a name and subtitle of the reference's length,
// so the glass bends glyph runs of the same extent; no private content is reproduced.
private val Names = listOf("Alex Morgan", "Jordan Lee", "Avery Westbrook", "Taylor Kim", "Riley Chen", "Casey Park")

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
    style: GlassTabBarStyle = PhoneGeometry.style(GlassTabBarStyle.Calm()),
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
                gap = PhoneGeometry.searchLeft - PhoneGeometry.barLeft - PhoneGeometry.barWidth, yieldsToLens = true),
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
            // Measured on the decoded recordings (2026-10-15): iOS's labels are as tall as these
            // but 11% wider (Contacts 137 px against 122, Calls 72 against 64, at 3 px/pt) and
            // lighter; the spacing takes up the width, which the font alone does not.
            style = TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp),
        )
    }
}

@Composable
private fun CallList(palette: PhonePalette, contacts: Boolean, scroll: Float) {
    Column(Modifier.fillMaxSize().offset(y = (-scroll).dp).padding(top = 10.dp)) {
        repeat(Names.size) { i ->
            Row(Modifier.fillMaxWidth().height(75.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(45.dp).background(Brush.verticalGradient(listOf(palette.avatar, palette.avatarEnd)), CircleShape),
                    contentAlignment = Alignment.Center) {
                    BasicText(Names[i].split(" ").joinToString("") { it.take(1) },
                        style = TextStyle(color = palette.primary, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                }
                // Measured in video2 (2026-10-14): names start 70 pt from the left, 17 pt semibold;
                // subtitles and dates 16 pt; a 40 pt dark call button with a handset.
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    // The second row is a missed call, red, as in the reference.
                    BasicText(Names[i], style = TextStyle(color = if (i == 1 && !contacts) palette.missed else palette.primary,
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
                    if (!contacts) BasicText(if (i % 2 == 0) "↗ Network Audio" else "↙ Network Audio",
                        style = TextStyle(color = palette.secondary, fontSize = 16.sp))
                }
                if (!contacts) {
                    BasicText("Yesterday", style = TextStyle(color = palette.secondary, fontSize = 16.sp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(40.dp).background(palette.button, CircleShape), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(18.dp)) { handsetGlyph(palette.accent) }
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
            Box(Modifier.offset(x - 39.dp, (-71).dp).size(78.dp).background(palette.key, CircleShape))
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
    // iOS's Calls clock reads ten to nine-ish: one hand up, one to the left (video2).
    drawLine(Color.Black.copy(alpha = .55f), c, Offset(c.x - r * .4f, c.y), r * .16f, StrokeCap.Round)
}

private fun DrawScope.personGlyph(color: Color) {
    val r = size.minDimension / 2
    drawCircle(color, r)
    // A filled head and a filled shoulder cut by the circle, as iOS's person.crop.circle.fill.
    drawCircle(Color.Black.copy(alpha = .55f), r * .3f, Offset(center.x, center.y - r * .22f))
    drawArc(Color.Black.copy(alpha = .55f), 200f, 140f, true,
        Offset(center.x - r * .62f, center.y + r * .22f), Size(r * 1.24f, r * 1.0f))
}

private fun DrawScope.keypadGlyph(color: Color) {
    // iOS's grid is 75 x 72 px against the 68 x 69 this drew at a 23 dp square (decoded
    // recordings, 2026-10-15): wider spacing across than down, and slightly larger dots.
    val step = size.minDimension / 3
    val r = step * .45f
    for (row in 0..2) for (col in 0..2) drawCircle(color, r,
        Offset(center.x + (col - 1) * step * 1.115f, center.y + (row - 1) * step * 1.05f))
}

private fun DrawScope.handsetGlyph(color: Color) {
    // A telephone handset: two ear/mouth pieces joined by a curved grip, drawn as a thick arc.
    val w = size.minDimension
    drawArc(color, 110f, 140f, false, Offset(w * .08f, w * .08f), Size(w * .84f, w * .84f),
        style = Stroke(w * .2f, cap = StrokeCap.Round))
}

private fun DrawScope.searchGlyph(color: Color) {
    val r = size.minDimension * .32f
    val c = Offset(size.width * .42f, size.height * .42f)
    drawCircle(color, r, c, style = Stroke(size.minDimension * .1f))
    drawLine(color, Offset(c.x + r * .72f, c.y + r * .72f), Offset(size.width * .92f, size.height * .92f),
        size.minDimension * .11f, StrokeCap.Round)
}
