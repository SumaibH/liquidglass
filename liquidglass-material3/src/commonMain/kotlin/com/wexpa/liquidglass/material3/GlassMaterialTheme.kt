package com.wexpa.liquidglass.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.wexpa.liquidglass.Glass

/**
 * How a glass component is laid out. [Ios] (the default) uses iOS 26's shapes and placement: a floating
 * capsule tab bar with a lens, capsule buttons, bars without a container. [Material] keeps Material 3's
 * own layout and proportions and only turns the surfaces into glass.
 */
enum class GlassLayout { Ios, Material }

/** Glass for the app's colours: what the components draw by default. */
object GlassMaterialDefaults {
    /** Plain glass: secondary buttons, cards, icon buttons, bars. */
    val regular: Glass @Composable @ReadOnlyComposable get() = Glass.Regular

    /**
     * Prominent glass, as SwiftUI's `.glassProminent` button style: glass tinted with [color] (the
     * scheme's primary by default), for the main action on a screen.
     */
    @Composable @ReadOnlyComposable
    fun prominent(color: Color = MaterialTheme.colorScheme.primary): Glass = Glass.Regular.tint(color.copy(alpha = ProminentAlpha))

    /** Tinted glass for a container colour a component was given, or plain glass when it is transparent. */
    @Composable @ReadOnlyComposable
    fun forContainer(container: Color): Glass =
        if (container.alpha < .05f || container == Color.Unspecified) Glass.Regular
        else Glass.Regular.tint(container.copy(alpha = (container.alpha * ProminentAlpha).coerceAtMost(ProminentAlpha)))

    /** How strongly prominent glass carries its tint. Authored, not measured. */
    const val ProminentAlpha = 0.62f
}

/** True when the scheme is a dark one (its surface is dark). */
internal val ColorScheme.isDark: Boolean get() = surface.luminance() < .5f
