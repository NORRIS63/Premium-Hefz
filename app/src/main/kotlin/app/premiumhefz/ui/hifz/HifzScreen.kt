package app.premiumhefz.ui.hifz

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.GoldProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HifzViewModel(private val repo: HefzRepository) : ViewModel() {
    val memorized: StateFlow<List<Int>> = repo.memorizedPages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPage(page: Int) {
        viewModelScope.launch { repo.markMemorized(page) }
    }

    fun removePage(page: Int) {
        viewModelScope.launch { repo.unmarkMemorized(page) }
    }
}

@Composable
fun HifzScreen(index: QuranIndex, onBack: () -> Unit, onOpenPage: (Int) -> Unit) {
    val vm: HifzViewModel = appViewModel { HifzViewModel(it.repository) }
    val memorized by vm.memorized.collectAsStateWithLifecycle()
    var pageText by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
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
                text = stringResource(R.string.hifz_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.hifz_progress_fmt, memorized.size, PageCoordinates.PAGE_COUNT),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                GoldProgress(fraction = memorized.size.toFloat() / PageCoordinates.PAGE_COUNT.toFloat())
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter { c -> c.isDigit() }; invalid = false },
                        label = { Text(stringResource(R.string.hifz_add_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val page = pageText.toIntOrNull()
                        if (page == null || !PageCoordinates.isValid(page)) {
                            invalid = true
                        } else {
                            invalid = false
                            pageText = ""
                            vm.addPage(page)
                        }
                    }) { Text(stringResource(R.string.hifz_add)) }
                }
                if (invalid) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.hifz_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
            if (memorized.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.hifz_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(memorized) { page ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = index.surahLabel(page),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.hifz_page_fmt, page),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenPage(page) }) {
                            Text(stringResource(R.string.hifz_open))
                        }
                        TextButton(onClick = { vm.removePage(page) }) {
                            Text(stringResource(R.string.hifz_remove))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
