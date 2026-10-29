package app.premiumhefz.ui.dhikr

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.DhikrData
import app.premiumhefz.data.DhikrRepository
import app.premiumhefz.data.TasbihProgress
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.theme.brand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DhikrViewModel(private val repo: DhikrRepository) : ViewModel() {
    sealed interface State {
        data object Loading : State
        data class Ready(val data: DhikrData, val progress: TasbihProgress) : State
        data object Failed : State
    }

    private val stateFlow = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = stateFlow.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun onTap() {
        val current = stateFlow.value as? State.Ready ?: return
        if (current.progress.done) return
        val phrases = current.data.tasbihPhrases
        val index = current.progress.phraseIndex.coerceIn(0, phrases.lastIndex)
        val phrase = phrases[index]
        val count = current.progress.count + 1
        val progress = if (count >= phrase.target) {
            if (index >= phrases.lastIndex) TasbihProgress(0, 0, done = true)
            else TasbihProgress(index + 1, 0, done = false)
        } else {
            TasbihProgress(index, count, done = false)
        }
        repo.saveProgress(progress)
        stateFlow.value = current.copy(progress = progress)
    }

    fun onReset() {
        val current = stateFlow.value as? State.Ready ?: return
        val progress = TasbihProgress(0, 0, done = false)
        repo.saveProgress(progress)
        stateFlow.value = current.copy(progress = progress)
    }

    private fun load() {
        viewModelScope.launch {
            stateFlow.value = State.Loading
            val data = repo.data()
            stateFlow.value = if (data != null) {
                State.Ready(data, repo.loadProgress())
            } else {
                State.Failed
            }
        }
    }
}

@Composable
fun DhikrScreen(onBack: () -> Unit) {
    val vm: DhikrViewModel = appViewModel { DhikrViewModel(it.dhikrRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
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
                text = stringResource(R.string.dhikr_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        when (val s = state) {
            is DhikrViewModel.State.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.brand.gold)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.dhikr_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is DhikrViewModel.State.Failed -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.dhikr_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::retry) { Text(stringResource(R.string.dhikr_retry)) }
                }
            }
            is DhikrViewModel.State.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.dhikr_sayyid_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = s.data.sayyid.arabic,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 32.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.dhikr_sayyid_ref, s.data.sayyid.reference, s.data.sayyid.narrator),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dhikr_sayyid_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                    item {
                        Text(
                            text = stringResource(R.string.dhikr_tasbih_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = s.data.tasbihReference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        if (s.progress.done) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.dhikr_done),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.brand.gold
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick = vm::onReset) {
                                        Text(stringResource(R.string.dhikr_reset))
                                    }
                                }
                            }
                        } else {
                            val phrase = s.data.tasbihPhrases[s.progress.phraseIndex.coerceIn(0, s.data.tasbihPhrases.lastIndex)]
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = vm::onTap,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = phrase.arabic,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 36.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = phrase.gloss,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.dhikr_count_fmt, s.progress.count, phrase.target),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.brand.gold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.dhikr_tap_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = vm::onReset) {
                                    Text(stringResource(R.string.dhikr_reset))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
