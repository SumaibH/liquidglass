@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.ExtendedFloatingActionButton as M3ExtendedFab
import androidx.compose.material3.FloatingActionButton as M3Fab
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.IconToggleButton as M3IconToggleButton

/** Glass replaces Material's container, ripple and minimum touch padding: the press is the glass's own. */
@Composable
internal fun GlassComponent(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        LocalRippleConfiguration provides null, content = content)

/** iOS 26's minimum button height. Authored from Apple's 44 pt hit-target guidance. */
internal val GlassButtonHeight = 44.dp

private fun Modifier.glassButton(glass: Glass, shape: Shape, enabled: Boolean) =
    heightIn(min = GlassButtonHeight).glassEffect(if (enabled) glass.interactive() else glass, shape)

// ---- Buttons --------------------------------------------------------------------------------------

/** Material 3's `Button` as prominent glass (SwiftUI's `.glassProminent`): tinted with the primary colour. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    glass: Glass = GlassMaterialDefaults.prominent(),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = GlassComponent {
    M3Button(onClick, modifier.glassButton(glass, shape, enabled), enabled, shape,
        ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = contentColor,
            disabledContainerColor = Color.Transparent), null, border, contentPadding, interactionSource, content)
}

/** Material 3's `FilledTonalButton` as glass tinted with the secondary container colour. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    glass: Glass = GlassMaterialDefaults.forContainer(MaterialTheme.colorScheme.secondaryContainer),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, glass, contentColor, border, contentPadding, interactionSource, content)

/** Material 3's `ElevatedButton` as plain glass (SwiftUI's `.glass` button style). */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, glass, contentColor, border, contentPadding, interactionSource, content)

/** Material 3's `OutlinedButton` as plain glass; the outline is the glass's own rim. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, glass, contentColor, border, contentPadding, interactionSource, content)

/** Material 3's `TextButton`: on iOS a borderless button has no glass, so this is Material's own with
 * glass when [glass] is given. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    glass: Glass = Glass.Identity,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, glass, contentColor, border, contentPadding, interactionSource, content)

// ---- Icon buttons ---------------------------------------------------------------------------------

/** Material 3's `IconButton` as a round glass button, as iOS 26's bar buttons are. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Glass = if (LocalInGlassGroup.current) Glass.Identity else GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = GlassComponent {
    M3IconButton(onClick, modifier.sizeIn(minWidth = GlassButtonHeight, minHeight = GlassButtonHeight)
        .glassEffect(if (enabled) glass.interactive() else glass, shape), enabled,
        IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent, contentColor = contentColor),
        interactionSource, shape = shape, content = content)
}

/** Material 3's `FilledIconButton` as prominent glass. */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Glass = GlassMaterialDefaults.prominent(),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = CircleShape,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconButton(onClick, modifier, enabled, glass, contentColor, shape, interactionSource, content)

/** Material 3's `FilledTonalIconButton` as glass tinted with the secondary container colour. */
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Glass = GlassMaterialDefaults.forContainer(MaterialTheme.colorScheme.secondaryContainer),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = CircleShape,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconButton(onClick, modifier, enabled, glass, contentColor, shape, interactionSource, content)

/** Material 3's `OutlinedIconButton` as plain glass. */
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconButton(onClick, modifier, enabled, glass, contentColor, shape, interactionSource, content)

/**
 * Material 3's `IconToggleButton` as a round glass button: plain glass when off, prominent glass
 * (tinted with the primary colour) when on, as an iOS toggle button. The same parameters as
 * Material's, plus the glass for each state.
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Glass = if (LocalInGlassGroup.current) Glass.Identity else GlassMaterialDefaults.regular,
    checkedGlass: Glass = GlassMaterialDefaults.prominent(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    checkedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = CircleShape,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = GlassComponent {
    val g = if (checked) checkedGlass else glass
    val ink = if (checked) checkedContentColor else contentColor
    M3IconToggleButton(checked, onCheckedChange,
        modifier.sizeIn(minWidth = GlassButtonHeight, minHeight = GlassButtonHeight)
            .glassEffect(if (enabled) g.interactive() else g, shape), enabled,
        IconButtonDefaults.iconToggleButtonColors(containerColor = Color.Transparent, contentColor = ink,
            checkedContainerColor = Color.Transparent, checkedContentColor = ink),
        interactionSource, shape = shape, content = content)
}

/** Material 3's `FilledIconToggleButton`: see [IconToggleButton]. Off is glass tinted with the
 * surface variant, as Material's filled toggle is. */
