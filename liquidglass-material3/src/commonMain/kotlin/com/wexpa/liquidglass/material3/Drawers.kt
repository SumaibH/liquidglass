@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect
import androidx.compose.material3.NavigationDrawerItem as M3NavigationDrawerItem

/** iPadOS's sidebar: a floating panel inset from the screen's edge. Authored, not measured. */
private val SidebarShape = RoundedCornerShape(28.dp)

@Composable
private fun GlassDrawerSheet(
    modifier: Modifier,
    shape: Shape,
    glass: Glass,
    contentColor: Color,
    windowInsets: WindowInsets,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.windowInsetsPadding(windowInsets).padding(8.dp).fillMaxHeight().widthIn(max = 320.dp)
        .glassEffect(glass, shape).padding(vertical = 12.dp),
) {
    OnGlassPanel { CompositionLocalProvider(LocalContentColor provides contentColor) { content() } }
}

/**
 * Material 3's `ModalDrawerSheet` as iPadOS's sidebar: a floating glass panel (the frosted
 * [GlassMaterialDefaults.panel]) inside Material's `ModalNavigationDrawer`, which keeps its own
 * gestures and scrim. The same parameters, with [glass] in place of the container colour and
 * elevation.
 */
@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = SidebarShape,
    drawerContainerColor: Color = Color.Unspecified,
    drawerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    drawerTonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    glass: Glass = drawerGlass(drawerContainerColor),
    content: @Composable ColumnScope.() -> Unit,
) = GlassDrawerSheet(modifier, drawerShape, glass, drawerContentColor, windowInsets, content)

/** Material 3's `DismissibleDrawerSheet` as a floating glass panel: see [ModalDrawerSheet]. */
@Composable
fun DismissibleDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = SidebarShape,
    drawerContainerColor: Color = Color.Unspecified,
    drawerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    drawerTonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    glass: Glass = drawerGlass(drawerContainerColor),
    content: @Composable ColumnScope.() -> Unit,
) = GlassDrawerSheet(modifier, drawerShape, glass, drawerContentColor, windowInsets, content)

/** Material 3's `PermanentDrawerSheet` as a floating glass panel: see [ModalDrawerSheet]. */
@Composable
fun PermanentDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = SidebarShape,
    drawerContainerColor: Color = Color.Unspecified,
    drawerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    drawerTonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    glass: Glass = drawerGlass(drawerContainerColor),
    content: @Composable ColumnScope.() -> Unit,
) = GlassDrawerSheet(modifier, drawerShape, glass, drawerContentColor, windowInsets, content)

/**
 * Material 3's `NavigationDrawerItem` on a glass sidebar: the selected item is a prominent glass
 * capsule (tinted with the primary colour), the others draw no container. The same parameters.
 */
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    selectedGlass: Glass = GlassMaterialDefaults.prominent(),
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = Color.Transparent,
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
        selectedBadgeColor = MaterialTheme.colorScheme.onPrimary,
    ),
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3NavigationDrawerItem(label = label, selected = selected, onClick = onClick,
        modifier = modifier.padding(horizontal = 8.dp).then(if (selected) Modifier.glassEffect(selectedGlass.interactive(), shape) else Modifier),
        icon = icon, badge = badge, shape = shape, colors = colors, interactionSource = interactionSource)
}

/** The panel glass, tinted with Material's `drawerContainerColor` when one is given. */
@Composable
private fun drawerGlass(color: Color): Glass =
    if (color == Color.Unspecified) GlassMaterialDefaults.panel else GlassMaterialDefaults.forContainer(color)

/** Material 3's `ModalDrawerSheet` overload that takes the [drawerState] for predictive back: the
 * glass sheet, with the state accepted for source compatibility. */
@Composable
fun ModalDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = SidebarShape,
    drawerContainerColor: Color = Color.Unspecified,
    drawerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    drawerTonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) = ModalDrawerSheet(modifier, drawerShape, drawerContainerColor, drawerContentColor, drawerTonalElevation, windowInsets, content = content)

/** Material 3's `DismissibleDrawerSheet` overload that takes the [drawerState]: see [ModalDrawerSheet]. */
@Composable
fun DismissibleDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = SidebarShape,
    drawerContainerColor: Color = Color.Unspecified,
    drawerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    drawerTonalElevation: Dp = 0.dp,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) = DismissibleDrawerSheet(modifier, drawerShape, drawerContainerColor, drawerContentColor, drawerTonalElevation, windowInsets, content = content)
