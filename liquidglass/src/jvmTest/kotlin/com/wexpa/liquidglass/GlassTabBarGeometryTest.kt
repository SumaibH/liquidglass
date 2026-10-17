package com.wexpa.liquidglass

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The item geometry a [GlassTabBar] drags against, pinned on the JVM.
 *
 * Two of these already shipped as bugs in the app before the component existed: a drag that
 * anchored at the wrong point could not reach the last item, and an index mapping off by a slot
 * would have looked identical from the outside. Neither needs a device to catch.
 */
class GlassTabBarGeometryTest {

    private val g = GlassTabBarGeometry(barWidth = 1342f, inset = 21f, count = 5)

    @Test
    fun testSlotsShareTheBarInsideTheInset() {
        assertEquals((1342f - 42f) / 5f, g.slotWidth, 0.001f)
        assertEquals(21f + g.slotWidth / 2f, g.firstCentre, 0.001f)
        assertEquals(1342f - 21f - g.slotWidth / 2f, g.lastCentre, 0.001f)
    }

    @Test
    fun testCentresAreSymmetricAboutTheBar() {
        for (i in 0 until 5) {
            assertEquals(1342f - g.centreOf(i), g.centreOf(4 - i), 0.001f, "centre $i")
        }
    }

    @Test
    fun testNearestIndexRoundTripsEveryCentreAndClampsTheEnds() {
        for (i in 0 until 5) assertEquals(i, g.nearestIndex(g.centreOf(i)))
        assertEquals(0, g.nearestIndex(-500f))
        assertEquals(4, g.nearestIndex(5000f))
        // Exactly on a boundary belongs to the item that starts there.
        assertEquals(1, g.nearestIndex(21f + g.slotWidth))
    }

    @Test
    fun testAFlickCarriesOneItemAndNeverPastTheEnds() {
        assertEquals(3, g.projected(lastIndex = 2, releasedVelocity = 900f, flingVelocity = 420f))
        assertEquals(1, g.projected(lastIndex = 2, releasedVelocity = -900f, flingVelocity = 420f))
        assertEquals(2, g.projected(lastIndex = 2, releasedVelocity = 300f, flingVelocity = 420f))
        assertEquals(4, g.projected(lastIndex = 4, releasedVelocity = 5000f, flingVelocity = 420f))
        assertEquals(0, g.projected(lastIndex = 0, releasedVelocity = -5000f, flingVelocity = 420f))
    }

    /**
     * [GlassTabBarStyle.slotWidth] against WhatsApp's iOS 26 bar (JGEY2190 50-74 s, 3x, measured
     * 17 October): a 348pt bar (x 63..1107), its tab centres at 193/370/584.5/799.25/976 px and its
     * resting pill 78pt wide, 4pt inside the bar's end when an end tab is selected. 71.5pt slots
     * are the measured inner spacing (214.6 px); the end tabs close up to where their pill fits.
     */
    @Test
    fun testFixedSlotsReproduceWhatsAppsCentres() {
        val style = GlassTabBarStyle.Calm().copy(slotWidth = 71.5.dp, pillWidth = 78.dp, pillInset = 4.dp)
        val w = glassTabBarGeometry(Density(3f), 1044f, style, 5)
        assertTrue(w.fixed)
        val measured = floatArrayOf(193f, 370f, 584.5f, 799.25f, 976f)
        for (i in 0 until 5) assertEquals(measured[i], 63f + w.centreOf(i), 2f, "centre $i")
        for (i in 0 until 5) assertEquals(i, w.nearestIndex(w.centreOf(i)))
        // Hit targets never overlap: each half-width is at most half the gap to a neighbour.
        for (i in 0 until 4) {
            assertTrue(w.centreOf(i) + w.itemHalfWidth(i) <= w.centreOf(i + 1) - w.itemHalfWidth(i + 1) + 0.001f)
        }
        // The selector's bar agrees with the row about every centre.
        val bar = GlassSelectorBar(1044f, 192f, 18f, 5, 96f, 117f, 84f, 84f, 110f, 150f, 3f, 600f,
            fixedSlot = w.fixedSlot, endReach = w.endReach)
        for (i in 0 until 5) assertEquals(w.centreOf(i), bar.centreOf(i), 0.001f)
    }

    @Test
    fun testUnspecifiedSlotWidthKeepsTheSharedOutSlots() {
        val d = glassTabBarGeometry(Density(3f), 1342f, GlassTabBarStyle.Calm().copy(contentPadding = 7.dp), 5)
        assertEquals(GlassTabBarGeometry(barWidth = 1342f, inset = 21f, count = 5), d)
    }
}
