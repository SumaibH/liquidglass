@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassOverlay
import com.wexpa.liquidglass.glassEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** The receiver of a glass [TooltipBox]'s `tooltip`, where [PlainTooltip] and [RichTooltip] live. */
interface TooltipScope

private object GlassTooltipScope : TooltipScope

/**
 * Material 3's `TooltipBox` with a glass tooltip. The tooltip draws in the app's window
 * ([GlassOverlay]) rather than in a popup, so its glass refracts the screen behind it. It is placed
 * by Material's [positionProvider] (for example `TooltipDefaults.rememberPlainTooltipPositionProvider()`).
 * A long press (touch) or hovering (mouse) on [content] shows it, and Material's [TooltipState]
 * hides it again. Pointer input is observed, never consumed, so [content] keeps its clicks.
 */
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val longPress = LocalViewConfiguration.current.longPressTimeoutMillis
    var anchor by remember { mutableStateOf(IntRect.Zero) }
    val input = if (!enableUserInput) Modifier else Modifier
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.type == PointerType.Mouse) return@awaitEachGesture
                // Shown once the finger has been down for the long-press timeout, or, if the up
                // arrives first but late (a batched gesture), on the up.
                var shown = false
                val timer = scope.launch { delay(longPress); shown = true; state.show() }
                val up = waitForUpOrCancellation(PointerEventPass.Initial)
                if (!shown) {
                    timer.cancel()
                    if (up != null && up.uptimeMillis - down.uptimeMillis >= longPress) scope.launch { state.show() }
                }
            }
        }
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.type != PointerType.Mouse }) continue
                    when (event.type) {
                        PointerEventType.Enter -> scope.launch { state.show() }
                        PointerEventType.Exit -> if (!state.isPersistent) {
                            state.dismiss()
                            onDismissRequest?.invoke()
                        }
                    }
                }
            }
        }
    Box(
        modifier
            .onGloballyPositioned { c ->
                val r = c.boundsInRoot()
                anchor = IntRect(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt())
            }
            .then(input),
        propagateMinConstraints = true,
    ) { content() }
    DisposableEffect(state) { onDispose { state.onDispose() } }
    if (!state.isVisible) return
    val direction = LocalLayoutDirection.current
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    var window by remember { mutableStateOf(IntSize.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    GlassOverlay {
        Box(Modifier.fillMaxSize().onGloballyPositioned {
            val p = it.positionInRoot()
            origin = IntOffset(p.x.roundToInt(), p.y.roundToInt())
            window = it.size
        })
        Box(
            Modifier
                .offset {
                    // Kept inside the window, as a popup is.
                    val at = positionProvider.calculatePosition(anchor.translate(-origin.x, -origin.y), window, direction, size)
                    IntOffset(at.x.coerceIn(0, maxOf(0, window.width - size.width)), at.y.coerceIn(0, maxOf(0, window.height - size.height)))
                }
                .onSizeChanged { size = it },
        ) { GlassTooltipScope.tooltip() }
    }
}

/**
 * Material 3's `PlainTooltip` as a small glass capsule. Material's default is an inverse-surface
 * block; this keeps the app's ink on frosted glass.
 */
@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 200.dp,
    shape: Shape = CapsuleShape,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    glass: Glass = if (containerColor == Color.Unspecified) GlassMaterialDefaults.panel else GlassMaterialDefaults.forContainer(containerColor),
    content: @Composable () -> Unit,
) = Box(modifier.widthIn(max = maxWidth).glassEffect(glass, shape).padding(horizontal = 12.dp, vertical = 6.dp)) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.bodySmall) { content() }
    }
}

/** Material 3's `RichTooltip` as a glass panel: an optional [title], the [text] and an optional [action]. */
@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    maxWidth: Dp = 320.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    text: @Composable () -> Unit,
) = Column(modifier.widthIn(max = maxWidth).glassEffect(glass, shape).padding(16.dp)) {
    OnGlassPanel {
        title?.let {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall) { Box(Modifier.padding(bottom = 4.dp)) { it() } }
            }
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
        }
        action?.let { Box(Modifier.padding(top = 8.dp)) { it() } }
    }
}
