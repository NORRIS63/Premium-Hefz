package app.premiumhefz.ui.home

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.premiumhefz.ui.theme.Emerald100
import app.premiumhefz.ui.theme.Emerald500
import app.premiumhefz.ui.theme.Emerald600
import app.premiumhefz.ui.theme.Emerald850
import app.premiumhefz.ui.theme.Emerald950
import app.premiumhefz.ui.theme.Gold400
import app.premiumhefz.ui.theme.Gold500
import app.premiumhefz.ui.theme.Ivory50
import kotlin.math.roundToInt
import kotlin.random.Random

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.data.PagePack
import app.premiumhefz.data.PackStatus
import app.premiumhefz.domain.Dates
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.domain.Streaks
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.GoldProgress
import app.premiumhefz.ui.components.QuickTile
import app.premiumhefz.ui.components.StarWatermark
import app.premiumhefz.ui.theme.brand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val lastPage: Int? = null,
    val pagesReadToday: Int = 0,
    val dailyGoal: Int = 5,
    val streak: Int = 0,
    val bookmarkCount: Int = 0,
    val pack: PackStatus = PackStatus(0, PageCoordinates.PAGE_COUNT, null)
)

class HomeViewModel(repo: HefzRepository, pack: PagePack) : ViewModel() {
    private val progress = combine(repo.readingPosition, repo.pagesReadToday(), repo.activeDays) { position, read, days ->
        Triple(position, read, days)
    }
    private val meta = combine(repo.bookmarks, repo.settings, pack.status) { bookmarks, settings, status ->
        Triple(bookmarks.size, settings.dailyReadGoal, status)
    }

