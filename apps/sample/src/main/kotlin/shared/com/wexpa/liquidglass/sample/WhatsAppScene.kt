package com.wexpa.liquidglass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wexpa.liquidglass.*

/**
 * Geometry of WhatsApp's iOS 26 five-tab bar in dark appearance, measured on the owner's recording
 * (JGEY2190 50-74 s, display colours, 3 px/pt, 2026-10-17): the bar 348 x 62 pt on the Phone bar's
 * row, tab centres 193/370/584.5/799/976 px, the resting pill 78 x 54 pt, 4 pt inside the bar. The
 * tabs are not equal shares of the bar: the inner spacing is 71.5 pt (the widest item, 63.3 pt, plus
 * 8 pt) and the end tabs close up to where their pill fits, which [GlassTabBarStyle.slotWidth] is.
 */
object WhatsAppGeometry {
    val barLeft = 21.dp
    val barWidth = 348.dp
    val slotWidth = 71.5.dp
    val pillWidth = 78.dp
    val pillInset = 4.dp

    /** [base] with this scene's bar height, slots and resting pill. */
    fun style(base: GlassTabBarStyle): GlassTabBarStyle = base.copy(
        height = PhoneGeometry.barHeight, slotWidth = slotWidth, pillWidth = pillWidth, pillInset = pillInset,
    )
}

private val WhatsAppLabels = listOf("Updates", "Calls", "Communities", "Chats", "You")

/** Icon extents on the recording, in pt (the profile disc is 31 pt). */
private val WhatsAppIconWidths = listOf(25.dp, 22.dp, 32.dp, 32.dp, 31.dp)

/**
 * The WhatsApp reference scene: a settings list on a near-black page behind a five-tab Calm bar
 * on [WhatsAppGeometry]'s slots. Selected ink stays white over the dark pill, as on the recording;
 * a selected tab's glyph fills. [card] false leaves the bare page behind the bar. Neutral placeholders: the "You" tab is a plain disc, not the
 * recording's profile photo, and no page content of the recording is reproduced.
 */
@Composable
fun WhatsAppScene(
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: GlassTabBarStyle = WhatsAppGeometry.style(GlassTabBarStyle.CalmClear()),
    card: Boolean = true,
    // Light appearance, ASSUMED colours (grouped background, white card, black ink).
    light: Boolean = false,
) {
    val page = if (light) Color(0xFFF2F2F7) else Color(0xFF0A0A0A)
    val ink = if (light) Color.Black else Color.White
    LiquidGlassTheme(dark = !light, background = page) {
    Box(modifier.size(PhoneGeometry.width, PhoneGeometry.height).background(page)) {
        Box(Modifier.fillMaxSize()) {
            // A grouped list card, x 48-1122 px (grey 23 on 10), rows on a 50 pt pitch with an
            // outlined glyph at 37 pt and a 17 pt label at 77 pt, one row behind the bar. Without
            // [card], the bare page (10), as under the bar on WhatsApp's chats list.
            if (card) Box(Modifier.offset(16.dp, 0.dp).size(358.dp, 260.dp)
                .background(if (light) Color.White else Color(0xFF171717), RoundedCornerShape(26.dp)))
            if (card) for ((i, label) in listOf("Account", "Storage", "Privacy", "Chats", "Appearance").withIndex()) {
                val y = (28f + 50f * i).dp
                Canvas(Modifier.offset(37.dp, y - 10.dp).size(20.dp)) {
                    drawRoundRect(ink, Offset(size.width * .1f, size.height * .1f),
                        Size(size.width * .8f, size.height * .8f), CornerRadius(size.width * .22f),
                        style = Stroke(size.width * .09f))
                }
                BasicText(label, Modifier.offset(77.dp, y - 11.dp),
                    style = TextStyle(color = ink, fontSize = 17.sp))
            }
        }
        val barTop = PhoneGeometry.barCentreY - PhoneGeometry.barHeight / 2
        GlassTabBar(
            5, selected, onSelected,
            Modifier.offset(WhatsAppGeometry.barLeft, barTop).width(WhatsAppGeometry.barWidth).testTag("whatsapp-bar"),
            style = style,
        ) { index, isSelected -> WhatsAppTab(index, isSelected, ink) }
    }
    }
}

