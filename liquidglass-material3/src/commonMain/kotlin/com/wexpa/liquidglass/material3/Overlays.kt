@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassOverlay
import com.wexpa.liquidglass.glassEffect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.DropdownMenuItem as M3DropdownMenuItem

private val Scrim = Color.Black.copy(alpha = .2f)

// ---- Dialogs ---------------------------------------------------------------------------------------

/**
 * Material 3's `AlertDialog` as an iOS 26 alert: a glass panel over a light scrim, drawn in the same
 * window as the app (in [GlassOverlay]) so the glass refracts the screen behind it. The same parameters.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(34.dp),
    glass: Glass = GlassMaterialDefaults.regular,
    iconContentColor: Color = MaterialTheme.colorScheme.secondary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    properties: DialogProperties = DialogProperties(),
) = BasicAlertDialog(onDismissRequest, modifier, properties) {
    Column(Modifier.widthIn(min = 270.dp, max = 400.dp).glassEffect(glass, shape).padding(24.dp)) {
        icon?.let {
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) { it() }
            }
        }
        title?.let {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                    Box(Modifier.align(if (icon != null) Alignment.CenterHorizontally else Alignment.Start).padding(bottom = 16.dp)) { it() }
                }
            }
        }
        text?.let {
            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { Box(Modifier.padding(bottom = 24.dp)) { it() } }
            }
        }
        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dismissButton?.invoke()
            confirmButton()
        }
    }
}

/** Material 3's `BasicAlertDialog`: [content] centred over a light scrim, in the app's window. */
@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) = GlassOverlay {
    Box(Modifier.fillMaxSize().background(Scrim).clickable(interactionSource = remember { MutableInteractionSource() },
        indication = null) { if (properties.dismissOnClickOutside) onDismissRequest() })
    Box(modifier.align(Alignment.Center).padding(24.dp)
        // Swallow taps on the panel so they do not reach the scrim.
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) { content() }
}

// ---- Bottom sheet ----------------------------------------------------------------------------------

/**
 * The state of a glass [ModalBottomSheet], with Material 3's `SheetState` calls: [show], [hide],
 * [isVisible], [currentValue], [targetValue].
 */
@Stable
class SheetState internal constructor(
    internal val skipPartiallyExpanded: Boolean,
    internal val confirmValueChange: (SheetValue) -> Boolean,
) {
    internal val progress = Animatable(0f)
    var targetValue by mutableStateOf(SheetValue.Hidden); private set
    val currentValue: SheetValue get() = if (progress.value <= 0f) SheetValue.Hidden else SheetValue.Expanded
    val isVisible: Boolean get() = progress.value > 0f || targetValue != SheetValue.Hidden

    suspend fun show() {
        if (!confirmValueChange(SheetValue.Expanded)) return
        targetValue = SheetValue.Expanded
        progress.animateTo(1f, spring(dampingRatio = .85f, stiffness = 380f))
    }

    suspend fun hide() {
        if (!confirmValueChange(SheetValue.Hidden)) return
        targetValue = SheetValue.Hidden
        progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f))
    }

    suspend fun expand() = show()
    suspend fun partialExpand() = show()
}

/** A [SheetState] for a glass [ModalBottomSheet], as Material 3's `rememberModalBottomSheetState`. */
@Composable
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
): SheetState = remember { SheetState(skipPartiallyExpanded, confirmValueChange) }

/**
 * Material 3's `ModalBottomSheet` as an iOS 26 sheet: a floating glass panel inset from the screen's
 * edges, over a light scrim, in the app's window. Drag it down or tap the scrim to dismiss.
 */
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = 640.dp,
    shape: Shape = RoundedCornerShape(38.dp),
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrimColor: Color = Scrim,
    dragHandle: @Composable (() -> Unit)? = { DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(sheetState) { sheetState.show() }
    var height by remember { mutableIntStateOf(1) }
    val dismiss: () -> Unit = { scope.launch { sheetState.hide(); onDismissRequest() } }
    val drag = rememberDraggableState { delta ->
        scope.launch { sheetState.progress.snapTo((sheetState.progress.value - delta / height).coerceIn(0f, 1f)) }
    }
    GlassOverlay {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = sheetState.progress.value }.background(scrimColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss))
        Column(
            modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
                .padding(8.dp)
                .onSizeChanged { height = it.height.coerceAtLeast(1) }
                .offset { IntOffset(0, ((1f - sheetState.progress.value) * (height + 64)).roundToInt()) }
                .draggable(drag, Orientation.Vertical, onDragStopped = { velocity ->
                    if (sheetState.progress.value < .7f || velocity > 1200f) dismiss() else sheetState.show()
                })
                .glassEffect(glass, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(bottom = 16.dp),
        ) {
            dragHandle?.let { Box(Modifier.align(Alignment.CenterHorizontally)) { it() } }
            CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
        }
    }
}

/** The sheet's grabber, as iOS draws it. */
@Composable
fun DragHandle(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f)) =
    Box(modifier.padding(vertical = 10.dp).size(36.dp, 5.dp).background(color, CapsuleShape))

// ---- Menus -----------------------------------------------------------------------------------------

/**
 * Material 3's `DropdownMenu` as an iOS 26 menu: a glass panel below its anchor (the layout it is
 * placed in, as Material's), in the app's window. Tap outside to dismiss.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    shape: Shape = RoundedCornerShape(24.dp),
    glass: Glass = GlassMaterialDefaults.regular,
    content: @Composable ColumnScope.() -> Unit,
) {
    var anchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // Where the anchor (this call's parent) is, in the window.
    Layout(Modifier.onGloballyPositioned { c -> c.parentLayoutCoordinates?.let { anchor = it.boundsInRoot() } }) { _, _ -> layout(0, 0) {} }
    if (!expanded) return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    GlassOverlay {
        Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest))
        Column(
            modifier
                .offset {
                    with(density) {
                        IntOffset((anchor.left - origin.x + offset.x.toPx()).roundToInt(),
                            (anchor.bottom - origin.y + offset.y.toPx()).roundToInt())
                    }
                }
                .widthIn(min = 180.dp, max = 280.dp)
                .glassEffect(glass, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

/** Material 3's `DropdownMenuItem`, inside a glass [DropdownMenu]. */
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) = M3DropdownMenuItem(text = text, onClick = onClick, modifier = modifier, leadingIcon = leadingIcon,
    trailingIcon = trailingIcon, enabled = enabled, colors = colors, contentPadding = contentPadding,
    interactionSource = interactionSource)
