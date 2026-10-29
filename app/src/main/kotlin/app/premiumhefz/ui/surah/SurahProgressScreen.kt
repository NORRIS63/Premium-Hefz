package app.premiumhefz.ui.surah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import app.premiumhefz.R
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.GoldProgress
import app.premiumhefz.ui.theme.brand
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class SurahProgress(
    val number: Int,
    val nameEn: String,
    val nameAr: String,
    val startPage: Int,
    val read: Int,
    val total: Int
) {
    val percent: Int get() = if (total > 0) (read * 100) / total else 0
}

class SurahProgressViewModel(repo: HefzRepository, index: QuranIndex) : ViewModel() {
    val rows: StateFlow<List<SurahProgress>> = repo.distinctPages
        .map { pages ->
            val set = pages.toSet()
            index.surahs.mapIndexed { i, surah ->
                val end = index.surahs.getOrNull(i + 1)?.startPage?.minus(1) ?: PageCoordinates.LAST_PAGE
                val total = end - surah.startPage + 1
                val read = (surah.startPage..end).count { it in set }
                SurahProgress(surah.number, surah.nameEn, surah.nameAr, surah.startPage, read, total)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun SurahProgressScreen(onBack: () -> Unit, onOpenPage: (Int) -> Unit) {
    val vm: SurahProgressViewModel = appViewModel { SurahProgressViewModel(it.repository, it.quranIndex) }
    val rows by vm.rows.collectAsStateWithLifecycle()
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
                text = stringResource(R.string.surah_progress_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(row.startPage) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = row.nameEn,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text(
                                text = row.nameAr,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        GoldProgress(fraction = row.percent / 100f)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.surah_progress_meta_fmt, row.number, row.read, row.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = stringResource(R.string.surah_progress_percent_fmt, row.percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.brand.gold
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
