package com.wexpa.liquidglass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.wexpa.liquidglass.*
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val Ground = Color(0xFF06080F)
private val Ink = Color(0xFFF2F5FA)
private val Muted = Color(0xFFA7B1C2)
private val Accent = Color(0xFF8FD3FF)
private val Scenes = listOf("Sky", "Type", "Lines")
private val BarHeight = 62.dp
// A magnifier being dragged keeps its view: no press glow (it hazes the interior) and no press
// scale (it resamples it). Authored for the loupe only.
private val LensDrag = GlassInteraction(pressScale = 1f, illumination = 0f, gel = false, pressLift = 0f, pull = false)

/** Normalized canvas coordinates: independent of sidebars, DPI and window resizing. */
internal fun boundedLens(position: Offset, size: IntSize, radius: Float, margin: Float): Offset {
    fun axis(value: Float, extent: Int): Float {
        val inset = (radius + margin).coerceAtMost(extent / 2f)
        return value.coerceIn(inset, (extent - inset).coerceAtLeast(inset))
    }
    return Offset(axis(position.x, size.width), axis(position.y, size.height))
}

/**
 * Atlas Studio, shared by Android and the desktop: one full-bleed scene whose content keeps
 * moving under every piece of glass (a navigation bar fused with a round button, a draggable
 * magnifier, a top control bar and a card). The navigation bar is the only scene switcher.
 * The scenes are illustrative, not instruments. [animateBackdrop] = false freezes the drift
 * (tests, screenshots); Reduce Motion freezes it too. Optional diagnostics report untransformed
 * navigation bounds in root pixels and actual scene-selection callbacks; they do not control the
 * library's input, material or animation.
 */
