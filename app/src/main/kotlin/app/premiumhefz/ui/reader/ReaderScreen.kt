package app.premiumhefz.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.data.PageBitmaps
import app.premiumhefz.data.PagePack
import app.premiumhefz.data.PageTone
import app.premiumhefz.data.hasPage
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.browse.GoToPageForm
import app.premiumhefz.ui.theme.Emerald950
import app.premiumhefz.ui.theme.Ink900
import app.premiumhefz.ui.theme.Ivory100
import app.premiumhefz.ui.theme.Ivory200
import app.premiumhefz.ui.theme.LocalIsDarkTheme
import app.premiumhefz.ui.theme.NightInk
import app.premiumhefz.ui.theme.NightPaper
import app.premiumhefz.ui.theme.brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ReaderViewModel(private val repo: HefzRepository) : ViewModel() {
    val bookmarkedPages: StateFlow<Set<Int>> =
        repo.bookmarkedPages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Calls [onResult] with true if the page is bookmarked after the toggle. */
    fun toggleBookmark(page: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.toggleBookmark(page)) }
    }

    fun onPageSettled(page: Int) {
        viewModelScope.launch { repo.saveReadingPosition(page) }
    }

    fun recordRead(page: Int) {
        viewModelScope.launch { repo.recordRead(page) }
    }
}

/** [background] is the page image's paper colour; [margin] is the calm surround the page rests on. */
private class ToneStyle(val background: Color, val filter: ColorFilter?, val margin: Color)

/** Maps the page image's white paper to [paper] and its black ink to [ink]. Only affects drawing, never the file. */
private fun toneMatrix(paper: Color, ink: Color): ColorMatrix = ColorMatrix(
    floatArrayOf(
        paper.red - ink.red, 0f, 0f, 0f, ink.red * 255f,
        0f, paper.green - ink.green, 0f, 0f, ink.green * 255f,
        0f, 0f, paper.blue - ink.blue, 0f, ink.blue * 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

@Composable
private fun rememberToneStyle(tone: PageTone): ToneStyle {
    val dark = LocalIsDarkTheme.current
    return remember(tone, dark) {
        val resolved = if (tone == PageTone.AUTO) (if (dark) PageTone.NIGHT else PageTone.PAPER) else tone
        when (resolved) {
            PageTone.ORIGINAL -> ToneStyle(Color.White, null, Ivory200)
            PageTone.PAPER -> ToneStyle(Ivory100, ColorFilter.colorMatrix(toneMatrix(paper = Ivory100, ink = Ink900)), Ivory200)
            else -> ToneStyle(NightPaper, ColorFilter.colorMatrix(toneMatrix(paper = NightPaper, ink = NightInk)), Emerald950)
        }
    }
}

@Composable
fun ReaderScreen(startPage: Int, onBack: () -> Unit, onImportPages: () -> Unit) {
    val container = LocalContainer.current
    val vm: ReaderViewModel = appViewModel { ReaderViewModel(it.repository) }
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val bookmarked by vm.bookmarkedPages.collectAsStateWithLifecycle()
    val packStatus by container.pagePack.status.collectAsStateWithLifecycle()
    val index = container.quranIndex
    val pack = container.pagePack
    val tone = rememberToneStyle(settings.pageTone)

    val pagerState = rememberPagerState(
        initialPage = PageCoordinates.pagerIndex(PageCoordinates.coerce(startPage))
    ) { PageCoordinates.PAGE_COUNT }
    val currentPage = PageCoordinates.pageForPagerIndex(pagerState.currentPage)

    var barsVisible by rememberSaveable { mutableStateOf(true) }
    // Bumped on every page-slider move so the bars stay up while the reader scrubs.
    var scrubInteraction by remember { mutableIntStateOf(0) }
    var zoomed by remember { mutableStateOf(false) }
    var showGoTo by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Let the page have the screen after a moment; a tap brings the bars back, and
    // scrubbing the page slider keeps them up while it is in use.
    LaunchedEffect(barsVisible, scrubInteraction) {
        if (barsVisible) {
            delay(3_000)
            barsVisible = false
        }
    }

    // The last-read position follows the page the pager settles on.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { vm.onPageSettled(PageCoordinates.pageForPagerIndex(it)) }
    }
    // A page counts as read for today once it has stayed on screen for a few seconds.
    LaunchedEffect(pagerState.settledPage) {
        delay(3_000)
        vm.recordRead(PageCoordinates.pageForPagerIndex(pagerState.settledPage))
    }

    fun toggleBookmarkFor(page: Int) {
        vm.toggleBookmark(page) { nowBookmarked ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                snackbarHost.currentSnackbarData?.dismiss()
                snackbarHost.showSnackbar(
                    context.getString(
                        if (nowBookmarked) R.string.reader_bookmarked_fmt else R.string.reader_bookmark_removed_fmt,
                        page
                    )
                )
            }
        }
    }

    Box(Modifier.fillMaxSize().background(tone.margin)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            userScrollEnabled = !zoomed,
            beyondViewportPageCount = 1,
            key = { it }
        ) { pagerIndex ->
            val page = PageCoordinates.pageForPagerIndex(pagerIndex)
            ReaderPage(
                page = page,
                pack = pack,
                packVersion = packStatus.installed,
                tone = tone,
                onTap = { barsVisible = !barsVisible },
                onLongPress = { toggleBookmarkFor(page) },
                onZoomChanged = { zoomed = it },
                onImportPages = onImportPages
            )
        }

        AnimatedVisibility(
            visible = barsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            val juz = index.juzForPage(currentPage)
            val pageInJuz = index.pageInJuz(currentPage)
            ReaderTopBar(
                title = index.surahLabel(currentPage),
                subtitle = if (juz != null && pageInJuz != null) {
                    stringResource(R.string.reader_subtitle_fmt, currentPage, juz.number, pageInJuz)
                } else {
                    stringResource(R.string.reader_subtitle_opening_fmt, currentPage)
                },
                bookmarked = currentPage in bookmarked,
                onBack = onBack,
                onToggleBookmark = { toggleBookmarkFor(currentPage) },
                onGoTo = { showGoTo = true }
            )
        }

        AnimatedVisibility(
            visible = barsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            ReaderBottomBar(
                currentPage = currentPage,
                pagerIndex = pagerState.currentPage,
                onScrub = { index ->
                    scope.launch { pagerState.scrollToPage(index) }
                },
                onScrubInteraction = { scrubInteraction++ }
            )
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (barsVisible) 76.dp else 16.dp
                )
        )
    }

    if (showGoTo) {
        AlertDialog(
            onDismissRequest = { showGoTo = false },
            title = { Text(stringResource(R.string.goto_title)) },
            text = {
                GoToPageForm(onOpen = { page ->
                    showGoTo = false
                    scope.launch { pagerState.scrollToPage(PageCoordinates.pagerIndex(page)) }
                })
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showGoTo = false }) { Text(stringResource(R.string.action_close)) } }
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onGoTo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onGoTo) { Text(stringResource(R.string.reader_go_to)) }
        IconButton(onClick = onToggleBookmark) {
            Icon(
                painter = painterResource(if (bookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
                contentDescription = stringResource(if (bookmarked) R.string.cd_bookmark_remove else R.string.cd_bookmark_add),
                tint = MaterialTheme.brand.gold
            )
        }
    }
}

