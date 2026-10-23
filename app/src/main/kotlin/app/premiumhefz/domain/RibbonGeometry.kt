package app.premiumhefz.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry of the welcome "energy ribbon": a string of light that rises along
 * a swirling guide path from near the bottom of the screen to just above the
 * top, drawn as a travelling segment whose head leads and whose tail follows
 * over [TOTAL_MS]. The design (and every constant below) is a faithful port
 * of the motion in Tapless, which Shafi chose as the reference; this class is
 * pure math so it can be unit-tested off-device.
 *
 * The guide path starts at the origin (default: 90% of the height), wanders
 * gently, makes one full calligraphic loop about two-thirds of the way up,
 * and exits through the top edge.
 */
class RibbonGeometry(private val samples: Int = DEFAULT_SAMPLES) {

    /** One computed frame: point positions plus a per-point alpha in [0, 1]. */
    class Frame(capacity: Int) {
        val x = FloatArray(capacity)
        val y = FloatArray(capacity)
        val a = FloatArray(capacity)
        var count: Int = 0
    }

    private var w = 0f
    private var h = 0f
    private var d = 1f
    private val bx = FloatArray(samples + 1)
    private val by = FloatArray(samples + 1)
    private val nx = FloatArray(samples + 1)
    private val ny = FloatArray(samples + 1)
    private var avgStep = 1f

    val capacity: Int get() = samples + 1

    /** Computes the guide path for a canvas of [width] x [height] px. */
    fun layout(width: Float, height: Float, density: Float, originY: Float = height * 0.9f) {
        w = width
        h = height
        d = density
        val topY = -0.06f * h
        val swirl = 0.165f * w
        for (i in 0..samples) {
            val f = i.toFloat() / samples
            val baseX = w * (0.5f + 0.31f * sin(6.9115043f * f - 1.9f))
            val ang = TWO_PI * smooth(0.56f, 0.77f, f)
            bx[i] = baseX + sin(ang) * swirl -
                w * 0.1f * (smooth(0.74f, 0.84f, f) * (1f - smooth(0.9f, 1.0f, f)))
            by[i] = (originY + (topY - originY) * f) - (1f - cos(ang)) * swirl
        }
        var total = 0f
        for (i in 0..samples) {
            val prev = max(i - 1, 0)
            val next = min(i + 1, samples)
            val dx = bx[next] - bx[prev]
            val dy = by[next] - by[prev]
            val len = max(sqrt(dx * dx + dy * dy), 0.001f)
            nx[i] = -dy / len
            ny[i] = dx / len
            if (i > 0) {
                val sx = bx[i] - bx[i - 1]
                val sy = by[i] - by[i - 1]
                total += sqrt(sx * sx + sy * sy)
            }
        }
        avgStep = max(total / samples, 0.5f)
    }

    /** Fills [out] for time [tMs]; returns false once the animation is over. */
    fun frame(tMs: Float, out: Frame): Boolean {
        if (w <= 0f || tMs >= TOTAL_MS) {
            out.count = 0
            return false
        }
        val head = hermite(tMs, HEAD_T, HEAD_U, HEAD_M)
        val tail = max(0f, head - hermite(tMs, LEN_T, LEN_U, LEN_M))
        val envelope = smooth(0f, 1100f, tMs) * (1f - smooth(5000f, 6900f, tMs))
        val iTail = (samples * tail).toInt().coerceIn(0, samples - 1)
        val iHead = (samples * head).toInt().coerceIn(iTail + 1, samples)
        val span = (iHead - iTail).toFloat()
        val k1 = TWO_PI * avgStep / (112f * d)
        val k2 = TWO_PI * avgStep / (50f * d)
        val omega = 2.8274333f * tMs / 1000f
        val amp = 4.2f * d * (0.62f + 0.38f * sin(TWO_PI * tMs / 2600f + 0.8f))
        val swayX = w * 0.01f * sin(TWO_PI * tMs / 3300f)
        val swayY = h * 0.004f * cos(TWO_PI * tMs / 4100f)
        var i = 0
        for (j in iTail..iHead) {
            val f5 = (j - iTail) / span
            val wobble = amp * sin(PI.toFloat() * f5) *
                (0.72f * sin(j * k1 - omega) + 0.28f * sin(j * k2 + 1.7f * omega + 1.3f))
            out.x[i] = bx[j] + nx[j] * wobble + (0.4f + 0.6f * f5) * swayX
            out.y[i] = by[j] + ny[j] * wobble + swayY
            out.a[i] = smooth(0f, 0.38f, f5) * envelope * (1f - smooth(0.8f, 1.0f, f5))
            i++
        }
        out.count = i
        return true
    }

    companion object {
        const val TOTAL_MS = 7000f
        private const val DEFAULT_SAMPLES = 520
        private const val TWO_PI = 6.2831855f

        // Head progress along the path over time (hermite keys: time, value, slope).
        private val HEAD_T = floatArrayOf(0f, 2000f, 4000f, 5000f, 7000f)
        private val HEAD_U = floatArrayOf(0f, 0.292f, 0.76f, 0.97f, 1.04f)
        private val HEAD_M = floatArrayOf(7.0e-5f, 1.5e-4f, 2.3e-4f, 8.0e-5f, 0f)

        // Visible length of the ribbon over time, same encoding.
        private val LEN_T = floatArrayOf(0f, 2000f, 3500f, 5000f, 7000f)
        private val LEN_U = floatArrayOf(0f, 0.292f, 0.36f, 0.36f, 0.1f)
        private val LEN_M = floatArrayOf(0f, 7.0e-5f, 2.0e-5f, -2.0e-5f, -4.0e-5f)

        /** Smoothstep between [a] and [b]. */
        fun smooth(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        /** Cubic hermite interpolation through keyframes [ts] -> [vs] with slopes [ms]. */
        fun hermite(t: Float, ts: FloatArray, vs: FloatArray, ms: FloatArray): Float {
            if (t <= ts[0]) return vs[0]
            if (t >= ts[ts.size - 1]) return vs[vs.size - 1]
            var i = 0
            while (t > ts[i + 1]) i++
            val seg = ts[i + 1] - ts[i]
            val u = (t - ts[i]) / seg
            val u2 = u * u
            val u3 = u2 * u
            return vs[i] * ((2f * u3) - (3f * u2) + 1f) +
                ((u3 - (2f * u2) + u) * seg * ms[i]) +
                vs[i + 1] * ((-2f * u3) + (3f * u2)) +
                ((u3 - u2) * seg * ms[i + 1])
        }
    }
}