@Composable
fun LoupeScreen(reduceMotion: Boolean = false, modifier: Modifier = Modifier,
                loupeStyle: GlassStyle = GlassStyle.clearLens(1.4f).copy(specular = .55f, bevel = 2.dp, contactShadow = .35f), initialScene: Int = 0,
                onNavigationLayout: ((Rect) -> Unit)? = null,
                onSceneObserved: ((Int) -> Unit)? = null,
                animateBackdrop: Boolean = true) {
    var scene by remember { mutableIntStateOf(initialScene.coerceIn(0, 2)) }
    var motion by remember { mutableStateOf(!reduceMotion) }
    var magnification by remember { mutableFloatStateOf(1.4f) }
    var lensVisible by remember { mutableStateOf(true) }
    val selectScene: (Int) -> Unit = { scene = it; onSceneObserved?.invoke(it) }
    val moving = motion && !reduceMotion
    // Honor later host accessibility changes; the in-app toggle can always reduce further.
    val interaction = if (moving) GlassInteraction.Calm else GlassInteraction.ReducedMotion
    // Seconds of drift. Read only while drawing, so the moving backdrop never recomposes.
    val clock = remember { mutableFloatStateOf(0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    if (moving && animateBackdrop) LaunchedEffect(Unit) {
        var last = -1L
        while (true) withFrameNanos { now ->
            if (last >= 0) clock.floatValue += ((now - last) / 1e9f).coerceAtMost(.1f)
            last = now
        }
    }
    val density = LocalDensity.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var normalized by remember { mutableStateOf(Offset(.5f, .42f)) }
    val diameter = with(density) { minOf(200.dp.toPx(), (minOf(viewport.width, viewport.height) * .42f).coerceAtLeast(0f)) }
    val margin = with(density) { 12.dp.toPx() }
    val centre = boundedLens(Offset(normalized.x * viewport.width, normalized.y * viewport.height), viewport, diameter / 2, margin)
    val lensPress = rememberGlassPressSource()
    val barStyle = GlassTabBarStyle.Calm().copy(height = BarHeight)

    GlassScene(background = Ground, modifier = modifier.fillMaxSize().testTag("atlas-canvas").onSizeChanged { viewport = it },
        backdrop = {
            AtlasBackdrop(scene, clock, pan, Modifier.fillMaxSize().pointerInput(Unit) {
                // Drag the page itself: content moves under the glass by hand as well as by drift.
                detectDragGestures { change, amount -> change.consume(); pan += amount }
            })
        }) {
        val glassState = state
        val wide = with(density) { viewport.width.toDp() } >= 720.dp
        val compact = with(density) { viewport.width.toDp() } < 480.dp

        // Top: title and controls, each on its own glass.
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.liquidGlass(glassState, RoundedCornerShape(26.dp), GlassStyle.DarkChrome, interaction = null)
                .heightIn(min = 52.dp).padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Label("ATLAS", 15, weight = FontWeight.Bold)
                if (!compact) Label("  Studio · LiquidGlass", 13, Muted)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.liquidGlass(glassState, RoundedCornerShape(26.dp), GlassStyle.DarkChrome, interaction = null)
                .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Action("−", "zoom-out", onClick = { magnification = (magnification - .1f).coerceAtLeast(1f) })
                Label("${(magnification * 100).roundToInt()}%", 13, Accent, FontWeight.Medium)
                Action("+", "zoom-in", onClick = { magnification = (magnification + .1f).coerceAtMost(1.6f) })
                if (!compact) Action(if (moving) "Motion on" else "Motion reduced", "motion", selected = !moving,
                    onClick = { motion = !motion })
            }
        }
        if (compact) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 76.dp, end = 16.dp)
            .liquidGlass(glassState, RoundedCornerShape(22.dp), GlassStyle.DarkChrome, interaction = null)) {
            Action(if (moving) "Motion on" else "Motion reduced", "motion", selected = !moving, onClick = { motion = !motion })
        }

        if (wide) Column(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(24.dp).widthIn(max = 360.dp)
            .liquidGlass(glassState, RoundedCornerShape(24.dp), GlassStyle.DarkChrome, interaction = interaction)
            .testTag("glass-note").padding(20.dp)) {
            Label(listOf("Find your perspective.", "Clarity, up close.", "Follow the curve.")[scene], 18, weight = FontWeight.Medium)
            Label("Hold and pull this card: its surface answers,\nits words and its place stay put.", 12, Ink.copy(alpha = .78f),
                modifier = Modifier.padding(top = 6.dp))
        }

        if (lensVisible && diameter > 0f) {
            Box(Modifier.offset { IntOffset((centre.x - diameter / 2).roundToInt(), (centre.y - diameter / 2).roundToInt()) }
                .size(with(density) { diameter.toDp() }).testTag("atlas-lens")
                .semantics { contentDescription = "Magnifier, ${(magnification * 100).roundToInt()} percent" }
                .liquidGlass(glassState, CircleShape, loupeStyle.copy(heldMagnification = 1f - 1f / magnification),
                    interaction = LensDrag, pressSource = lensPress)
                .pointerInput(viewport, diameter) {
                    detectDragGestures(onDragStart = { lensPress.press(it) }, onDragEnd = lensPress::release,
                        onDragCancel = lensPress::release) { change, amount ->
                        change.consume()
                        // Read state on each event; no remembered screen-space offsets.
                        val current = Offset(normalized.x * viewport.width, normalized.y * viewport.height)
                        val next = boundedLens(current + amount, viewport, diameter / 2, margin)
                        normalized = Offset(next.x / viewport.width.coerceAtLeast(1), next.y / viewport.height.coerceAtLeast(1))
                        lensPress.press(Offset(diameter / 2, diameter / 2))
                    }
                })
        }

        // The one scene switcher: Calm navigation fused with a round lens button, as iOS 26's
        // Phone bar is with its search button.
        val gap = 8.dp
        val maxBar = with(density) { viewport.width.toDp() } - 32.dp - BarHeight - gap
        val barWidth = minOf(300.dp, maxBar).coerceAtLeast(180.dp)
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GlassTabBar(glassState, Scenes.size, scene, selectScene,
                modifier = Modifier.width(barWidth).testTag("atlas-navigation").onGloballyPositioned { onNavigationLayout?.invoke(it.boundsInRoot()) },
                style = barStyle, motionEnabled = moving,
                trailingAccessory = GlassTabBarAccessory(BarHeight, gap = gap)) { index, selected ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(listOf("✦", "Aa", "▦")[index], 18, if (selected) Accent else Ink)
                    Label(Scenes[index], 10, if (selected) Accent else Muted, FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(gap))
            Box(Modifier.size(BarHeight).testTag("lens-toggle")
                .liquidGlass(glassState, CircleShape, barStyle.bar, light = barStyle.light)
                .clip(CircleShape).clickable(role = Role.Switch) { lensVisible = !lensVisible }
                .semantics { contentDescription = if (lensVisible) "Hide magnifier" else "Show magnifier" },
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(24.dp)) {
                    val r = size.minDimension * .34f
                    val c = Offset(size.width * .44f, size.height * .44f)
                    val tint = if (lensVisible) Accent else Ink
                    drawCircle(tint, r, c, style = Stroke(2.2.dp.toPx()))
                    drawLine(tint, c + Offset(r * .72f, r * .72f), Offset(size.width * .9f, size.height * .9f), 2.6.dp.toPx())
                }
            }
        }
    }
}