/**
 * Slim bottom chrome: a page slider for scrubbing through the book. It shares the top bar's
 * fade and auto-hide, so while reading, nothing but the page is on screen.
 */
@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    pagerIndex: Int,
    onScrub: (Int) -> Unit,
    onScrubInteraction: () -> Unit
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentPage.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = dragValue ?: pagerIndex.toFloat(),
            onValueChange = {
                dragValue = it
                onScrubInteraction()
            },
            onValueChangeFinished = {
                dragValue?.let { onScrub(it.roundToInt()) }
                dragValue = null
            },
            valueRange = 0f..(PageCoordinates.PAGE_COUNT - 1).toFloat(),
            steps = PageCoordinates.PAGE_COUNT - 2,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.brand.gold.copy(alpha = 0.7f),
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                thumbColor = MaterialTheme.brand.gold,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
        Text(
            text = PageCoordinates.LAST_PAGE.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReaderPage(
    page: Int,
    pack: PagePack,
    packVersion: Int,
    tone: ToneStyle,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onImportPages: () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = PageBitmaps.peek(page), page, packVersion) {
        value = PageBitmaps.load(page, pack)
    }
    ZoomableBox(onTap = onTap, onLongPress = onLongPress, onZoomChanged = onZoomChanged) {
        // The page rests on a calm margin like a mushaf on a stand: the margin is a
        // shade deeper than the page paper, and a hairline gold frame hugs the page
        // itself. The frame is sized to the displayed image, never to the screen,
        // so it never crops or stretches the page.
        BoxWithConstraints(Modifier.fillMaxSize().background(tone.margin)) {
            val image = bitmap
            if (image != null) {
                val density = LocalDensity.current
                val aspect = image.width.toFloat() / image.height.toFloat()
                val widthPx = minOf(constraints.maxWidth.toFloat(), constraints.maxHeight * aspect)
                val heightPx = widthPx / aspect
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(
                            width = with(density) { widthPx.toDp() },
                            height = with(density) { heightPx.toDp() }
                        )
                        .shadow(8.dp, RectangleShape, clip = false)
                        .border(1.dp, MaterialTheme.brand.hairline, RectangleShape)
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = stringResource(R.string.cd_quran_page_fmt, page),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.High,
                        colorFilter = tone.filter
                    )
                }
            } else if (!pack.hasPage(page)) {
                MissingPage(page, onImportPages)
            }
        }
    }
}

@Composable
private fun MissingPage(page: Int, onImportPages: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.reader_missing_title_fmt, page),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.reader_missing_body),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onImportPages) { Text(stringResource(R.string.home_pack_action)) }
    }
}

/**
 * Pinch to zoom, double-tap to toggle zoom, drag to pan while zoomed. A single finger at normal scale is
 * left alone so the pager can swipe; a long press is reported separately (used to bookmark).
 */
@Composable
private fun ZoomableBox(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // The pointer handlers below are keyed on Unit, so they read the latest callbacks through these.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = { currentOnLongPress() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.2f
                        }
                        currentOnZoomChanged(scale > 1.01f)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                            val pan = event.calculatePan()
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                            scale = newScale
                            currentOnZoomChanged(scale > 1.01f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        content()
    }
}