    val state: StateFlow<HomeUiState> = combine(progress, meta) { (position, read, days), (bookmarkCount, goal, status) ->
        HomeUiState(
            lastPage = position,
            pagesReadToday = read,
            dailyGoal = goal,
            streak = Streaks.current(days.toSet(), Dates.epochDay(System.currentTimeMillis())),
            bookmarkCount = bookmarkCount,
            pack = status
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@Composable
fun HomeScreen(
    onContinue: (Int) -> Unit,
    onOpenBrowse: (Int) -> Unit,
    onOpenBookmarks: () -> Unit,
    onImportPages: () -> Unit
) {
    val index = LocalContainer.current.quranIndex
    val vm: HomeViewModel = appViewModel { HomeViewModel(it.repository, it.pagePack) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader(state.pack)
        if (!state.pack.complete) PagesMissingCard(state.pack, onImportPages)
        ContinueCard(state.lastPage, index, onContinue)
        TodayCard(state.pagesReadToday, state.dailyGoal, state.streak)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(
                label = stringResource(R.string.home_quick_read),
                onClick = { onOpenBrowse(0) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(painterResource(R.drawable.ic_book), contentDescription = null, tint = MaterialTheme.brand.gold)
            }
            QuickTile(
                label = stringResource(R.string.home_quick_bookmarks),
                onClick = onOpenBookmarks,
                modifier = Modifier.weight(1f)
            ) {
                Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, tint = MaterialTheme.brand.gold)
            }
            QuickTile(
                label = stringResource(R.string.home_quick_goto),
                onClick = { onOpenBrowse(2) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.brand.gold)
            }
        }
    }
}

@Composable
private fun HomeHeader(pack: PackStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.startup_wordmark),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dot = if (pack.complete) MaterialTheme.brand.gold else MaterialTheme.colorScheme.onSurfaceVariant
                Box(Modifier.size(7.dp).background(dot, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (pack.complete) {
                        stringResource(R.string.home_offline_ready, pack.installed)
                    } else {
                        stringResource(R.string.home_offline_partial, pack.installed, pack.total)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PagesMissingCard(pack: PackStatus, onImportPages: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = stringResource(R.string.home_pack_missing_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (pack.installed == 0) {
                    stringResource(R.string.home_pack_missing_body)
                } else {
                    stringResource(R.string.home_pack_partial_body, pack.installed, pack.total)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onImportPages) { Text(stringResource(R.string.home_pack_action)) }
        }
    }
}

@Composable
private fun ContinueCard(lastPage: Int?, index: QuranIndex, onContinue: (Int) -> Unit) {
    val page = lastPage ?: PageCoordinates.FIRST_PAGE
    val juz = index.juzForPage(page)
    val pageInJuz = index.pageInJuz(page)
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = Gold500.copy(alpha = 0.5f),
                spotColor = Gold500.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(Emerald950)
    ) {
        NoiseGradientBackground(Modifier.matchParentSize())
        StarWatermark(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-46).dp)
        )
        Column(Modifier.padding(22.dp)) {
            Text(
                text = stringResource(if (lastPage == null) R.string.home_start else R.string.home_continue),
                style = MaterialTheme.typography.labelLarge,
                color = Gold400
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = index.surahLabel(page),
                style = MaterialTheme.typography.headlineMedium,
                color = Ivory50
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (juz != null && pageInJuz != null) {
                    stringResource(R.string.home_page_juz_fmt, page, juz.number, pageInJuz)
                } else {
                    stringResource(R.string.home_page_only_fmt, page)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Emerald100.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onContinue(page) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = Emerald950
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(stringResource(if (lastPage == null) R.string.home_start_button else R.string.home_resume))
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * The premium card backdrop: a deep emerald gradient with three large, very
 * slow drifting colour fields (emerald, teal, a whisper of gold) and a fine
 * film-grain noise layer on top — the NoiseBackground idea, drawn natively
 * on a Canvas. Cycles run 12–21 s so the motion stays restrained, and the
 * grain is a single tiny bitmap generated once, so the cost is negligible.
 */
@Composable
private fun NoiseGradientBackground(modifier: Modifier = Modifier) {
    val noise = remember { createNoiseBitmap() }
    val transition = rememberInfiniteTransition(label = "resumeCard")
    val driftA by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftA"
    )
    val driftB by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(21_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftB"
    )
    val grainShift by transition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "grainShift"
    )
    Canvas(modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Emerald850, Emerald950),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        val centerA = Offset(
            size.width * (0.12f + 0.30f * driftA),
            size.height * (0.05f + 0.45f * driftB)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Emerald600.copy(alpha = 0.38f), Color.Transparent),
                center = centerA,
                radius = size.maxDimension * 0.85f
            ),
            radius = size.maxDimension * 0.85f,
            center = centerA
        )
        val centerB = Offset(
            size.width * (0.88f - 0.30f * driftB),
            size.height * (0.95f - 0.40f * driftA)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Emerald500.copy(alpha = 0.18f), Color.Transparent),
                center = centerB,
                radius = size.maxDimension * 0.8f
            ),
            radius = size.maxDimension * 0.8f,
            center = centerB
        )
        val centerC = Offset(
            size.width * (0.75f - 0.15f * driftA),
            size.height * (0.10f + 0.20f * driftB)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Gold500.copy(alpha = 0.10f), Color.Transparent),
                center = centerC,
                radius = size.maxDimension * 0.55f
            ),
            radius = size.maxDimension * 0.55f,
            center = centerC
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.55f
            )
        )
        val slack = 56
        drawImage(
            image = noise,
            dstOffset = IntOffset(
                -slack / 2 + grainShift.roundToInt(),
                -slack / 2 + (driftB * 10f).roundToInt()
            ),
            dstSize = IntSize(size.width.toInt() + slack, size.height.toInt() + slack),
            alpha = 0.05f
        )
    }
}

private fun createNoiseBitmap(sizePx: Int = 192): ImageBitmap {
    val random = Random(20261023)
    val pixels = IntArray(sizePx * sizePx)
    for (i in pixels.indices) {
        val g = random.nextInt(256)
        pixels[i] = android.graphics.Color.rgb(g, g, g)
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bitmap.asImageBitmap()
}

@Composable
private fun TodayCard(read: Int, goal: Int, streak: Int) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.home_today),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (streak > 0) stringResource(R.string.home_streak_fmt, streak) else stringResource(R.string.home_streak_none),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.brand.gold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (read >= goal) stringResource(R.string.home_goal_done) else stringResource(R.string.home_goal_fmt, read, goal),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            GoldProgress(fraction = read.toFloat() / goal.toFloat())
        }
    }
}
