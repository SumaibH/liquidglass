@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassTabBar
import com.wexpa.liquidglass.GlassTabBarStyle
import com.wexpa.liquidglass.glassEffect
import androidx.compose.material3.ScrollableTabRow as M3ScrollableTabRow
import androidx.compose.material3.SegmentedButtonDefaults as M3SegmentedButtonDefaults
import androidx.compose.material3.Tab as M3Tab

// ---- One segmented control for segmented buttons and tab rows -------------------------------------

@Stable
internal class SegmentSlot {
    var selected by mutableStateOf(false)
    var onClick: () -> Unit by mutableStateOf({})
    var enabled by mutableStateOf(true)
    var icon: (@Composable () -> Unit)? by mutableStateOf(null)
    var label: (@Composable () -> Unit)? by mutableStateOf(null)
    var selectedColor: Color? by mutableStateOf(null)
    var unselectedColor: Color? by mutableStateOf(null)
}

@Stable
internal class SegmentItems { val slots = mutableStateListOf<SegmentSlot>() }

internal val LocalSegmentItems = staticCompositionLocalOf<SegmentItems?> { null }

@Composable
private fun RegisterSegment(selected: Boolean, onClick: () -> Unit, enabled: Boolean, icon: (@Composable () -> Unit)?,
                            label: (@Composable () -> Unit)?, selectedColor: Color?, unselectedColor: Color?) {
    val items = LocalSegmentItems.current ?: error("A segment must be inside its glass row")
    val slot = remember { SegmentSlot() }
    slot.selected = selected; slot.onClick = onClick; slot.enabled = enabled; slot.icon = icon; slot.label = label
    slot.selectedColor = selectedColor; slot.unselectedColor = unselectedColor
    DisposableEffect(items, slot) { items.slots += slot; onDispose { items.slots -= slot } }
}

/** iOS 26's segmented control: a glass capsule whose selection is a lens, built on [GlassTabBar]. */
@Composable
private fun GlassSegmentedControl(modifier: Modifier, height: Dp, content: @Composable () -> Unit) {
    val items = remember { SegmentItems() }
    CompositionLocalProvider(LocalSegmentItems provides items) { Box(Modifier.size(0.dp)) { content() } }
    val slots = items.slots
    if (slots.isEmpty()) return
    val dark = MaterialTheme.colorScheme.isDark
    GlassTabBar(
        itemCount = slots.size,
        selectedIndex = slots.indexOfFirst { it.selected }.coerceAtLeast(0),
        onSelected = { i -> slots.getOrNull(i)?.let { if (it.enabled) it.onClick() } },
        modifier = modifier,
        style = GlassTabBarStyle.Calm(dark).copy(height = height),
    ) { index, isSelected ->
        val slot = slots.getOrNull(index) ?: return@GlassTabBar
        val color = when {
            !slot.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            isSelected -> slot.selectedColor ?: MaterialTheme.colorScheme.primary
            else -> slot.unselectedColor ?: MaterialTheme.colorScheme.onSurface
        }
        CompositionLocalProvider(LocalContentColor provides color) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                slot.icon?.let { Box(Modifier.size(18.dp), Alignment.Center) { it() }; Spacer(Modifier.width(6.dp)) }
                slot.label?.let { ProvideTextStyle(MaterialTheme.typography.labelLarge) { it() } }
            }
        }
    }
}

// ---- Segmented buttons ----------------------------------------------------------------------------

/** The scope of a glass [SingleChoiceSegmentedButtonRow]. */
interface SingleChoiceSegmentedButtonRowScope : RowScope

/** The scope of a glass [MultiChoiceSegmentedButtonRow]. */
interface MultiChoiceSegmentedButtonRowScope : RowScope

private class SegmentedScope(row: RowScope) : SingleChoiceSegmentedButtonRowScope, MultiChoiceSegmentedButtonRowScope, RowScope by row

/**
 * Material 3's `SingleChoiceSegmentedButtonRow` as iOS 26's segmented control: a glass capsule with a
 * lens selection that you can tap or drag between segments. The segments are [SegmentedButton]s.
 */
@Composable
fun SingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = 0.dp,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit,
) = GlassSegmentedControl(modifier.fillMaxWidth(), 40.dp) {
    Row { SegmentedScope(this).content() }
}

/** One segment of a glass [SingleChoiceSegmentedButtonRow]; [shape], [border] and [contentPadding] are the
 * control's own and ignored here, kept so Material code compiles unchanged. */
@Composable
fun SingleChoiceSegmentedButtonRowScope.SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
) = RegisterSegment(selected, onClick, enabled, null, label, colors.activeContentColor, colors.inactiveContentColor)

