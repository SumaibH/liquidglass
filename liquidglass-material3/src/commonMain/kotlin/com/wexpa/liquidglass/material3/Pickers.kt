@file:OptIn(ExperimentalMaterial3Api::class)

package com.wexpa.liquidglass.material3

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect
import androidx.compose.material3.DatePicker as M3DatePicker
import androidx.compose.material3.DateRangePicker as M3DateRangePicker

/**
 * Material 3's `DatePicker` without its opaque container, so it sits on a glass [DatePickerDialog]
 * or any glass panel. Material's own title and headline; pass [colors] for the rest.
 */
@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    colors: DatePickerColors = DatePickerDefaults.colors(containerColor = Color.Transparent),
    title: (@Composable () -> Unit)? = MaterialDefault,
    headline: (@Composable () -> Unit)? = MaterialDefault,
    showModeToggle: Boolean = true,
) = when {
    title === MaterialDefault && headline === MaterialDefault ->
        M3DatePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, showModeToggle = showModeToggle)
    title === MaterialDefault ->
        M3DatePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, headline = headline, showModeToggle = showModeToggle)
    headline === MaterialDefault ->
        M3DatePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, title = title, showModeToggle = showModeToggle)
    else ->
        M3DatePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, title = title, headline = headline, showModeToggle = showModeToggle)
}

/** Material 3's `DateRangePicker` without its opaque container: see [DatePicker]. */
@Composable
fun DateRangePicker(
    state: DateRangePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    colors: DatePickerColors = DatePickerDefaults.colors(containerColor = Color.Transparent),
    title: (@Composable () -> Unit)? = MaterialDefault,
    headline: (@Composable () -> Unit)? = MaterialDefault,
    showModeToggle: Boolean = true,
) = when {
    title === MaterialDefault && headline === MaterialDefault ->
        M3DateRangePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, showModeToggle = showModeToggle)
    title === MaterialDefault ->
        M3DateRangePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, headline = headline, showModeToggle = showModeToggle)
    headline === MaterialDefault ->
        M3DateRangePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, title = title, showModeToggle = showModeToggle)
    else ->
        M3DateRangePicker(state = state, modifier = modifier, dateFormatter = dateFormatter, colors = colors, title = title, headline = headline, showModeToggle = showModeToggle)
}

/** Marks a title or headline left to Material's default, which reads Material-internal padding. */
private val MaterialDefault: @Composable () -> Unit = {}

@Composable
private fun GlassPickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)?,
    modifier: Modifier,
    shape: Shape,
    glass: Glass,
    properties: DialogProperties,
    title: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) = BasicAlertDialog(onDismissRequest, properties = properties) {
    Column(modifier.widthIn(max = 400.dp).glassEffect(glass, shape).padding(vertical = 12.dp)) {
        OnGlassPanel {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                title?.let {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) { it() }
                    }
                }
                content()
                Row(Modifier.align(Alignment.End).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Material 3's `DatePickerDialog` as a glass panel in the app's window, like [AlertDialog]. Put the
 * glass [DatePicker] (or [DateRangePicker]) in it: Material's own pickers paint an opaque container.
 */
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(34.dp),
    tonalElevation: Dp = 0.dp,
    colors: DatePickerColors = DatePickerDefaults.colors(containerColor = Color.Transparent),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    glass: Glass = GlassMaterialDefaults.panel,
    content: @Composable ColumnScope.() -> Unit,
) = GlassPickerDialog(onDismissRequest, confirmButton, dismissButton, modifier, shape, glass, properties, null, content)

/** Material 3's `TimePickerDialog` as a glass panel in the app's window: see [DatePickerDialog]. */
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    dismissButton: @Composable (() -> Unit)? = null,
    modeToggleButton: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(34.dp),
    containerColor: Color = Color.Unspecified,
    glass: Glass = GlassMaterialDefaults.panel,
    content: @Composable ColumnScope.() -> Unit,
) = GlassPickerDialog(onDismissRequest, confirmButton, dismissButton, modifier, shape, glass, properties, title, content)
