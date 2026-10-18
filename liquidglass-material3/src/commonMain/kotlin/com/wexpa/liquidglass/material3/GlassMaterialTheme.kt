package com.wexpa.liquidglass.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.GlassStyle

/**
 * How a glass component is laid out. [Ios] (the default) uses iOS 26's shapes and placement: a floating
 * capsule tab bar with a lens, capsule buttons, bars without a container. [Material] keeps Material 3's
 * own layout and proportions and only turns the surfaces into glass.
 */
enum class GlassLayout { Ios, Material }

/** True inside a glass panel's content; [GlassMaterialDefaults.regular] reads it. */
internal val LocalOnGlassPanel = staticCompositionLocalOf { false }

/** Marks [content] as sitting on a glass panel. */
@Composable
internal fun OnGlassPanel(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalOnGlassPanel provides true, content = content)

/** Glass for the app's colours: what the components draw by default. */
object GlassMaterialDefaults {
    /**
     * Plain glass: secondary buttons, icon buttons, chips, bars. On a glass panel (a card, dialog,
     * sheet, menu or snackbar) it is [panel]: glass on glass samples the page under the panel, not the
     * panel, so unfrosted it would show a sharp, bent copy of what the panel hides.
     */
    val regular: Glass @Composable @ReadOnlyComposable get() = if (LocalOnGlassPanel.current) panel else Glass.Regular

    /**
     * Prominent glass, as SwiftUI's `.glassProminent` button style: glass tinted with [color] (the
     * scheme's primary by default), for the main action on a screen.
     */
    @Composable @ReadOnlyComposable
    fun prominent(color: Color = MaterialTheme.colorScheme.primary): Glass = regular.tint(color.copy(alpha = ProminentAlpha))

    /** Tinted glass for a container colour a component was given, or plain glass when it is transparent. */
    @Composable @ReadOnlyComposable
    fun forContainer(container: Color): Glass =
        if (container.alpha < .05f || container == Color.Unspecified) regular
        else regular.tint(container.copy(alpha = (container.alpha * ProminentAlpha).coerceAtMost(ProminentAlpha)))

    /**
     * The frost under [panel]: 7.7 dp, measured on iOS's system backdrops (Control Center, App
     * Library, the Today view; [GlassStyle.backdropSigma]). Using it for in-app panels is authored.
     */
    val PanelBackdropSigma = 7.7.dp

    /**
     * Glass for large surfaces: cards, dialogs, sheets, menus, snackbars and the search panel. The
     * same material as [regular] over a frosted backdrop, as iOS draws its sheets and alerts: without
     * it, a panel's wide rim band compresses the text beside it into a sharp, smeared copy along the
     * edge. The blur is the measured system-backdrop sigma ([PanelBackdropSigma]).
     */
    val panel: Glass = Glass(
        GlassStyle.inApp(dark = false).copy(backdropSigma = PanelBackdropSigma),
        dark = GlassStyle.inApp(dark = true).copy(backdropSigma = PanelBackdropSigma),
    )

    /** How strongly prominent glass carries its tint. Authored, not measured. */
    const val ProminentAlpha = 0.62f
}

/** True when the scheme is a dark one (its surface is dark). */
internal val ColorScheme.isDark: Boolean get() = surface.luminance() < .5f
