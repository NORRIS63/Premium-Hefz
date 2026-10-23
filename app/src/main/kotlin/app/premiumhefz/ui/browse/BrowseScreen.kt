package app.premiumhefz.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import app.premiumhefz.R
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.domain.Surah
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.components.EmptyState
import app.premiumhefz.ui.components.ScreenTitle
import app.premiumhefz.ui.components.StarBadge
import app.premiumhefz.ui.theme.brand

@Composable
fun BrowseScreen(initialTab: Int, onOpenPage: (Int) -> Unit) {
    val index = LocalContainer.current.quranIndex
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val labels = listOf(R.string.browse_tab_surah, R.string.browse_tab_juz, R.string.browse_tab_page)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.nav_browse))
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.brand.gold
        ) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(stringResource(label)) },
                    selectedContentColor = MaterialTheme.brand.gold,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when (tab) {
            0 -> SurahList(index, onOpenPage)
            1 -> JuzList(index, onOpenPage)
            else -> Column(Modifier.fillMaxWidth().padding(20.dp)) { GoToPageForm(onOpen = onOpenPage) }
        }
    }
}

@Composable
private fun SurahList(index: QuranIndex, onOpenPage: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { index.search(query) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.browse_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )
        if (results.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.browse_no_results, query),
                body = stringResource(R.string.browse_no_results_body)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(results, key = { it.number }) { surah ->
                    SurahRow(surah, onClick = { onOpenPage(surah.startPage) })
                }
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StarBadge(surah.number)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(surah.nameEn, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(R.string.surah_meta_fmt, surah.ayahCount, surah.revelation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = surah.nameAr,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl),
                color = MaterialTheme.brand.gold
            )
            Text(
                text = stringResource(R.string.surah_page_fmt, surah.startPage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun JuzList(index: QuranIndex, onOpenPage: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(index.juz, key = { it.number }) { juz ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPage(juz.startPage) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarBadge(juz.number)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.juz_title_fmt, juz.number),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = juz.nameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.juz_meta_fmt, juz.startPage, index.juzPageCount(juz.number)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = juz.openingAr,
                    style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.brand.gold
                )
            }
        }
    }
}
