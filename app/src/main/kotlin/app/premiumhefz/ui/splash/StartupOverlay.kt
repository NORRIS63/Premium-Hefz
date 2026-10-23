package app.premiumhefz.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.premiumhefz.R
import app.premiumhefz.domain.StartupMotion
import app.premiumhefz.ui.components.octagramPath
import app.premiumhefz.ui.theme.Emerald300
import app.premiumhefz.ui.theme.Emerald900
import app.premiumhefz.ui.theme.Gold400
import app.premiumhefz.ui.theme.Gold500
import app.premiumhefz.ui.theme.Ivory100
import kotlinx.coroutines.delay

private val GoldHighlight = Color(0xFFF3E4AE)

/**
 * The signature opening: on a deep emerald field a hairline rises from the bottom edge to the top edge,
 * revealing the app identity as it passes, then the field parts along the line's path to show the home
 * screen. All timing lives in [StartupMotion]. The overlay draws in a single Canvas and reads the clock
 * only inside draw/graphics blocks, so it never recomposes per frame.
 */
@Composable
fun StartupOverlay(reducedMotion: Boolean, onFinished: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(350)
            fade.animateTo(0f, tween(durationMillis = 180))
        } else {
            clock.animateTo(
                targetValue = StartupMotion.TOTAL_MS,
                animationSpec = tween(durationMillis = StartupMotion.TOTAL_MS.toInt(), easing = LinearEasing)
            )
        }
        latestOnFinished()
    }

    fun frameNow() = if (reducedMotion) StartupMotion.reducedFrame else StartupMotion.frame(clock.value)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawStartup(frameNow())
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val frame = frameNow()
                    alpha = frame.identityAlpha
                    translationY = (1f - frame.identityAlpha) * 14.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(58.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f * 0.92f
                drawPath(octagramPath(c.x, c.y, r), Gold400, style = Stroke(width = 1.4.dp.toPx()))
                drawPath(octagramPath(c.x, c.y, r * 0.62f), Gold400.copy(alpha = 0.6f), style = Stroke(width = 1.dp.toPx()))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.startup_wordmark),
                color = Ivory100,
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 1.6.sp)
            )
        }
    }
}

private fun DrawScope.drawStartup(frame: StartupMotion.Frame) {
    val w = size.width
    val h = size.height
    val half = w / 2f
    val shift = half * frame.openProgress

    // Two emerald halves that meet on the centre line and slide apart at the end.
    drawRect(Emerald900, topLeft = Offset(-shift, 0f), size = Size(half + 0.75f, h))
    drawRect(Emerald900, topLeft = Offset(half + shift - 0.75f, 0f), size = Size(half + 0.75f, h))

    // A faint afterglow on the two edges of the seam while it opens.
    if (frame.openProgress > 0f && frame.openProgress < 1f) {
        val edge = 1.dp.toPx()
        val glow = Gold500.copy(alpha = 0.5f * (1f - frame.openProgress))
        drawRect(glow, topLeft = Offset(half - shift - edge, 0f), size = Size(edge, h))
        drawRect(glow, topLeft = Offset(half + shift, 0f), size = Size(edge, h))
    }

    // The line itself: its head starts at the bottom edge and finishes at the top edge.
    if (frame.lineAlpha > 0f) {
        val minLength = 36.dp.toPx()
        val headY = (h - minLength) * (1f - frame.lineProgress)
        val trailLength = h * 0.42f * (1f - frame.trailRetract)
        val bodyLength = maxOf(minLength, trailLength)
        val brush = Brush.verticalGradient(
            0f to GoldHighlight,
            0.14f to Gold500,
            0.6f to Emerald300.copy(alpha = 0.45f),
            1f to Color.Transparent,
            startY = headY,
            endY = headY + bodyLength
        )
        val lineWidth = 1.5.dp.toPx()
        val haloWidth = 10.dp.toPx()
        drawRect(brush, Offset(half - haloWidth / 2f, headY), Size(haloWidth, bodyLength), alpha = 0.10f * frame.lineAlpha)
        drawRect(brush, Offset(half - lineWidth / 2f, headY), Size(lineWidth, bodyLength), alpha = frame.lineAlpha)
    }
}
