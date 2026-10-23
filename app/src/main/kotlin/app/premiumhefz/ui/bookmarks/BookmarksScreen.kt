package app.premiumhefz.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.BookmarkEntity
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.domain.BookmarkImport
import app.premiumhefz.domain.ImportMode
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.EmptyState
import app.premiumhefz.ui.components.ScreenTitle
import app.premiumhefz.ui.components.StarBadge
import app.premiumhefz.ui.theme.brand
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val repo: HefzRepository) : ViewModel() {
    val bookmarks: StateFlow<List<BookmarkEntity>> =
        repo.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteBookmark(id) }
    }

    /** Calls [onDone] with the number of bookmarks that were actually added. */
    fun import(pages: List<Int>, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repo.importBookmarks(pages)) }
    }
}

@Composable
fun BookmarksScreen(onOpenPage: (Int) -> Unit) {
    val index = LocalContainer.current.quranIndex
    val vm: BookmarksViewModel = appViewModel { BookmarksViewModel(it.repository) }
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf<Int?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.bookmarks_title)) {
            TextButton(onClick = { showImport = true }) { Text(stringResource(R.string.action_import)) }
        }
        importedCount?.let { count ->
            Text(
                text = stringResource(R.string.imported_fmt, count),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.brand.gold
            )
        }
        if (bookmarks.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.bookmarks_empty_title),
                body = stringResource(R.string.bookmarks_empty_body)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        index = index,
                        date = dateFormat.format(Date(bookmark.createdAt)),
                        onOpen = { onOpenPage(bookmark.page) },
                        onDelete = { pendingDelete = bookmark }
                    )
                }
            }
        }
    }

    pendingDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.bookmark_delete_title)) },
            text = { Text(stringResource(R.string.bookmark_delete_body_fmt, bookmark.page)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(bookmark.id); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showImport) {
        ImportDialog(
            index = index,
            onDismiss = { showImport = false },
            onConfirm = { pages ->
                showImport = false
                vm.import(pages) { importedCount = it }
            }
        )
    }
}

@Composable
private fun BookmarkRow(bookmark: BookmarkEntity, index: QuranIndex, date: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    val juz = index.juzForPage(bookmark.page)
    val pageInJuz = index.pageInJuz(bookmark.page)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StarBadge(bookmark.page, badgeSize = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = index.surahLabel(bookmark.page),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (juz != null && pageInJuz != null) {
                    stringResource(R.string.bookmark_subtitle_fmt, bookmark.page, juz.number, pageInJuz, date)
                } else {
                    stringResource(R.string.bookmark_subtitle_opening_fmt, bookmark.page, date)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportDialog(index: QuranIndex, onDismiss: () -> Unit, onConfirm: (List<Int>) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var modeOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val modes = listOf(
        ImportMode.PRINTED_PAGE to R.string.import_mode_printed,
        ImportMode.JUZ_AND_PAGE to R.string.import_mode_juz,
        ImportMode.ORIGINAL_GO_TO to R.string.import_mode_goto,
        ImportMode.ORIGINAL_STORED to R.string.import_mode_stored
    )
    val mode = modes[modeOrdinal].first
    val parsed = remember(text, mode) { BookmarkImport.parse(text, mode, index) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.import_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                modes.forEachIndexed { i, (_, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { modeOrdinal = i },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = modeOrdinal == i, onClick = { modeOrdinal = i })
                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.import_input_hint)) },
                    minLines = 3
                )
                if (text.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.import_preview_fmt, parsed.pages.size, parsed.skipped.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.brand.gold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsed.pages) }, enabled = parsed.pages.isNotEmpty()) {
                Text(stringResource(R.string.action_import))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