@Composable private fun AtlasBackdrop(scene: Int, clock: State<Float>, pan: Offset, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val stars = remember { val random = Random(271); List(720) { Triple(random.nextFloat(), random.nextFloat(), random.nextFloat()) } }
    Canvas(modifier) {
        val t = clock.value
        when (scene) {
            0 -> sky(t, pan, stars, measurer)
            1 -> type(t, pan, measurer)
            else -> lines(t, pan)
        }
    }
}

private fun wrap(value: Float, extent: Float): Float = value - floor(value / extent) * extent

/** A night sky: aurora ribbons, drifting stars, a constellation and a moon. */
private fun DrawScope.sky(t: Float, pan: Offset, stars: List<Triple<Float, Float, Float>>, measurer: TextMeasurer) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF050817), Color(0xFF0E0B2A), Color(0xFF1A0C2C))))
    val w = size.width; val h = size.height
    val ribbons = listOf(Color(0xFF19C6A6), Color(0xFF7B5CFF), Color(0xFFE0479E))
    ribbons.forEachIndexed { i, colour ->
        val phase = t * (.05f + i * .02f) + i * 2.1f
        val centre = Offset(w * (.3f + .25f * i + .12f * sin(phase)) + pan.x * .3f, h * (.35f + .1f * cos(phase * 1.3f)) + pan.y * .3f)
        drawCircle(Brush.radialGradient(listOf(colour.copy(alpha = .55f), Color.Transparent), centre, w * .32f), w * .32f, centre)
    }
    val drift = t * 6.dp.toPx()
    stars.forEach { (x, y, z) ->
        val p = Offset(wrap(x * w - drift * (.4f + z) + pan.x * (.3f + z * .7f), w), wrap(y * h + pan.y * (.3f + z * .7f), h))
        if (z > .975f) drawCircle(Brush.radialGradient(listOf(Color(0x66BFE7FF), Color.Transparent), p, 14.dp.toPx()), 14.dp.toPx(), p)
        drawCircle(Ink.copy(alpha = .25f + z * .7f), (.4f + z * 1.1f).dp.toPx(), p)
    }
    val moon = Offset(w * .78f + pan.x * .6f, h * .3f + pan.y * .6f)
    val mr = minOf(w, h) * .13f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4D6), Color(0xFFE8C98F), Color(0xFF9C7A4A)), moon - Offset(mr * .3f, mr * .3f), mr * 1.4f), mr, moon)
    drawCircle(Color(0x33000000), mr * .18f, moon + Offset(mr * .25f, mr * .1f))
    drawCircle(Color(0x26000000), mr * .11f, moon + Offset(-mr * .35f, mr * .35f))
    val points = listOf(Offset(.2f, .3f), Offset(.34f, .44f), Offset(.46f, .36f), Offset(.55f, .58f), Offset(.68f, .52f))
        .map { Offset(wrap(it.x * w - drift * .9f + pan.x, w), it.y * h + pan.y) }
    points.zipWithNext { a, b -> if ((a - b).getDistance() < w * .4f) drawLine(Accent.copy(alpha = .5f), a, b, 1.2.dp.toPx()) }
    points.forEachIndexed { i, p ->
        drawCircle(Ink, 2.5.dp.toPx(), p)
        drawCircle(Accent.copy(alpha = .35f), 7.dp.toPx(), p, style = Stroke(1.dp.toPx()))
        drawText(measurer, listOf("NORTH", "ALTAIR", "VEGA", "DENEB", "EAST")[i], p + Offset(12.dp.toPx(), 4.dp.toPx()),
            TextStyle(color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp))
    }
}

