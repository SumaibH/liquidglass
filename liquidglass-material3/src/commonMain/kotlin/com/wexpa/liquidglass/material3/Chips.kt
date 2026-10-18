package com.wexpa.liquidglass.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.interaction.MutableInteractionSource
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

/** Material 3's `ElevatedAssistChip`: glass carries its own depth, so it is [AssistChip]. */
@Composable
fun ElevatedAssistChip(
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
) = AssistChip(onClick, label, modifier, enabled, leadingIcon, trailingIcon, shape, glass, labelColor, interactionSource)

/** Material 3's `ElevatedFilterChip`: glass carries its own depth, so it is [FilterChip]. */
@Composable
fun ElevatedFilterChip(
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
) = FilterChip(selected, onClick, label, modifier, enabled, leadingIcon, trailingIcon, shape, glass, labelColor, interactionSource)

/** Material 3's `ElevatedSuggestionChip`: glass carries its own depth, so it is [SuggestionChip]. */
@Composable
fun ElevatedSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    interactionSource: MutableInteractionSource? = null,
) = SuggestionChip(onClick, label, modifier, enabled, icon, shape, glass, labelColor, interactionSource)

// ---- Snackbar --------------------------------------------------------------------------------------

/**
 * Material 3's `Snackbar` as floating glass: the message, then the action and the dismiss action.
 * Drawn here rather than by Material's, whose surface always casts its own shadow inside the glass.
 */
@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionContentColor: Color = MaterialTheme.colorScheme.primary,
    dismissActionContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    val message = @Composable {
        Box(Modifier.padding(vertical = 14.dp)) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
            }
        }
    }
    val actions = @Composable {
        if (action != null) CompositionLocalProvider(LocalContentColor provides actionContentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { action() }
        }
        if (dismissAction != null) CompositionLocalProvider(LocalContentColor provides dismissActionContentColor) { dismissAction() }
    }
    val box = modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 48.dp).glassEffect(glass, shape)
        .padding(start = 16.dp, end = 8.dp)
    OnGlassPanel {
        if (actionOnNewLine) Column(box) {
            message()
            Row(Modifier.align(Alignment.End).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
        } else Row(box, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { message() }
            actions()
        }
    }
}

/** Material 3's `Snackbar` for a [SnackbarHostState][androidx.compose.material3.SnackbarHostState]'s data, as glass. */
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    glass: Glass = GlassMaterialDefaults.panel,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    actionContentColor: Color = MaterialTheme.colorScheme.primary,
    dismissActionContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val visuals = snackbarData.visuals
    Snackbar(
        modifier = modifier,
        action = visuals.actionLabel?.let { label ->
            { TextButton(onClick = { snackbarData.performAction() }, contentColor = actionColor) { Text(label) } }
        },
        dismissAction = if (!visuals.withDismissAction) null else {
            { IconButton(onClick = { snackbarData.dismiss() }) { CloseGlyph() } }
        },
        actionOnNewLine = actionOnNewLine, shape = shape, glass = glass, contentColor = contentColor,
        actionContentColor = actionContentColor, dismissActionContentColor = dismissActionContentColor,
    ) { Text(visuals.message) }
}

/** A 14 dp cross in the content colour, without an icon library. */
@Composable
private fun CloseGlyph() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(14.dp)) {
        val w = 1.8.dp.toPx()
        drawLine(color, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
    }
}

// ---- Navigation rail -------------------------------------------------------------------------------

/** Material 3's `NavigationRail` on a floating glass panel (iPadOS's sidebar material). */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    glass: Glass = GlassMaterialDefaults.toolbar,
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