@Composable
private fun WhatsAppTab(index: Int, selected: Boolean, ink: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(WhatsAppIconWidths[index], 22.dp)) {
            val fill: DrawStyle = if (selected) Fill else Stroke(2.2.dp.toPx())
            when (index) {
                0 -> updatesGlyph(fill, ink)
                1 -> callsGlyph(fill, ink)
                2 -> communitiesGlyph(fill, ink)
                3 -> chatsGlyph(fill, ink)
                else -> profileGlyph(selected, ink)
            }
        }
        Spacer(Modifier.height(5.dp))
        BasicText(WhatsAppLabels[index], style = TextStyle(color = ink, fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, letterSpacing = 0.2.sp))
    }
}

// Glyphs of the recording's extents, drawn plainly: ink of the right size and weight under the glass.
private fun DrawScope.updatesGlyph(fill: DrawStyle, ink: Color = Color.White) {
    val r = size.minDimension / 2f
    drawCircle(ink, r * .92f, center, style = Stroke(r * .14f))
    drawCircle(ink, r * .58f, center, style = fill)
}

private fun DrawScope.callsGlyph(fill: DrawStyle, ink: Color = Color.White) {
    val w = size.width; val h = size.height
    drawPath(Path().apply {
        moveTo(w * .12f, h * .08f); lineTo(w * .34f, h * .04f); lineTo(w * .44f, h * .3f); lineTo(w * .32f, h * .4f)
        cubicTo(w * .4f, h * .58f, w * .5f, h * .68f, w * .62f, h * .74f)
        lineTo(w * .72f, h * .6f); lineTo(w * .96f, h * .7f); lineTo(w * .92f, h * .94f)
        cubicTo(w * .5f, h * 1f, w * .02f, h * .56f, w * .12f, h * .08f); close()
    }, ink, style = fill)
}

private fun DrawScope.communitiesGlyph(fill: DrawStyle, ink: Color = Color.White) {
    val w = size.width; val h = size.height
    for ((cx, r, top) in listOf(Triple(.5f, .17f, .02f), Triple(.2f, .13f, .14f), Triple(.8f, .13f, .14f))) {
        drawCircle(ink, h * r, Offset(w * cx, h * (top + r)), style = fill)
        val bw = if (cx == .5f) w * .4f else w * .3f
        val by = h * (top + r * 2f + .12f)
        drawArc(ink, 180f, 180f, true, Offset(w * cx - bw / 2f, by),
            Size(bw, (h - by) * 2f - h * .04f), style = fill)
    }
}

private fun DrawScope.chatsGlyph(fill: DrawStyle, ink: Color = Color.White) {
    val w = size.width; val h = size.height
    drawOval(ink, Offset(w * .36f, h * .12f), Size(w * .62f, h * .7f), style = fill)
    drawOval(ink, Offset(w * .02f, h * .02f), Size(w * .62f, h * .74f), style = fill)
    drawPath(Path().apply { moveTo(w * .12f, h * .62f); lineTo(w * .08f, h * .98f); lineTo(w * .3f, h * .74f); close() },
        ink, style = fill)
}

private fun DrawScope.profileGlyph(selected: Boolean, ink: Color = Color.White) {
    val r = size.height / 2f + 4.5.dp.toPx()
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(Color(0xFF5E5E5E), r * (if (selected) .84f else 1f), c)
    drawCircle(Color(0xFF9A9A9A), r * .3f, Offset(c.x, c.y - r * .18f))
    drawArc(Color(0xFF9A9A9A), 200f, 140f, true, Rect(c.x - r * .55f, c.y + r * .2f, c.x + r * .55f, c.y + r * 1.1f).topLeft,
        Size(r * 1.1f, r * .9f))
    if (selected) drawCircle(ink, r, c, style = Stroke(r * .1f))
}