private val Copy = listOf(
    "Light" to Color(0xFFFFFFFF), "A little light changes everything." to Color(0xFFFFC27A),
    "Look closer; there is more to see." to Color(0xFF8FD3FF), "Glass" to Color(0xFFFF8FC8),
    "Edges bend what lies beneath them." to Color(0xFFE8ECF4), "Clarity, up close." to Color(0xFFA4F0C4),
    "Depth" to Color(0xFFC6B6FF), "Every surface samples the scene behind it." to Color(0xFFFFE08A),
)

/** An editorial page that keeps scrolling upward under the glass. */
private fun DrawScope.type(t: Float, pan: Offset, measurer: TextMeasurer) {
    drawRect(Brush.linearGradient(listOf(Color(0xFF0B0D16), Color(0xFF141026)), Offset.Zero, Offset(size.width, size.height)))
    val line = 64.dp.toPx()
    val scroll = t * 22.dp.toPx() - pan.y
    val left = maxOf(24.dp.toPx(), size.width * .5f - 300.dp.toPx()) + pan.x
    var row = floor((scroll - line) / line).toInt()
    while (true) {
        val y = row * line - scroll
        if (y > size.height) break
        val (text, colour) = Copy[((row % Copy.size) + Copy.size) % Copy.size]
        val headline = text.length < 8
        if (headline) drawRect(colour.copy(alpha = .16f), Offset(0f, y + 6.dp.toPx()), Size(size.width, line - 12.dp.toPx()))
        drawText(measurer, if (headline) text.uppercase() else text, Offset(left, y + if (headline) 2.dp.toPx() else 14.dp.toPx()),
            TextStyle(color = colour, fontSize = if (headline) 44.sp else 26.sp,
                fontWeight = if (headline) FontWeight.Black else FontWeight.Normal, letterSpacing = if (headline) 6.sp else 0.sp))
        row++
    }
}

/** Saturated diagonal stripes over a fine grid, sliding sideways. */
private fun DrawScope.lines(t: Float, pan: Offset) {
    drawRect(Color(0xFF05060B))
    val gap = 18.dp.toPx()
    val shift = wrap(t * 24.dp.toPx() + pan.x, gap * 12f)
    val span = size.width + size.height
    var i = -12
    while (i * gap < span + gap * 12) {
        val x = i * gap + shift
        val hue = ((i % 12 + 12) % 12) / 12f
        val colour = Color.hsv(hue * 360f, .75f, 1f)
        drawLine(colour.copy(alpha = .85f), Offset(x, 0f), Offset(x - size.height, size.height), 3.dp.toPx())
        i++
    }
    val g = 24.dp.toPx()
    clipRect {
        var y = wrap(pan.y, g)
        while (y < size.height) { drawLine(Color(0x33FFFFFF), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += g }
    }
    val c = Offset(size.width * .5f + pan.x * .5f, size.height * .45f + pan.y * .5f)
    for (k in 1..7) drawCircle(Color.White.copy(alpha = .5f - k * .05f), k * 26.dp.toPx() + 6.dp.toPx() * sin(t * 1.2f + k), c,
        style = Stroke(2.dp.toPx()))
}

@Composable private fun Action(text: String, tag: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.testTag(tag).heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(22.dp))
        .background(if (selected) Color(0x332FA6FF) else Color.Transparent)
        .clickable(role = Role.Button, onClick = onClick).semantics {
            this.selected = selected
            if (tag == "zoom-in") contentDescription = "Increase magnification"
            if (tag == "zoom-out") contentDescription = "Decrease magnification"
        }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Label(text, 14, if (selected) Accent else Ink)
    }
}

@Composable private fun Label(text: String, size: Int, color: Color = Ink, weight: FontWeight = FontWeight.Normal,
                              modifier: Modifier = Modifier) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, lineHeight = (size * 1.4f).sp))
}
