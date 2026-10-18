package com.wexpa.liquidglass.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect
import androidx.compose.material3.AssistChip as M3AssistChip
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.InputChip as M3InputChip
import androidx.compose.material3.NavigationRail as M3NavigationRail
import androidx.compose.material3.NavigationRailItem as M3NavigationRailItem
import androidx.compose.material3.Snackbar as M3Snackbar
import androidx.compose.material3.SuggestionChip as M3SuggestionChip

// ---- Chips: glass capsules; a selected filter or input chip is prominent glass --------------------

/** Material 3's `AssistChip` as a glass capsule. */
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3AssistChip(onClick = onClick, label = label, modifier = modifier.glassEffect(if (enabled) glass.interactive() else glass, shape),
        enabled = enabled, leadingIcon = leadingIcon, trailingIcon = trailingIcon, shape = shape,
        colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent, labelColor = labelColor,
            leadingIconContentColor = labelColor, trailingIconContentColor = labelColor),
        elevation = null, border = null, interactionSource = interactionSource)
}

/** Material 3's `FilterChip` as a glass capsule, prominent glass while [selected]. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = if (selected) GlassMaterialDefaults.prominent() else GlassMaterialDefaults.regular,
    labelColor: Color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3FilterChip(selected = selected, onClick = onClick, label = label,
        modifier = modifier.glassEffect(if (enabled) glass.interactive() else glass, shape), enabled = enabled,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, shape = shape,
        colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, selectedContainerColor = Color.Transparent,
            labelColor = labelColor, selectedLabelColor = labelColor, iconColor = labelColor,
            selectedLeadingIconColor = labelColor, selectedTrailingIconColor = labelColor),
        elevation = null, border = null, interactionSource = interactionSource)
}

/** Material 3's `InputChip` as a glass capsule, prominent glass while [selected]. */
@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    avatar: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = if (selected) GlassMaterialDefaults.prominent() else GlassMaterialDefaults.regular,
    labelColor: Color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3InputChip(selected = selected, onClick = onClick, label = label,
        modifier = modifier.glassEffect(if (enabled) glass.interactive() else glass, shape), enabled = enabled,
        leadingIcon = leadingIcon, avatar = avatar, trailingIcon = trailingIcon, shape = shape,
        colors = InputChipDefaults.inputChipColors(containerColor = Color.Transparent, selectedContainerColor = Color.Transparent,
            labelColor = labelColor, selectedLabelColor = labelColor),
        elevation = null, border = null, interactionSource = interactionSource)
}

/** Material 3's `SuggestionChip` as a glass capsule. */
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) = GlassComponent {
    M3SuggestionChip(onClick = onClick, label = label,
        modifier = modifier.glassEffect(if (enabled) glass.interactive() else glass, shape), enabled = enabled, icon = icon,
        shape = shape, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Transparent, labelColor = labelColor),
        elevation = null, border = null, interactionSource = interactionSource)
}

// ---- Snackbar --------------------------------------------------------------------------------------

/** Material 3's `Snackbar` as a floating glass capsule. */
@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionContentColor: Color = MaterialTheme.colorScheme.primary,
    dismissActionContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) = M3Snackbar(modifier = modifier.glassEffect(glass, shape), action = action, dismissAction = dismissAction,
    actionOnNewLine = actionOnNewLine, shape = shape, containerColor = Color.Transparent, contentColor = contentColor,
    actionContentColor = actionContentColor, dismissActionContentColor = dismissActionContentColor, content = content)

/** Material 3's `Snackbar` for a [SnackbarHostState][androidx.compose.material3.SnackbarHostState]'s data, as glass. */
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    glass: Glass = GlassMaterialDefaults.regular,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    actionContentColor: Color = MaterialTheme.colorScheme.primary,
    dismissActionContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) = M3Snackbar(snackbarData = snackbarData, modifier = modifier.glassEffect(glass, shape), actionOnNewLine = actionOnNewLine,
    shape = shape, containerColor = Color.Transparent, contentColor = contentColor, actionColor = actionColor,
    actionContentColor = actionContentColor, dismissActionContentColor = dismissActionContentColor)

// ---- Navigation rail -------------------------------------------------------------------------------

/** Material 3's `NavigationRail` on a floating glass panel (iPadOS's sidebar material). */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    glass: Glass = Glass.Toolbar,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) = M3NavigationRail(modifier = modifier.glassEffect(glass, androidx.compose.foundation.shape.RoundedCornerShape(28.dp)),
    containerColor = Color.Transparent, contentColor = contentColor, header = header, windowInsets = windowInsets,
    content = content)

/** Material 3's `NavigationRailItem` in a glass rail: the selection is tinted glass. */
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .14f)),
    interactionSource: MutableInteractionSource? = null,
) = M3NavigationRailItem(selected = selected, onClick = onClick, icon = icon, modifier = modifier, enabled = enabled,
    label = label, alwaysShowLabel = alwaysShowLabel, colors = colors, interactionSource = interactionSource)
