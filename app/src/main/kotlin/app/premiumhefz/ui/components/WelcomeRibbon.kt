package app.premiumhefz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.premiumhefz.domain.RibbonGeometry
import app.premiumhefz.ui.theme.Gold400

/**
 * The welcome flourish: a glowing gold string rises from the bottom of the
 * screen to the top along a swirling path, wiggling gently as it travels,
 * then fades away. Ported from the motion design in Tapless (its
 * EnergyRibbonView + RibbonGeometry) and re-implemented natively in Compose;
 * the clock is read only inside the draw block, so it never recomposes per
 * frame. It draws no background and takes no touches — purely decorative.
 */
@Composable
fun WelcomeRibbon(modifier: Modifier = Modifier) {
    val geometry = remember { RibbonGeometry() }
    val frame = remember { RibbonGeometry.Frame(geometry.capacity) }
    val clock = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    // Canvas size the guide path was last laid out for (mutated in draw only).
    val laidOut = remember { floatArrayOf(0f, 0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        clock.animateTo(
            RibbonGeometry.TOTAL_MS,
            tween(RibbonGeometry.TOTAL_MS.toInt(), easing = LinearEasing)
        )
        finished = true
    }
    if (finished) return

    Canvas(modifier.fillMaxSize()) {
        if (laidOut[0] != size.width || laidOut[1] != size.height) {
            geometry.layout(size.width, size.height, density)
            laidOut[0] = size.width
            laidOut[1] = size.height
        }
        if (geometry.frame(clock.value, frame) && frame.count >= 2) {
            drawRibbon(frame)
        }
    }
}

/** Glow layers under the bright core: (stroke width, alpha) pairs, as in the reference. */
private val GLOW_LAYERS = listOf(14f to 0.05f, 7f to 0.11f, 3.6f to 0.24f)
private const val CORE_CHUNK = 5
private const val ALPHA_LEVELS = 12

private fun DrawScope.drawRibbon(frame: RibbonGeometry.Frame) {
    val count = frame.count
    val path = Path()
    fun buildPath(s: Int, e: Int) {
        path.reset()
        path.moveTo(frame.x[s], frame.y[s])
        if (e - s == 1) {
            path.lineTo(frame.x[e], frame.y[e])
            return
        }
        for (i in s + 1 until e) {
            path.quadraticTo(
                frame.x[i], frame.y[i],
                (frame.x[i] + frame.x[i + 1]) / 2f, (frame.y[i] + frame.y[i + 1]) / 2f
            )
        }
        path.lineTo(frame.x[e], frame.y[e])
    }

    // Glow: consecutive segments grouped by quantized alpha, as in the reference.
    var i = 0
    while (i < count - 1) {
        val level = (ALPHA_LEVELS * frame.a[i] + 0.5f).toInt()
        var end = i + 1
        while (end < count - 1 && (ALPHA_LEVELS * frame.a[end] + 0.5f).toInt() == level) end++
        if (level > 0) {
            buildPath(i, end)
            val scale = level / ALPHA_LEVELS.toFloat()
            for ((widthDp, alpha) in GLOW_LAYERS) {
                drawPath(
                    path = path,
                    color = Gold400.copy(alpha = alpha * scale),
                    style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round)
                )
            }
        }
        i = end
    }

    // Bright core: short chunks stroked at their mean alpha.
    var start = 0
    while (start < count - 1) {
        val end = minOf(count - 1, start + CORE_CHUNK)
        var sum = 0f
        for (k in start..end) sum += frame.a[k]
        val mean = sum / (end - start + 1)
        if (mean > 0.01f) {
            buildPath(start, end)
            drawPath(
                path = path,
                color = Gold400.copy(alpha = mean),
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        start = end
    }
}
