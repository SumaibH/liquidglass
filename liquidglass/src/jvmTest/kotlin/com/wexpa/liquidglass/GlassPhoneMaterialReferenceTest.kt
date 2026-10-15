package com.wexpa.liquidglass

import kotlin.test.Test
import kotlin.test.assertEquals

/** Original 6756 T01: stable scene, untouched bar region, native PQ->sRGB values. */
class GlassPhoneMaterialReferenceTest {
    @Test fun theCalmBarDoesNotBrightenTwiceWhileHeld() {
        // GlassTabBar supplies heldLift to its style; LiquidGlass adds the interaction's
        // pressLift. Check their combined endpoint against the independent references.
        // Display colours (15 October; the recordings are HDR10, decoded with research/README.md
        // II.4): iOS's held bar reads 45 over black in docs/2, IMG_6756 and LOLL8185.
        assertEquals(45.0, heldBar(GlassTabBarStyle.Calm(dark = true)), 1.5,
            "Phone held bar must not add generic control lift again")
        // The calibration made against the recordings decoded as sRGB (13 October): 52-53 held.
        // The older 6756 T01 plateau (44 held) stays on V3, tested below.
        assertEquals(53.0, heldBar(GlassTabBarStyle.CalmRecordedSpace(dark = true)), 1.0,
            "Recorded-space held bar")
    }

    @Test fun onlyDarkCalmLiftsTheBarForAnyTouch() {
        // iOS's whole bar lifts on every touch, a tap included (docs/1 in display colours: +13 at
        // the tab a tap never visits). Opt-in: off by default and on the recorded-space preset.
        assertEquals(false, GlassTabBarStyle().touchLift)
        assertEquals(true, GlassTabBarStyle.Calm(dark = true).touchLift)
        assertEquals(false, GlassTabBarStyle.Calm(dark = false).touchLift)
        assertEquals(false, GlassTabBarStyle.CalmRecordedSpace(dark = true).touchLift)
    }

    private fun heldBar(calm: GlassTabBarStyle): Double {
        val s = calm.bar
        val pixels = GlassRender.render(
            width = 834, height = 186, pad = 24, profile = 2f,
            tintAlpha = s.tint.alpha,
            tint = Triple(s.tint.red, s.tint.green, s.tint.blue),
            lift = s.tintLift + calm.heldLift + checkNotNull(calm.barInteraction).pressLift,
            backdrop = { _, _ -> 0xFF000000.toInt() },
        )
        return GlassRender.luma(pixels[(93+24)*(834+48)+350+24])
    }

    @Test fun theDarkV3BarMatchesTheOriginalRestAndHeldPlateaus() {
        val v3 = GlassTabBarStyle.V3(dark = true)
        for ((held, expected) in listOf(false to 32.0, true to 44.0)) {
            val s = v3.bar
            val pixels = GlassRender.render(
                width = 834, height = 186, pad = 24, profile = 2f,
                tintAlpha = s.tint.alpha,
                tint = Triple(s.tint.red, s.tint.green, s.tint.blue),
                lift = s.tintLift + if (held) v3.heldLift else 0f,
                backdrop = { _, _ -> 0xFF000000.toInt() },
            )
            val value = GlassRender.luma(pixels[(93+24)*(834+48)+350+24])
            assertEquals(expected, value, 1.0, "6756 T01 bar plateau, held=$held")
        }
        // The original target-calibrated general material remains available unchanged.
        assertEquals(35f / 255f, GlassTabBarStyle.Measured(dark = true).bar.tintLift, 1e-6f)
        assertEquals(16f / 255f, GlassTabBarStyle.Measured(dark = true).heldLift, 1e-6f)
        val light = GlassTabBarStyle.Measured(dark = false)
        assertEquals(light.bar.tintLift, GlassTabBarStyle.V3(dark = false).bar.tintLift)
        assertEquals(light.heldLift, GlassTabBarStyle.V3(dark = false).heldLift)
    }
}
