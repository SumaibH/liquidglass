package com.wexpa.liquidglass

import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [GlassPullShape.Free]: iOS 26's Phone search button pulled off its place (IMG_6756 in display
 * colours, 2026-10-15). The default shapes are unchanged.
 */
class GlassFreePullTest {
    private val free = GlassInteraction.Pullable.copy(pullShape = GlassPullShape.Free)

    @Test fun aFreeBodyFollowsTheFingerWhileAdaptiveStaysAnchored() {
        // A 62 dp button at 3 px/dp, pressed 1.258, pulled 550 px up (resisted): IMG_6756's 110 px rise
        // at Pullable's follow of 0.2 (0.35 before 18 October reached it at 314 px).
        val args = floatArrayOf(186f, 186f, 93f, 3f, 1.258f, 1.258f, 0f, -550f, 117f)
        fun pull(i: GlassInteraction) = glassMaterialPull(args[0], args[1], args[2], args[3], args[4], args[5],
            args[6], args[7], args[8], i)
        val f = pull(free)
        assertEquals(-110f, f.translationY, 1f, "the body rises with the finger")
        // The measured shape at that rise: 297-303 px tall and 158-166 px wide on a 234 px pressed body.
        assertEquals(1.29f, f.along, .02f); assertEquals(.71f, f.across, .02f)
        val adaptive = pull(GlassInteraction.Pullable)
        assertEquals(0f, adaptive.translationY, "Adaptive keeps the layout centre")
        assertEquals(GlassPullShape.Adaptive, GlassInteraction.Pullable.pullShape)
        assertEquals(1f, GlassInteraction().pullLiftShare)
    }

    @Test fun aLandingSquashesAgainstTheRestingEdgeAwayFromThePull() {
        // Pulled up (-90 degrees), carried back 32 px past rest: the near edge leads by 32 px while
        // the far edge holds, so the body is 32 px shorter and bulges 0.2 of that across.
        val overshoot = GlassPullDeformation(1f, 1f, -90f, 0f, 32f)
        val landed = glassFreeLanding(overshoot, -90f, 93f)
        val n = landed.angleDegrees * Math.PI.toFloat() / 180f
        val centre = landed.translationX * cos(n) + landed.translationY * sin(n)
        val half = 93f * landed.along
        assertEquals(-93f, centre - half, .5f, "far (lower) edge stays at rest")
        assertEquals(186f - 32f, 2f * half, .5f, "IMG_6756: 154 px against 186")
        assertEquals(1f + .2f * (32f / 186f), landed.across, .01f)
        // Not past rest: unchanged.
        val lifted = GlassPullDeformation(1.29f, .71f, -90f, 0f, -110f)
        assertEquals(lifted, glassFreeLanding(lifted, -90f, 93f))
        // No anchor yet: unchanged.
        assertEquals(overshoot, glassFreeLanding(overshoot, Float.NaN, 93f))
    }

    @Test fun theBarsCopyOfAStretchedAccessoryStaysInsideIt() {
        for ((a, b) in listOf(83f to 151f, 151f to 83f, 92f to 77f, 93f to 93f)) {
            val (hw, hh, r) = glassInscribedStadium(a, b).let { Triple(it[0], it[1], it[2]) }
            assertTrue(r <= minOf(hw, hh) + 1e-3f)
            // Sample the stadium's outline: every point inside the ellipse.
            for (i in 0 until 72) {
                val t = i * (2 * Math.PI / 72)
                val long = hw >= hh
                val c = (if (long) hw else hh) - r
                val sx = c * Math.signum(cos(t)) + r * cos(t)
                val sy = r * sin(t)
                val x = if (long) sx else sy; val y = if (long) sy else sx
                assertTrue(x * x / (a * a) + y * y / (b * b) <= 1.001, "($a, $b): ($x, $y) outside")
            }
            assertTrue(hw >= .8f * a && hh >= .8f * b, "($a, $b) -> $hw x $hh: too small")
        }
    }
}
