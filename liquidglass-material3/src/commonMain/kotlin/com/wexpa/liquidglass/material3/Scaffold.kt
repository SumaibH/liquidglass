package com.wexpa.liquidglass.material3

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wexpa.liquidglass.LiquidGlassTheme
import androidx.compose.material3.Scaffold as M3Scaffold

/**
 * Material 3's `Scaffold`, with Liquid Glass: the same parameters, and the screen behind every bar,
 * button and sheet in it is recorded once so they all refract it. Glass components in the bars, and
 * `Modifier.glassEffect` anywhere in [content], need nothing else.
 *
 * As on iOS, [content] runs under the bars; pass the [PaddingValues] it receives to a list's
 * `contentPadding` so the list scrolls under them rather than stopping at their edge.
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    LiquidGlassTheme(dark = MaterialTheme.colorScheme.isDark, background = containerColor) {
        M3Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}
