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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wexpa.liquidglass.*

/**
 * Geometry of iOS 26's App Store five-tab bar in dark appearance, measured on the owner's recording
 * (LOLL8185 at 37.5 s, display colours, 3 px/pt, 2026-10-15): the bar 348 x 62 pt on the Phone bar's
 * row, five 64 pt slots with 13.7 pt end padding (tab centres 200/393/585/777/969 px), the resting pill
 * 83.7 x 52.7 pt, 4 pt inside the bar.
 */
object AppStoreGeometry {
    val barLeft = 21.dp
    val barWidth = 348.dp
    val itemInset = 13.67.dp
    val pillWidth = 83.7.dp

    /** [base] with this scene's bar height, end padding and resting pill width. */
    fun style(base: GlassTabBarStyle): GlassTabBarStyle =
        base.copy(height = PhoneGeometry.barHeight, contentPadding = itemInset, pillWidth = pillWidth)
}

private val AppStoreLabels = listOf("Today", "Games", "Apps", "Arcade", "Search")

/**
 * The App Store reference scene: a dark editorial card behind a five-tab Calm bar, the card's grey
 * header and white headline running under the bar as on the recording (a card 28/28/30 on black,
 * display colours). Neutral placeholder text; no content of the recording is reproduced.
 */
@Composable
fun AppStoreScene(
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: GlassTabBarStyle = AppStoreGeometry.style(GlassTabBarStyle.Calm()),
    accent: Color = Color(0xFF369CFE),
) {
    val state = rememberLiquidGlassState(Color.Black)
    Box(modifier.size(PhoneGeometry.width, PhoneGeometry.height).background(Color.Black)) {
        Box(Modifier.fillMaxSize().liquidGlassSource(state)) {
            // The card: x 57-1112 px, its header at rows ~430-455 and a two-line headline behind the bar.
            Box(Modifier.offset(19.dp, 120.dp).size(352.dp, 200.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(18.dp)))
            BasicText("CARD HEADER", Modifier.offset(38.dp, 139.dp),
                style = TextStyle(color = Color(0xFF969696), fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
            BasicText("Daily new games", Modifier.offset(38.dp, 165.dp),
                style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold))
            BasicText("for every mood", Modifier.offset(38.dp, 210.dp),
                style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold))
        }
        val barTop = PhoneGeometry.barCentreY - PhoneGeometry.barHeight / 2
        GlassTabBar(
            state, 5, selected, onSelected,
            Modifier.offset(AppStoreGeometry.barLeft, barTop).width(AppStoreGeometry.barWidth).testTag("appstore-bar"),
            style = style,
        ) { index, isSelected -> AppStoreTab(index, if (isSelected) accent else Color.White) }
    }
}

@Composable
private fun AppStoreTab(index: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(26.dp)) {
            when (index) {
                0 -> todayGlyph(color)
                1 -> gamesGlyph(color)
                2 -> appsGlyph(color)
                3 -> arcadeGlyph(color)
                else -> magnifierGlyph(color)
            }
        }
        Spacer(Modifier.height(3.dp))
        BasicText(AppStoreLabels[index],
            style = TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp))
    }
}

// Glyphs of the reference's extents (about 22-24 pt), drawn plainly: what matters here is ink of the
// right size and weight under the glass, not the symbols themselves.
private fun DrawScope.todayGlyph(c: Color) {
    val w = size.width; val h = size.height
    drawRoundRect(c, Offset(w * .18f, h * .05f), Size(w * .64f, h * .9f), CornerRadius(w * .12f), style = Stroke(w * .09f))
    drawRect(c, Offset(w * .29f, h * .5f), Size(w * .42f, h * .34f))
    drawLine(c, Offset(w * .31f, h * .19f), Offset(w * .6f, h * .19f), w * .07f, StrokeCap.Round)
    drawLine(c, Offset(w * .31f, h * .33f), Offset(w * .69f, h * .33f), w * .07f, StrokeCap.Round)
}

private fun DrawScope.gamesGlyph(c: Color) {
    val w = size.width; val h = size.height
    val body = Path().apply {
        moveTo(w * .92f, h * .06f); cubicTo(w * .6f, h * .1f, w * .3f, h * .35f, w * .25f, h * .62f)
        lineTo(w * .4f, h * .76f); cubicTo(w * .66f, h * .7f, w * .9f, h * .4f, w * .92f, h * .06f); close()
    }
    drawPath(body, c)
    drawPath(Path().apply { moveTo(w * .3f, h * .45f); lineTo(w * .08f, h * .5f); lineTo(w * .26f, h * .62f); close() }, c)
    drawPath(Path().apply { moveTo(w * .54f, h * .72f); lineTo(w * .5f, h * .94f); lineTo(w * .38f, h * .76f); close() }, c)
    drawLine(c, Offset(w * .2f, h * .8f), Offset(w * .06f, h * .94f), w * .07f, StrokeCap.Round)
}

private fun DrawScope.appsGlyph(c: Color) {
    val w = size.width; val h = size.height
    fun layer(y: Float) = Path().apply {
        moveTo(w * .5f, y); lineTo(w * .97f, y + h * .19f); lineTo(w * .5f, y + h * .38f); lineTo(w * .03f, y + h * .19f); close()
    }
    drawPath(layer(h * .54f), c); drawPath(layer(h * .3f), c); drawPath(layer(h * .05f), c)
}

private fun DrawScope.arcadeGlyph(c: Color) {
    val w = size.width; val h = size.height
    drawPath(Path().apply {
        moveTo(w * .5f, h * .48f); lineTo(w * 1.03f, h * .72f); lineTo(w * .5f, h * .97f); lineTo(w * -.03f, h * .72f); close()
    }, c)
    drawLine(c, Offset(w * .5f, h * .2f), Offset(w * .5f, h * .6f), w * .1f, StrokeCap.Round)
    drawCircle(c, w * .14f, Offset(w * .5f, h * .15f))
}

private fun DrawScope.magnifierGlyph(c: Color) {
    val r = size.minDimension * .3f
    val o = Offset(size.width * .42f, size.height * .4f)
    drawCircle(c, r, o, style = Stroke(size.minDimension * .1f))
    drawLine(c, Offset(o.x + r * .72f, o.y + r * .72f), Offset(size.width * .9f, size.height * .9f),
        size.minDimension * .11f, StrokeCap.Round)
}
