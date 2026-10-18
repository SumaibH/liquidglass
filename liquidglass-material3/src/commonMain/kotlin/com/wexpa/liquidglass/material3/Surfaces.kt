package com.wexpa.liquidglass.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect

/**
 * Material 3's `Surface` as glass: [content] on a [glass] panel in [shape], with [contentColor]
 * for what is drawn on it. The default is the frosted [GlassMaterialDefaults.panel]; a [color]
 * given tints it. Glass has its own depth, so the elevations are accepted for source compatibility
 * and ignored. A [border] is drawn over the glass.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = Color.Unspecified,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    glass: Glass = surfaceGlass(color),
    content: @Composable () -> Unit,
) = Box(modifier.glassEffect(glass, shape).then(border?.let { Modifier.border(it, shape) } ?: Modifier), propagateMinConstraints = true) {
    OnGlassPanel { CompositionLocalProvider(LocalContentColor provides contentColor) { content() } }
}

/** Material 3's clickable `Surface` as interactive glass: see [Surface]. */
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = Color.Unspecified,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    glass: Glass = surfaceGlass(color),
    content: @Composable () -> Unit,
) = Box(
    modifier.glassEffect(if (enabled) glass.interactive() else glass, shape)
        .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
        .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    propagateMinConstraints = true,
) {
    OnGlassPanel { CompositionLocalProvider(LocalContentColor provides contentColor) { content() } }
}

/** The panel glass, tinted with [color] when one is given (Material's `color` parameter). */
@Composable
private fun surfaceGlass(color: Color): Glass =
    if (color == Color.Unspecified || color.alpha < .05f) GlassMaterialDefaults.panel
    else GlassMaterialDefaults.panel.tint(
        color.copy(alpha = (color.alpha * GlassMaterialDefaults.ProminentAlpha).coerceAtMost(GlassMaterialDefaults.ProminentAlpha)))
