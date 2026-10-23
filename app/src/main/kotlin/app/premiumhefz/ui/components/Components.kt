package app.premiumhefz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.premiumhefz.ui.theme.brand

/** Two overlapping squares: the eight-point star (Rub el Hizb) used as the app's one geometric motif. */
fun octagramPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    val k = r * 0.70710677f
    moveTo(cx - k, cy - k)
    lineTo(cx + k, cy - k)
    lineTo(cx + k, cy + k)
    lineTo(cx - k, cy + k)
    close()
    moveTo(cx, cy - r)
    lineTo(cx + r, cy)
    lineTo(cx, cy + r)
    lineTo(cx - r, cy)
    close()
}

@Composable
fun StarBadge(number: Int, modifier: Modifier = Modifier, badgeSize: Dp = 42.dp) {
    val gold = MaterialTheme.brand.gold
    Box(modifier.size(badgeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f * 0.96f
            drawPath(
                path = octagramPath(this.size.width / 2f, this.size.height / 2f, r),
                color = gold.copy(alpha = 0.6f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** A faint nested star used behind hero content. Kept to low contrast so it never competes with text. */
@Composable
fun StarWatermark(modifier: Modifier = Modifier, alpha: Float = 0.09f) {
    val gold = MaterialTheme.brand.gold
    Canvas(modifier) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension / 2f
        val stroke = Stroke(width = 1.dp.toPx())
        drawPath(octagramPath(cx, cy, r), gold.copy(alpha = alpha), style = stroke)
        drawPath(octagramPath(cx, cy, r * 0.72f), gold.copy(alpha = alpha), style = stroke)
        drawPath(octagramPath(cx, cy, r * 0.44f), gold.copy(alpha = alpha), style = stroke)
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.brand.gold
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StarWatermark(Modifier.size(72.dp), alpha = 0.45f)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** A tonal tile (no outline) with a short label, used for quick actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
