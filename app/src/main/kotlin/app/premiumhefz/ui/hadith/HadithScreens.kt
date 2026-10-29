package app.premiumhefz.ui.hadith

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.HadithBookData
import app.premiumhefz.data.HadithRepository
import app.premiumhefz.domain.HADITH_BOOKS
import app.premiumhefz.domain.HadithBookMeta
import app.premiumhefz.domain.HadithEntry
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.theme.brand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------- Detail ----------

class HadithDetailViewModel(
    private val repo: HadithRepository,
    private val bookId: String,
    private val number: String
) : ViewModel() {
    sealed interface DetailState {
        data object Loading : DetailState
        data class Ready(val entry: HadithEntry, val meta: HadithBookMeta, val sectionTitle: String?) : DetailState
        data object Failed : DetailState
    }

    private val stateFlow = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = stateFlow.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            stateFlow.value = DetailState.Loading
            val data = repo.bookData(bookId)
            val entry = data?.entries?.firstOrNull { it.number == number }
            stateFlow.value = if (data != null && entry != null) {
                DetailState.Ready(entry, data.meta, data.sections[entry.section])
            } else {
                DetailState.Failed
            }
        }
    }
}

@Composable
fun HadithDetailScreen(bookId: String, number: String, onBack: () -> Unit) {
    val vm: HadithDetailViewModel = appViewModel { HadithDetailViewModel(it.hadithRepository, bookId, number) }
    val state by vm.state.collectAsStateWithLifecycle()
    val meta = HADITH_BOOKS.firstOrNull { it.id == bookId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = meta?.title ?: stringResource(R.string.hadith_library_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        when (val s = state) {
            is HadithDetailViewModel.DetailState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.brand.gold)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.hadith_loading_book),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is HadithDetailViewModel.DetailState.Failed -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.hadith_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::retry) { Text(stringResource(R.string.hadith_retry)) }
                }
            }
            is HadithDetailViewModel.DetailState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.hadith_ref_fmt, s.meta.title, s.entry.number),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.brand.gold
                        )
                        s.sectionTitle?.let { title ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        s.entry.inBookRef?.let { ref ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hadith_detail_inbook_fmt, ref),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    s.entry.arabic?.let { arabic ->
                        item {
                            Text(
                                text = arabic,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 32.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    item {
                        if (s.entry.bengali != null) {
                            Text(
                                text = s.entry.bengali,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.hadith_no_bengali),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    if (s.entry.grades.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.hadith_grades_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(s.entry.grades) { (scholar, grade) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(
                                    text = scholar,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = grade,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.brand.gold
                                )
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.hadith_detail_source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ---------- Library ----------

class HadithLibraryViewModel(private val repo: HadithRepository) : ViewModel() {
    sealed interface Level {
        data object Books : Level
        data class Chapters(val bookId: String) : Level
        data class List(val bookId: String, val section: Int) : Level
    }

    sealed interface BookLoadState {
        data object Idle : BookLoadState
        data object Loading : BookLoadState
        data class Ready(val data: HadithBookData) : BookLoadState
        data object Failed : BookLoadState
    }

    private val levelFlow = MutableStateFlow<Level>(Level.Books)
    val level: StateFlow<Level> = levelFlow.asStateFlow()

    private val bookFlow = MutableStateFlow<BookLoadState>(BookLoadState.Idle)
    val book: StateFlow<BookLoadState> = bookFlow.asStateFlow()

    fun openBook(bookId: String) {
        levelFlow.value = Level.Chapters(bookId)
        load(bookId)
    }

    fun openSection(section: Int) {
        val cur = levelFlow.value
        if (cur is Level.Chapters) levelFlow.value = Level.List(cur.bookId, section)
    }

    /** Returns false when already at the book list (caller should pop the screen). */
    fun up(): Boolean {
        return when (val cur = levelFlow.value) {
            is Level.Books -> false
            is Level.Chapters -> {
                levelFlow.value = Level.Books
                true
            }
            is Level.List -> {
                levelFlow.value = Level.Chapters(cur.bookId)
                true
            }
        }
    }

    fun retry() {
        val cur = levelFlow.value
        val bookId = when (cur) {
            is Level.Chapters -> cur.bookId
            is Level.List -> cur.bookId
            is Level.Books -> return
        }
        load(bookId)
    }

    private fun load(bookId: String) {
        viewModelScope.launch {
            bookFlow.value = BookLoadState.Loading
            bookFlow.value = repo.bookData(bookId)?.let { BookLoadState.Ready(it) } ?: BookLoadState.Failed
        }
    }
}

@Composable
fun HadithLibraryScreen(onOpenDetail: (String, String) -> Unit, onBack: () -> Unit) {
    val vm: HadithLibraryViewModel = appViewModel { HadithLibraryViewModel(it.hadithRepository) }
    val level by vm.level.collectAsStateWithLifecycle()
    val bookState by vm.book.collectAsStateWithLifecycle()
    BackHandler(enabled = level != HadithLibraryViewModel.Level.Books) { vm.up() }
    val title = when (level) {
        is HadithLibraryViewModel.Level.Books -> stringResource(R.string.hadith_library_title)
        is HadithLibraryViewModel.Level.Chapters -> {
            val id = (level as HadithLibraryViewModel.Level.Chapters).bookId
            HADITH_BOOKS.firstOrNull { it.id == id }?.title ?: stringResource(R.string.hadith_library_title)
        }
        is HadithLibraryViewModel.Level.List -> {
            val list = level as HadithLibraryViewModel.Level.List
            val data = (bookState as? HadithLibraryViewModel.BookLoadState.Ready)?.data
            data?.sections?.get(list.section) ?: stringResource(R.string.hadith_library_title)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!vm.up()) onBack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        when (level) {
            is HadithLibraryViewModel.Level.Books -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    items(HADITH_BOOKS) { book ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { vm.openBook(book.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = book.titleAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.hadith_book_meta_fmt, book.compiler, book.count),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.brand.gold
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.hadith_library_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            is HadithLibraryViewModel.Level.Chapters,
            is HadithLibraryViewModel.Level.List -> {
                when (val s = bookState) {
                    is HadithLibraryViewModel.BookLoadState.Loading,
                    is HadithLibraryViewModel.BookLoadState.Idle -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.brand.gold)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.hadith_loading_book),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is HadithLibraryViewModel.BookLoadState.Failed -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.hadith_load_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = vm::retry) { Text(stringResource(R.string.hadith_retry)) }
                        }
                    }
                    is HadithLibraryViewModel.BookLoadState.Ready -> {
                        val data = s.data
                        if (level is HadithLibraryViewModel.Level.Chapters) {
                            val chapters = data.sections.entries.sortedBy { it.key }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                if (chapters.isEmpty()) {
                                    item {
                                        ChapterRow(
                                            title = stringResource(R.string.hadith_all_hadith),
                                            count = data.entries.size,
                                            onClick = { vm.openSection(0) }
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                } else {
                                    items(chapters) { (num, title) ->
                                        val count = data.entries.count { it.section == num }
                                        ChapterRow(
                                            title = title,
                                            count = count,
                                            onClick = { vm.openSection(num) }
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        } else {
                            val section = (level as HadithLibraryViewModel.Level.List).section
                            val entries = if (data.sections.isEmpty()) {
                                data.entries
                            } else {
                                data.entries.filter { it.section == section }
                            }.sortedBy { it.number.toIntOrNull() ?: 0 }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                items(entries) { entry ->
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        onClick = { onOpenDetail(data.meta.id, entry.number) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(18.dp)) {
                                            Text(
                                                text = stringResource(R.string.hadith_list_number_fmt, entry.number),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.brand.gold
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            entry.arabic?.let { arabic ->
                                                Text(
                                                    text = arabic,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Right
                                                )
                                                Spacer(Modifier.height(6.dp))
                                            }
                                            if (entry.bengali != null) {
                                                Text(
                                                    text = entry.bengali,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(R.string.hadith_no_bengali),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(title: String, count: Int, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.hadith_count_fmt, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.brand.gold
            )
        }
    }
}