/**
 * Material 3's `MultiChoiceSegmentedButtonRow` on a glass capsule: each checked segment is prominent glass.
 */
@Composable
fun MultiChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = 4.dp,
    content: @Composable MultiChoiceSegmentedButtonRowScope.() -> Unit,
) = Row(modifier.heightIn(min = 40.dp).glassEffect(Glass.Regular, CapsuleShape).padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(space), verticalAlignment = Alignment.CenterVertically) {
    SegmentedScope(this).content()
}

/** One toggle of a glass [MultiChoiceSegmentedButtonRow]. */
@Composable
fun MultiChoiceSegmentedButtonRowScope.SegmentedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
) {
    // A checked segment is prominent glass, tinted with the primary colour, so the default active
    // colour (primary) would vanish into it: take onPrimary, as Button does, unless the caller chose.
    val scheme = MaterialTheme.colorScheme
    val color = if (!checked) colors.inactiveContentColor
        else if (colors.activeContentColor == scheme.primary) scheme.onPrimary else colors.activeContentColor
    TextButton(onClick = { onCheckedChange(!checked) }, modifier = modifier.weight(1f), enabled = enabled, shape = CapsuleShape,
        glass = if (checked) GlassMaterialDefaults.prominent() else Glass.Identity, contentColor = color,
        contentPadding = contentPadding, interactionSource = interactionSource) { label() }
}

/** Material 3's `SegmentedButtonDefaults`, for code that passes its shapes, colours and icon. */
object SegmentedButtonDefaults {
    @Composable
    fun colors(
        activeContentColor: Color = MaterialTheme.colorScheme.primary,
        inactiveContentColor: Color = MaterialTheme.colorScheme.onSurface,
    ): SegmentedButtonColors = M3SegmentedButtonDefaults.colors(activeContentColor = activeContentColor,
        inactiveContentColor = inactiveContentColor, activeContainerColor = Color.Transparent,
        inactiveContainerColor = Color.Transparent)

    @Composable
    fun itemShape(index: Int, count: Int, baseShape: androidx.compose.foundation.shape.CornerBasedShape = CapsuleShape): Shape =
        M3SegmentedButtonDefaults.itemShape(index, count, baseShape)

    @Composable
    fun Icon(active: Boolean, activeContent: @Composable () -> Unit = {}, inactiveContent: (@Composable () -> Unit)? = null) =
        M3SegmentedButtonDefaults.Icon(active, activeContent, inactiveContent)
}

// ---- Tabs ------------------------------------------------------------------------------------------

/** Tabs register with a segmented control, or render as Material's inside a scrollable row. */
internal val LocalTabsScrollable = staticCompositionLocalOf { false }

/**
 * Material 3's `PrimaryTabRow` as iOS 26's segmented control: a glass capsule with a lens selection.
 * [indicator] and [divider] belong to Material's layout and are not drawn.
 */
@Composable
fun PrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    indicator: @Composable TabIndicatorScope.() -> Unit = {},
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit,
) = GlassSegmentedControl(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), 44.dp) {
    CompositionLocalProvider(LocalTabsScrollable provides false) { tabs() }
}

/** Material 3's `SecondaryTabRow`, as the same glass segmented control. */
@Composable
fun SecondaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    indicator: @Composable TabIndicatorScope.() -> Unit = {},
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit,
) = PrimaryTabRow(selectedTabIndex, modifier, containerColor, contentColor, indicator, divider, tabs)

/** Material 3's `TabRow`, as the same glass segmented control. */
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = {},
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit,
) = GlassSegmentedControl(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), 44.dp) {
    CompositionLocalProvider(LocalTabsScrollable provides false) { tabs() }
}

/** Material 3's `ScrollableTabRow` on a glass capsule; its tabs are Material's, scrolling inside it. */
@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    edgePadding: Dp = 12.dp,
    tabs: @Composable () -> Unit,
) = M3ScrollableTabRow(selectedTabIndex = selectedTabIndex,
    modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp).glassEffect(Glass.Regular, CapsuleShape),
    containerColor = Color.Transparent, contentColor = contentColor, edgePadding = edgePadding, divider = {},
    tabs = { CompositionLocalProvider(LocalTabsScrollable provides true) { tabs() } })

/** Material 3's `Tab`: a segment of a glass tab row. */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) {
    if (LocalTabsScrollable.current) {
        M3Tab(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled, text = text, icon = icon,
            selectedContentColor = selectedContentColor, unselectedContentColor = unselectedContentColor,
            interactionSource = interactionSource)
    } else {
        RegisterSegment(selected, onClick, enabled, icon, text, selectedContentColor, unselectedContentColor)
    }
}
