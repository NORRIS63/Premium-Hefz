package app.premiumhefz.ui.sajdah

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.premiumhefz.R
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.domain.SajdahDispute
import app.premiumhefz.domain.SajdahVerse
import app.premiumhefz.domain.SajdahVerses
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.theme.brand

/**
 * Index of the 15 verses marked with a prostration marker, shown with the
 * Hanafi set first and the two school-disputed verses noted.
 *
 * Tapping a verse opens its surah's first page in the reader. Exact verse
 * pages are deliberately NOT shown: no verified verse-to-page table exists
 * for this app's 610-page mushaf, and the screen says so.
 */
@Composable
fun SajdahScreen(onOpenPage: (Int) -> Unit, onBack: () -> Unit) {
    val index = LocalContainer.current.quranIndex
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
                text = stringResource(R.string.sajdah_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sajdah_header),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                SectionHeader(stringResource(R.string.sajdah_section_hanafi))
            }
            items(SajdahVerses.hanafi) { verse ->
                SajdahRow(verse, index, onOpenPage)
                Spacer(Modifier.height(10.dp))
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(stringResource(R.string.sajdah_section_other))
            }
            items(SajdahVerses.otherSchools) { verse ->
                SajdahRow(verse, index, onOpenPage)
                Spacer(Modifier.height(10.dp))
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sajdah_open_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sajdah_footer_schools),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SajdahRow(
    verse: SajdahVerse,
    index: QuranIndex,
    onOpenPage: (Int) -> Unit
) {
    val surah = index.surahByNumber(verse.surah)
    val startPage = surah?.startPage
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { if (startPage != null) onOpenPage(startPage) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (surah != null) {
                        stringResource(R.string.sajdah_row_title, surah.number, surah.nameEn)
                    } else {
                        stringResource(R.string.sajdah_row_title_unknown, verse.surah)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sajdah_row_ayah, verse.ayahLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val noteRes = when (verse.dispute) {
                    SajdahDispute.HAJJ_77 -> R.string.sajdah_note_hajj
                    SajdahDispute.SAD_24 -> R.string.sajdah_note_sad
                    null -> null
                }
                if (noteRes != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(noteRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.brand.goldSoft
                    )
                }
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
