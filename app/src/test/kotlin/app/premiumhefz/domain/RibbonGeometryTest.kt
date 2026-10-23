package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RibbonGeometryTest {

    private fun laidOut(): RibbonGeometry {
        val g = RibbonGeometry()
        g.layout(1000f, 2000f, 2f)
        return g
    }

    @Test
    fun smoothEndpointsAndMidpoint() {
        assertEquals(0f, RibbonGeometry.smooth(0f, 1f, 0f), 1e-6f)
        assertEquals(1f, RibbonGeometry.smooth(0f, 1f, 1f), 1e-6f)
        assertEquals(0.5f, RibbonGeometry.smooth(0f, 1f, 0.5f), 1e-6f)
    }

    @Test
    fun frameEndsAtTotalDuration() {
        val g = laidOut()
        val frame = RibbonGeometry.Frame(g.capacity)
        assertFalse(g.frame(RibbonGeometry.TOTAL_MS, frame))
        assertEquals(0, frame.count)
    }

    @Test
    fun ribbonStartsNearTheBottom() {
        val g = laidOut()
        val frame = RibbonGeometry.Frame(g.capacity)
        assertTrue(g.frame(500f, frame))
        assertTrue(frame.count >= 2)
        // First points are on the lower part of a 2000px-tall canvas.
        assertTrue(frame.y[0] > 1000f)
    }

    @Test
    fun headReachesTheUpperHalfByMidAnimation() {
        val g = laidOut()
        val frame = RibbonGeometry.Frame(g.capacity)
        assertTrue(g.frame(4500f, frame))
        assertTrue(frame.count >= 2)
        assertTrue(frame.y[frame.count - 1] < 1000f)
    }

    @Test
    fun alphasStayWithinRangeAcrossTheAnimation() {
        val g = laidOut()
        val frame = RibbonGeometry.Frame(g.capacity)
        var t = 0f
        while (t < RibbonGeometry.TOTAL_MS) {
            assertTrue(g.frame(t, frame))
            for (i in 0 until frame.count) {
                assertTrue("alpha ${frame.a[i]} at t=$t", frame.a[i] in 0f..1f)
            }
            t += 250f
        }
    }

    @Test
    fun frameBeforeLayoutIsEmpty() {
        val g = RibbonGeometry()
        val frame = RibbonGeometry.Frame(g.capacity)
        assertFalse(g.frame(100f, frame))
        assertEquals(0, frame.count)
    }
}
