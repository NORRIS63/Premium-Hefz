package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupMotionTest {
    private fun times(from: Float, to: Float, step: Float = 5f): List<Float> {
        val out = ArrayList<Float>()
        var t = from
        while (t <= to) {
            out.add(t)
            t += step
        }
        return out
    }

    @Test
    fun theLineTravelsFromTheBottomEdgeToTheTopEdgeAndNeverBacktracks() {
        assertEquals(0f, StartupMotion.frame(0f).lineProgress, 1e-6f)
        assertEquals(0f, StartupMotion.frame(StartupMotion.LINE_START_MS).lineProgress, 1e-6f)
        assertEquals(1f, StartupMotion.frame(StartupMotion.LINE_END_MS).lineProgress, 1e-6f)
        var previous = -1f
        for (t in times(0f, StartupMotion.TOTAL_MS)) {
            val p = StartupMotion.frame(t).lineProgress
            assertTrue("progress went backwards at $t ms", p >= previous)
            previous = p
        }
        // strictly moving the whole way between start and end
        val moving = times(StartupMotion.LINE_START_MS + 10f, StartupMotion.LINE_END_MS - 10f)
        for (i in 1 until moving.size) {
            assertTrue(StartupMotion.frame(moving[i]).lineProgress > StartupMotion.frame(moving[i - 1]).lineProgress)
        }
    }

    @Test
    fun totalDurationStaysInsideTheBriefedRange() {
        assertTrue(StartupMotion.TOTAL_MS >= 1000f && StartupMotion.TOTAL_MS <= 1500f)
        assertEquals(1f, StartupMotion.frame(StartupMotion.TOTAL_MS).openProgress, 1e-6f)
        assertTrue(StartupMotion.OPEN_END_MS <= StartupMotion.TOTAL_MS)
    }

    @Test
    fun theIdentityIsRevealedOnlyAfterTheLinePassesTheMiddleOfTheScreen() {
        for (t in times(0f, StartupMotion.OPEN_START_MS)) {
            val f = StartupMotion.frame(t)
            if (f.lineProgress < 0.5f) assertEquals(0f, f.identityAlpha, 1e-6f)
        }
        assertEquals(1f, StartupMotion.frame(StartupMotion.OPEN_START_MS - 1f).identityAlpha, 1e-3f)
        assertEquals(0f, StartupMotion.frame(StartupMotion.OPEN_START_MS + 160f).identityAlpha, 1e-6f)
    }

    @Test
    fun theCoverStaysClosedUntilTheLineHasArrivedAndFaded() {
        assertTrue(StartupMotion.LINE_END_MS + StartupMotion.LINE_FADE_OUT_MS <= StartupMotion.OPEN_START_MS)
        for (t in times(0f, StartupMotion.OPEN_START_MS)) assertEquals(0f, StartupMotion.frame(t).openProgress, 1e-6f)
        for (t in times(StartupMotion.OPEN_START_MS, StartupMotion.TOTAL_MS)) {
            assertTrue(StartupMotion.frame(t).openProgress >= StartupMotion.frame(t - 5f).openProgress)
        }
    }

    @Test
    fun theLineFadesInAtTheBottomAndOutAtTheTop() {
        assertEquals(0f, StartupMotion.frame(0f).lineAlpha, 1e-6f)
        assertEquals(1f, StartupMotion.frame(120f).lineAlpha, 1e-6f)
        assertEquals(1f, StartupMotion.frame(StartupMotion.LINE_END_MS).lineAlpha, 1e-6f)
        assertEquals(0f, StartupMotion.frame(StartupMotion.LINE_END_MS + StartupMotion.LINE_FADE_OUT_MS).lineAlpha, 1e-6f)
    }

    @Test
    fun reducedMotionShowsTheIdentityWithoutAnyLine() {
        val f = StartupMotion.reducedFrame
        assertEquals(1f, f.identityAlpha, 1e-6f)
        assertEquals(0f, f.lineAlpha, 1e-6f)
        assertEquals(0f, f.openProgress, 1e-6f)
    }
}