@Composable
fun FilledIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    glass: Glass = GlassMaterialDefaults.forContainer(MaterialTheme.colorScheme.surfaceContainerHighest),
    checkedGlass: Glass = GlassMaterialDefaults.prominent(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconToggleButton(checked, onCheckedChange, modifier, enabled, glass, checkedGlass,
    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, shape, interactionSource, content)

/** Material 3's `FilledTonalIconToggleButton`: see [IconToggleButton]. On is glass tinted with the
 * secondary container colour. */
@Composable
fun FilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    checkedGlass: Glass = GlassMaterialDefaults.forContainer(MaterialTheme.colorScheme.secondaryContainer),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconToggleButton(checked, onCheckedChange, modifier, enabled, glass, checkedGlass,
    MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onSecondaryContainer, shape, interactionSource, content)

/** Material 3's `OutlinedIconToggleButton`: see [IconToggleButton]. */
@Composable
fun OutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    checkedGlass: Glass = GlassMaterialDefaults.forContainer(MaterialTheme.colorScheme.inverseSurface),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = IconToggleButton(checked, onCheckedChange, modifier, enabled, glass, checkedGlass,
    MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.inverseOnSurface, shape, interactionSource, content)

// ---- Floating action buttons ----------------------------------------------------------------------

/** Material 3's `FloatingActionButton` as prominent glass, tinted with [containerColor]. */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    glass: Glass = GlassMaterialDefaults.forContainer(containerColor),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = GlassComponent {
    M3Fab(onClick, modifier.glassEffect(glass.interactive(), shape), shape, Color.Transparent, contentColor,
        FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp), interactionSource, content)
}

/** Material 3's `SmallFloatingActionButton` as prominent glass. */
@Composable
fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallShape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    glass: Glass = GlassMaterialDefaults.forContainer(containerColor),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = FloatingActionButton(onClick, modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp), shape, containerColor,
    contentColor, glass, interactionSource, content)

/** Material 3's `LargeFloatingActionButton` as prominent glass. */
@Composable
fun LargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeShape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    glass: Glass = GlassMaterialDefaults.forContainer(containerColor),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = FloatingActionButton(onClick, modifier.sizeIn(minWidth = 96.dp, minHeight = 96.dp), shape, containerColor,
    contentColor, glass, interactionSource, content)

/** Material 3's `ExtendedFloatingActionButton` (icon and text) as prominent glass. */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    glass: Glass = GlassMaterialDefaults.forContainer(containerColor),
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3ExtendedFab(text, icon, onClick, modifier.glassEffect(glass.interactive(), shape), expanded, shape,
        Color.Transparent, contentColor, FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp), interactionSource)
}

// ---- Cards ----------------------------------------------------------------------------------------

/** Material 3's `Card` as a glass panel. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = GlassComponent {
    M3Card(modifier.glassEffect(glass, shape), shape,
        CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = contentColor),
        CardDefaults.cardElevation(0.dp), border) { OnGlassPanel { content() } }
}

/** Material 3's clickable `Card` as an interactive glass panel. */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) = GlassComponent {
    M3Card(onClick, modifier.glassEffect(if (enabled) glass.interactive() else glass, shape), enabled, shape,
        CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = contentColor),
        CardDefaults.cardElevation(0.dp), border, interactionSource) { OnGlassPanel { content() } }
}

/** Material 3's `ElevatedCard` as a glass panel; glass carries its own depth, so there is no shadow. */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) = Card(modifier, shape, glass, contentColor, null, content)

/** Material 3's `OutlinedCard` as a glass panel; the rim is the outline. */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.outlinedShape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = Card(modifier, shape, glass, contentColor, border, content)
