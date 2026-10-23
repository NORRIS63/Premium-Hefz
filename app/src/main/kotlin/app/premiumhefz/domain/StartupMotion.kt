package app.premiumhefz.domain

/**
 * The opening animation as a pure function of time, so its timing and direction can be unit tested.
 *
 *   90-820 ms   a hairline travels from the bottom edge to the top edge, leaving an emerald-and-gold trail
 *   470-820 ms  the app identity is revealed as the line passes the middle of the screen
 *   820-900 ms  the line fades out at the top edge
 *   900-1290 ms the emerald cover splits along the line's path and slides apart, revealing the home screen
 */
object StartupMotion {
    const val TOTAL_MS = 1300f
    const val LINE_START_MS = 90f
    const val LINE_END_MS = 820f
    const val LINE_FADE_OUT_MS = 80f
    const val OPEN_START_MS = 900f
    const val OPEN_END_MS = 1290f

    data class Frame(
        /** 0 = the line is at the bottom edge, 1 = it has reached the top edge. */
        val lineProgress: Float,
        val lineAlpha: Float,
        /** 0 = full trail, 1 = trail gone. */
        val trailRetract: Float,
        val identityAlpha: Float,
        /** 0 = cover closed, 1 = the two halves are fully apart. */
        val openProgress: Float
    )

    /** What is shown instead of the animation when motion is reduced: the identity, then a plain fade. */
    val reducedFrame = Frame(lineProgress = 1f, lineAlpha = 0f, trailRetract = 1f, identityAlpha = 1f, openProgress = 0f)

    fun frame(tMs: Float): Frame {
        val lineProgress = bezier(0.40f, 0.0f, 0.20f, 1.0f, ((tMs - LINE_START_MS) / (LINE_END_MS - LINE_START_MS)).coerceIn(0f, 1f))
        val fadeIn = (tMs / 120f).coerceIn(0f, 1f)
        val fadeOut = 1f - ((tMs - LINE_END_MS) / LINE_FADE_OUT_MS).coerceIn(0f, 1f)
        val identityIn = smooth((lineProgress - 0.5f) / 0.3f)
        val identityOut = 1f - smooth((tMs - OPEN_START_MS) / 160f)
        return Frame(
            lineProgress = lineProgress,
            lineAlpha = fadeIn * fadeOut,
            trailRetract = smooth((tMs - LINE_END_MS) / 220f),
            identityAlpha = identityIn * identityOut,
            openProgress = bezier(0.70f, 0.0f, 0.20f, 1.0f, ((tMs - OPEN_START_MS) / (OPEN_END_MS - OPEN_START_MS)).coerceIn(0f, 1f))
        )
    }

    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** CSS-style cubic-bezier(x1, y1, x2, y2) easing, solved by bisection. */
    private fun bezier(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (curve(x1, x2, mid) < x) lo = mid else hi = mid
        }
        return curve(y1, y2, (lo + hi) / 2f)
    }

    private fun curve(a: Float, b: Float, u: Float): Float {
        val v = 1f - u
        return 3f * v * v * u * a + 3f * v * u * u * b + u * u * u
    }
}
